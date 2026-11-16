import time
from typing import Any, Dict, Optional
import httpx
from app.config import Settings, get_settings


class GeminiClient:
    """Async client for Google Gemini API fallback when local GPU is occupied."""

    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.api_key = self.settings.gemini_api_key
        self.model = self.settings.gemini_model

    async def generate(
        self,
        system_prompt: str,
        user_prompt: str,
        timeout_seconds: float = 20.0,
    ) -> Dict[str, Any]:
        """Call Gemini Flash API requesting JSON output format."""
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY is not configured in settings.")

        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.api_key}"
        payload = {
            "contents": [
                {
                    "role": "user",
                    "parts": [{"text": f"{system_prompt}\n\n{user_prompt}"}],
                }
            ],
            "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.1,
            },
        }

        start_time = time.perf_counter()
        async with httpx.AsyncClient(timeout=timeout_seconds) as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            elapsed_ms = (time.perf_counter() - start_time) * 1000.0

            data = response.json()
            candidates = data.get("candidates", [])
            if not candidates:
                raise ValueError("Gemini returned empty candidates list.")

            raw_text = candidates[0]["content"]["parts"][0]["text"]
            return {
                "text": raw_text,
                "latency_ms": elapsed_ms,
                "model": self.model,
            }
