import json
import re
from typing import Awaitable, Callable, Optional, Tuple
from pydantic import ValidationError
from app.models.ast import ConstraintMutationAST


class SelfHealingValidator:
    """Self-healing JSON schema validator with automatic LLM retry feedback loop."""

    MAX_RETRIES = 2

    @staticmethod
    def clean_json_markdown(text: str) -> str:
        """Strip surrounding markdown code fences if present."""
        cleaned = text.strip()
        match = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", cleaned)
        if match:
            return match.group(1).strip()
        return cleaned

    async def validate_with_retry(
        self,
        initial_raw_text: str,
        retry_fn: Callable[[str], Awaitable[str]],
    ) -> Tuple[ConstraintMutationAST, int]:
        """Validate JSON against ConstraintMutationAST, triggering self-healing retries on failure."""
        current_text = initial_raw_text
        retries_used = 0

        while True:
            cleaned = self.clean_json_markdown(current_text)
            try:
                ast = ConstraintMutationAST.model_validate_json(cleaned)
                return ast, retries_used
            except (ValidationError, json.JSONDecodeError) as exc:
                if retries_used >= self.MAX_RETRIES:
                    raise exc

                retries_used += 1
                error_msg = str(exc)
                correction_prompt = (
                    f"Your previous response produced invalid JSON schema error:\n"
                    f"{error_msg}\n\n"
                    f"Please fix the error and return ONLY valid JSON matching ConstraintMutationAST."
                )
                current_text = await retry_fn(correction_prompt)
