import json
import logging
import time
from typing import Any, Dict, Optional, Tuple
from zoneinfo import ZoneInfo
from app.config import Settings, get_settings
from app.llm.date_resolver import SymbolicDateResolver
from app.llm.gemini_client import GeminiClient
from app.llm.prompts import INTENT_COMPILER_SYSTEM_PROMPT, format_intent_prompt
from app.llm.validator import SelfHealingValidator
from app.models.ast import (
    ActionType,
    ConstraintItem,
    ConstraintMutationAST,
    DeviceRequirement,
    TargetWindow,
)
from app.models.intent import ParseIntentResponse
from app.models.problem_detail import CadenceServiceException
from app.models.trace import DebugTrace
from app.security.scrubber import SecretScrubber
from app.services.ollama_client import OllamaClient

logger = logging.getLogger(__name__)


class LLMRouter:
    """Orchestrates 3-part prompt sandwich, provider routing, fallback, and validation."""

    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.scrubber = SecretScrubber()
        self.validator = SelfHealingValidator()
        self.date_resolver = SymbolicDateResolver(self.settings.default_user_timezone)
        self.ollama = OllamaClient(self.settings)
        self.gemini = GeminiClient(self.settings)

    async def parse_intent(
        self,
        raw_prompt: str,
        reference_date_iso: Optional[str] = None,
        tz_name: Optional[str] = None,
        user_context: Optional[Dict[str, Any]] = None,
        trace_id: str = "trace-intent",
    ) -> ParseIntentResponse:
        """Execute full pipeline: scrub -> route -> validate -> resolve windows."""
        # 1. Scrub secrets from user utterance
        scrub_result = self.scrubber.scrub(raw_prompt)
        clean_prompt = scrub_result.scrubbed_text

        # 2. Temporal grounding
        target_tz = ZoneInfo(tz_name or self.settings.default_user_timezone)
        from datetime import datetime
        base_dt = (
            datetime.fromisoformat(reference_date_iso).astimezone(target_tz)
            if reference_date_iso
            else datetime.now(target_tz)
        )

        user_prompt = format_intent_prompt(clean_prompt, base_dt, user_context)

        # 3. Call LLM provider with fallback
        ast, retries, provider, model_name, latency_ms = await self._execute_llm_call(
            clean_prompt, user_prompt
        )

        # 4. Resolve symbolic anchors to exact timestamps
        resolved_windows = self.date_resolver.resolve_constraints(
            ast.constraints, reference_date_iso, tz_name
        )

        status_str = "AMBIGUOUS" if ast.is_ambiguous else "RESOLVED"
        debug_trace = DebugTrace(
            trace_id=trace_id,
            llm_provider_used=provider,
            llm_model=model_name,
            llm_latency_ms=round(latency_ms, 2),
            secrets_redacted_count=scrub_result.redacted_count,
            validation_retries_used=retries,
            extracted_ast_summary={
                "is_ambiguous": ast.is_ambiguous,
                "constraints_count": len(ast.constraints),
                "summary": ast.summary,
            },
        )

        return ParseIntentResponse(
            status=status_str,
            ast=ast,
            resolved_windows=resolved_windows,
            clarification_options=ast.clarification_options,
            _debug_trace=debug_trace,
        )

    async def _execute_llm_call(
        self, raw_clean: str, full_prompt: str
    ) -> Tuple[ConstraintMutationAST, int, str, str, float]:
        """Dispatch call to Mock, Ollama, or Gemini with self-healing validation."""
        mode = self.settings.llm_provider_mode.upper()

        if mode == "MOCK":
            start = time.perf_counter()
            ast = self._deterministic_mock_parse(raw_clean)
            return ast, 0, "deterministic_mock", "cadence-mock-rule-engine", (time.perf_counter() - start) * 1000

        # Try local Ollama first
        try:
            start = time.perf_counter()
            full_req = f"{INTENT_COMPILER_SYSTEM_PROMPT}\n\n{full_prompt}"
            res = await self.ollama.generate(full_req, timeout_seconds=20.0)
            raw_output = res.get("response", "{}")

            async def retry_ollama(corr: str) -> str:
                retry_res = await self.ollama.generate(f"{full_req}\n\n{corr}")
                return retry_res.get("response", "{}")

            ast, retries = await self.validator.validate_with_retry(raw_output, retry_ollama)
            elapsed = (time.perf_counter() - start) * 1000
            return ast, retries, "ollama_local", self.settings.ollama_model, elapsed
        except Exception as ollama_err:
            logger.warning("Ollama call failed: %s", ollama_err)
            if self.settings.allow_cloud_fallback and self.settings.gemini_api_key:
                logger.info("Falling back to Google Gemini Flash API")
                start = time.perf_counter()
                gem_res = await self.gemini.generate(INTENT_COMPILER_SYSTEM_PROMPT, full_prompt)

                async def retry_gemini(corr: str) -> str:
                    retry_res = await self.gemini.generate(
                        INTENT_COMPILER_SYSTEM_PROMPT, f"{full_prompt}\n\n{corr}"
                    )
                    return retry_res.get("text", "{}")

                ast, retries = await self.validator.validate_with_retry(gem_res["text"], retry_gemini)
                elapsed = (time.perf_counter() - start) * 1000
                return ast, retries, "gemini_fallback", gem_res["model"], elapsed

            # If Ollama is offline/unreachable and no cloud key, fall back to deterministic mock parser
            logger.info("Falling back to deterministic mock parser: %s", ollama_err)
            start = time.perf_counter()
            ast = self._deterministic_mock_parse(raw_clean)
            elapsed = (time.perf_counter() - start) * 1000
            return ast, 0, "deterministic_mock", "cadence-mock-rule-engine", elapsed

    def _deterministic_mock_parse(self, text: str) -> ConstraintMutationAST:
        """Fast rule-based mock parser for offline CI tests and instant evaluation."""
        lower = text.lower()
        # 0. Prompt injection / jailbreak neutralization
        if any(w in lower for w in ["ignore", "override", "dan", "drop table", "<script", "secret api key", "print the", "reveal system", "exec", "<!--"]):
            return ConstraintMutationAST(
                is_ambiguous=True,
                ambiguity_reason="Adversarial prompt injection pattern detected and neutralized.",
                clarification_options=["Cancel request", "Rephrase scheduling instruction"],
                constraints=[],
                summary="Adversarial prompt injection neutralized. No calendar modifications allowed.",
            )

        # 1. Ambiguity detection
        if any(w in lower for w in ["busy this week", "too tired this week", "maybe later", "not sure", "swamped"]):
            return ConstraintMutationAST(
                is_ambiguous=True,
                ambiguity_reason="Vague availability statement lacks specific days or target hours.",
                clarification_options=["Block all evenings this week", "Downgrade study sessions to 3-min floor"],
                constraints=[],
                summary="Ambiguous availability request requiring clarification.",
            )

        # 2. Emergency Floor / Extreme fatigue
        if any(w in lower for w in ["exhausted", "floor", "3 min", "burnt out", "only 3 mins"]):
            return ConstraintMutationAST(
                is_ambiguous=False,
                constraints=[
                    ConstraintItem(
                        action=ActionType.DOWNGRADE_FLOOR,
                        day_offset=0,
                        window=TargetWindow.EVENING,
                        reason="User requested 3-minute Emergency Floor due to fatigue",
                        is_hard_constraint=True,
                    )
                ],
                summary="Downgrade today's study block to 3-minute Emergency Floor.",
            )

        # 3. Check for weekdays and actions
        constraints = []
        for day in ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]:
            if day.lower() in lower:
                action = ActionType.BLOCK
                window = TargetWindow.ALL_DAY
                reason = "Blocked by user utterance"
                if "evening" in lower:
                    window = TargetWindow.EVENING
                elif "morning" in lower:
                    window = TargetWindow.MORNING
                elif "afternoon" in lower:
                    window = TargetWindow.AFTERNOON

                if "dentist" in lower:
                    reason = "Dentist appointment"
                elif "gym" in lower:
                    action = ActionType.PROTECT_LEISURE
                    reason = "Gym session"
                elif "party" in lower:
                    reason = "Party event"
                elif "free" in lower or "unblock" in lower:
                    action = ActionType.UNBLOCK
                    reason = "User unblocked schedule"

                device = DeviceRequirement.ANY
                if "phone only" in lower:
                    device = DeviceRequirement.PHONE_ONLY
                elif "laptop only" in lower:
                    device = DeviceRequirement.LAPTOP_ONLY

                constraints.append(
                    ConstraintItem(
                        action=action,
                        target_weekday=day,
                        window=window,
                        device_requirement=device,
                        reason=reason,
                        is_hard_constraint=True,
                    )
                )

        if not constraints:
            # Default to blocking today or tomorrow if keywords matched
            day_offset = 1 if "tomorrow" in lower else 0
            window = TargetWindow.EVENING if "evening" in lower else TargetWindow.ALL_DAY
            constraints.append(
                ConstraintItem(
                    action=ActionType.BLOCK,
                    day_offset=day_offset,
                    window=window,
                    reason="User requested schedule adjustment",
                    is_hard_constraint=True,
                )
            )

        return ConstraintMutationAST(
            is_ambiguous=False,
            constraints=constraints,
            summary=f"Extracted {len(constraints)} constraint mutation(s).",
        )
