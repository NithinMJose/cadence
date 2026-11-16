import json
import logging
import re
import time
from typing import Any, Dict, List, Optional
from app.config import Settings, get_settings
from app.llm.autopsy_prompts import AUTOPSY_SYSTEM_PROMPT
from app.llm.gemini_client import GeminiClient
from app.models.autopsy import CommitAutopsyRequest, CommitAutopsyResponse, GeneratedFlashcard
from app.models.trace import DebugTrace
from app.security.scrubber import SecretScrubber
from app.services.ollama_client import OllamaClient

logger = logging.getLogger(__name__)


class CommitAutopsyService:
    """Extracts FSRS cards, warm-up briefs, and emergency floor quizzes from git diffs."""

    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.scrubber = SecretScrubber()
        self.ollama = OllamaClient(self.settings)
        self.gemini = GeminiClient(self.settings)

    async def analyze_diff(
        self, request: CommitAutopsyRequest, trace_id: str = "trace-autopsy"
    ) -> CommitAutopsyResponse:
        start_time = time.perf_counter()

        # 1. Scrub credentials and high-entropy tokens
        scrub_result = self.scrubber.scrub(request.raw_diff)
        scrubbed_diff = scrub_result.scrubbed_text

        # 2. Wrap in sandboxed XML tags to prevent prompt injection
        sandboxed_input = (
            f"Commit Message: {request.commit_message or 'No commit message'}\n"
            f"Repository: {request.repo_name or 'unknown'}\n"
            f"<untrusted_code_diff>\n{scrubbed_diff[:8000]}\n</untrusted_code_diff>"
        )

        mode = self.settings.llm_provider_mode.upper()
        if mode == "MOCK":
            res = self._deterministic_diff_analysis(scrubbed_diff, request.commit_message)
            latency = (time.perf_counter() - start_time) * 1000
            res.debug_trace = DebugTrace(
                trace_id=trace_id,
                llm_provider_used="deterministic_mock",
                llm_model="cadence-diff-analyzer-mock",
                llm_latency_ms=round(latency, 2),
                secrets_redacted_count=scrub_result.redacted_count,
            )
            res.secrets_redacted_count = scrub_result.redacted_count
            return res

        # Attempt Ollama -> Gemini -> Mock fallback
        try:
            full_prompt = f"{AUTOPSY_SYSTEM_PROMPT}\n\n{sandboxed_input}"
            ollama_res = await self.ollama.generate(full_prompt, timeout_seconds=25.0)
            parsed = self._extract_json(ollama_res.get("response", "{}"))
            res = CommitAutopsyResponse.model_validate(parsed)
            res.secrets_redacted_count = scrub_result.redacted_count
            res.debug_trace = DebugTrace(
                trace_id=trace_id,
                llm_provider_used="ollama_local",
                llm_model=self.settings.ollama_model,
                llm_latency_ms=round((time.perf_counter() - start_time) * 1000, 2),
                secrets_redacted_count=scrub_result.redacted_count,
            )
            return res
        except Exception as e:
            logger.warning("Ollama autopsy failed: %s", e)
            if self.settings.allow_cloud_fallback and self.settings.gemini_api_key:
                try:
                    gem_res = await self.gemini.generate(AUTOPSY_SYSTEM_PROMPT, sandboxed_input)
                    parsed = self._extract_json(gem_res.get("text", "{}"))
                    res = CommitAutopsyResponse.model_validate(parsed)
                    res.secrets_redacted_count = scrub_result.redacted_count
                    res.debug_trace = DebugTrace(
                        trace_id=trace_id,
                        llm_provider_used="gemini_fallback",
                        llm_model=gem_res.get("model", "gemini-2.5-flash"),
                        llm_latency_ms=round((time.perf_counter() - start_time) * 1000, 2),
                        secrets_redacted_count=scrub_result.redacted_count,
                    )
                    return res
                except Exception as gem_err:
                    logger.warning("Gemini fallback autopsy failed: %s", gem_err)

        # High-signal deterministic rule-based analysis
        res = self._deterministic_diff_analysis(scrubbed_diff, request.commit_message)
        latency = (time.perf_counter() - start_time) * 1000
        res.secrets_redacted_count = scrub_result.redacted_count
        res.debug_trace = DebugTrace(
            trace_id=trace_id,
            llm_provider_used="deterministic_mock",
            llm_model="cadence-diff-analyzer-mock",
            llm_latency_ms=round(latency, 2),
            secrets_redacted_count=scrub_result.redacted_count,
        )
        return res

    def _extract_json(self, raw: str) -> Dict[str, Any]:
        match = re.search(r"\{[\s\S]*\}", raw)
        return json.loads(match.group(0)) if match else json.loads(raw)

    def _deterministic_diff_analysis(
        self, diff_text: str, commit_msg: Optional[str]
    ) -> CommitAutopsyResponse:
        # Detect key themes from diff
        files_changed = re.findall(r"diff --git a/(.*?) b/", diff_text)
        primary_file = files_changed[0] if files_changed else "core_module.py"
        lower = diff_text.lower()

        topic = "architecture"
        if any(k in lower for k in ["fsrs", "card", "spaced", "interval"]):
            topic = "spaced_repetition"
        elif any(k in lower for k in ["solver", "cpsat", "cp-sat", "constraint", "schedule"]):
            topic = "constraint_solver"
        elif any(k in lower for k in ["auth", "token", "jwt", "key", "security"]):
            topic = "security"

        title = commit_msg.split("\n")[0] if commit_msg else f"Updates to {primary_file}"
        title = re.sub(r"^(feat|fix|refactor|test|chore)(\(.*?\))?:\s*", "", title).strip()
        if not title:
            title = f"Refactoring in {primary_file}"

        card = GeneratedFlashcard(
            title=title[:60],
            topic_tag=topic,
            summary=f"Key patterns, state transitions, and invariants in {primary_file}.",
            emergency_floor_prompt=f"Identify the primary edge case or race condition handled in {primary_file}.",
            rubric_checkpoints=[
                f"1. Explain the architectural responsibility of {primary_file}",
                "2. State the key invariant or data boundary enforced by the changes",
                "3. Describe the potential failure mode or edge case addressed",
                "4. Detail the time or space complexity implications of this approach",
            ],
        )

        warmup = [
            f"1. Modified {len(files_changed) or 1} file(s) focusing on {title[:40]}.",
            f"2. Pay special attention to state invariants and concurrency in {primary_file}.",
            "3. Next step: write comprehensive test assertions covering boundary cases.",
        ]

        quiz = f"What happens in {primary_file} if invalid or empty inputs are provided?"

        return CommitAutopsyResponse(
            session_warmup_brief=warmup,
            emergency_floor_microquiz=quiz,
            flashcards=[card],
            secrets_redacted_count=0,
            debug_trace=DebugTrace(trace_id="mock"),
        )
