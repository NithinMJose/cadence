import json
import logging
import re
import time
from typing import Any, Dict, List, Optional
from app.config import Settings, get_settings
from app.llm.autopsy_prompts import FEYNMAN_SYSTEM_PROMPT
from app.llm.gemini_client import GeminiClient
from app.models.feynman import FeynmanGradeRequest, FeynmanGradeResponse
from app.models.trace import DebugTrace
from app.services.ollama_client import OllamaClient

logger = logging.getLogger(__name__)


class FeynmanGraderService:
    """Evaluates 60-second spoken concept explanations against 4-point rubrics."""

    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.ollama = OllamaClient(self.settings)
        self.gemini = GeminiClient(self.settings)

    async def grade_explanation(
        self, request: FeynmanGradeRequest, trace_id: str = "trace-feynman"
    ) -> FeynmanGradeResponse:
        start_time = time.perf_counter()

        # Build prompt payload
        rubric_bullets = "\n".join(f"- {cp}" for cp in request.rubric_checkpoints)
        user_input = (
            f"Topic: {request.card_title} ({request.topic_tag})\n"
            f"Concept Summary: {request.concept_summary}\n\n"
            f"Expected Rubric Checkpoints:\n{rubric_bullets}\n\n"
            f"User Spoken Explanation Transcript:\n\"{request.transcript}\""
        )

        mode = self.settings.llm_provider_mode.upper()
        if mode == "MOCK":
            res = self._deterministic_feynman_grading(request)
            latency = (time.perf_counter() - start_time) * 1000
            res.debug_trace = DebugTrace(
                trace_id=trace_id,
                llm_provider_used="deterministic_mock",
                llm_model="cadence-feynman-evaluator-mock",
                llm_latency_ms=round(latency, 2),
            )
            return res

        # Ollama -> Gemini -> Mock fallback
        try:
            full_prompt = f"{FEYNMAN_SYSTEM_PROMPT}\n\n{user_input}"
            ollama_res = await self.ollama.generate(full_prompt, timeout_seconds=20.0)
            parsed = self._extract_json(ollama_res.get("response", "{}"))
            res = FeynmanGradeResponse.model_validate(parsed)
            res.debug_trace = DebugTrace(
                trace_id=trace_id,
                llm_provider_used="ollama_local",
                llm_model=self.settings.ollama_model,
                llm_latency_ms=round((time.perf_counter() - start_time) * 1000, 2),
            )
            return res
        except Exception as e:
            logger.warning("Ollama Feynman grade failed: %s", e)
            if self.settings.allow_cloud_fallback and self.settings.gemini_api_key:
                try:
                    gem_res = await self.gemini.generate(FEYNMAN_SYSTEM_PROMPT, user_input)
                    parsed = self._extract_json(gem_res.get("text", "{}"))
                    res = FeynmanGradeResponse.model_validate(parsed)
                    res.debug_trace = DebugTrace(
                        trace_id=trace_id,
                        llm_provider_used="gemini_fallback",
                        llm_model=gem_res.get("model", "gemini-2.5-flash"),
                        llm_latency_ms=round((time.perf_counter() - start_time) * 1000, 2),
                    )
                    return res
                except Exception as gem_err:
                    logger.warning("Gemini Feynman grade failed: %s", gem_err)

        res = self._deterministic_feynman_grading(request)
        latency = (time.perf_counter() - start_time) * 1000
        res.debug_trace = DebugTrace(
            trace_id=trace_id,
            llm_provider_used="deterministic_mock",
            llm_model="cadence-feynman-evaluator-mock",
            llm_latency_ms=round(latency, 2),
        )
        return res

    def _extract_json(self, raw: str) -> Dict[str, Any]:
        match = re.search(r"\{[\s\S]*\}", raw)
        return json.loads(match.group(0)) if match else json.loads(raw)

    def _deterministic_feynman_grading(
        self, request: FeynmanGradeRequest
    ) -> FeynmanGradeResponse:
        transcript = request.transcript.strip().lower()
        if not transcript or len(transcript) < 10 or "don't know" in transcript:
            return FeynmanGradeResponse(
                fsrs_grade=1,
                matched_checkpoints=[],
                missing_checkpoints=list(request.rubric_checkpoints),
                feedback="No substantive explanation was provided. Review the foundational concepts first.",
                debug_trace=DebugTrace(trace_id="mock"),
            )

        matched: List[str] = []
        missing: List[str] = []

        # Common stop words and generic filler verbs to ignore when matching
        stop_words = {
            "the", "a", "an", "in", "on", "at", "to", "for", "of", "and", "or",
            "is", "are", "define", "integer", "variable", "point", "explain", "state",
        }

        for cp in request.rubric_checkpoints:
            words = [w for w in re.findall(r"\b[a-z]{3,}\b", cp.lower()) if w not in stop_words]
            if not words:
                continue
            # Match if at least 50% of distinct significant keywords appear in transcript
            overlap = [w for w in words if w in transcript]
            if len(overlap) >= max(1, (len(words) + 1) // 2):
                matched.append(cp)
            else:
                missing.append(cp)

        matched_count = len(matched)
        if matched_count >= 4:
            grade = 4
        elif matched_count == 3:
            grade = 3
        elif matched_count == 2:
            grade = 2
        else:
            grade = 1

        if missing:
            feedback = f"Good effort. You covered {matched_count} points, but missed: {'; '.join(missing)}."
        else:
            feedback = "Flawless explanation! You addressed all 4 technical checkpoints with complete precision."

        return FeynmanGradeResponse(
            fsrs_grade=grade,
            matched_checkpoints=matched,
            missing_checkpoints=missing,
            feedback=feedback,
            debug_trace=DebugTrace(trace_id="mock"),
        )
