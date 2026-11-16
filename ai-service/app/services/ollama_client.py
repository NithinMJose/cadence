import time
from typing import Any, Dict, Optional
import httpx
from app.config import Settings, get_settings


class OllamaClient:
    """Async client for interacting with the local Ollama LLM daemon."""

    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()
        self.base_url = self.settings.ollama_base_url.rstrip("/")
        self.default_model = self.settings.ollama_model

    async def check_health(self) -> bool:
        """Verify Ollama server accessibility."""
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                res = await client.get(f"{self.base_url}/api/tags")
                return res.status_code == 200
        except Exception:
            return False

    async def generate(
        self,
        prompt: str,
        model: Optional[str] = None,
        timeout_seconds: float = 30.0,
    ) -> Dict[str, Any]:
        """Send generation request to Ollama and measure latency."""
        target_model = model or self.default_model
        payload = {
            "model": target_model,
            "prompt": prompt,
            "stream": False,
        }
        start_time = time.perf_counter()
        async with httpx.AsyncClient(timeout=timeout_seconds) as client:
            response = await client.post(
                f"{self.base_url}/api/generate",
                json=payload,
            )
            response.raise_for_status()
            elapsed_ms = (time.perf_counter() - start_time) * 1000.0
            data = response.json()
            data["_latency_ms"] = elapsed_ms
            return data

    async def get_running_processes(self) -> Dict[str, Any]:
        """Fetch active model residency telemetry from Ollama /api/ps."""
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(f"{self.base_url}/api/ps")
            response.raise_for_status()
            return response.json()
