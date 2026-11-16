from unittest.mock import AsyncMock, patch
import httpx
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_gpu_smoke_test_active_gpu(async_client: AsyncClient):
    """Test GPU smoke test when Ollama reports model weights resident in VRAM."""
    mock_gen = {
        "response": "READY. RTX 5060 test OK.",
        "_latency_ms": 142.5,
    }
    mock_ps = {
        "models": [
            {
                "name": "qwen2.5:7b-instruct-q4_K_M",
                "size": 4683073984,
                "size_vram": 4683073984,
            }
        ]
    }

    with (
        patch("app.routers.gpu.OllamaClient.generate", new_callable=AsyncMock) as mock_g,
        patch("app.routers.gpu.OllamaClient.get_running_processes", new_callable=AsyncMock) as mock_p,
    ):
        mock_g.return_value = mock_gen
        mock_p.return_value = mock_ps

        response = await async_client.get(
            "/api/v1/ai/gpu-smoke-test",
            headers={"X-Trace-Id": "test-trace-gpu-pass"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "SUCCESS"
        assert data["is_gpu_accelerated"] is True
        assert data["vram_bytes_allocated"] == 4683073984
        assert data["offload_percentage"] == 100.0
        assert "Blackwell" in data["blackwell_rtx5060_notes"]
        assert data["_debug_trace"]["trace_id"] == "test-trace-gpu-pass"
        assert data["_debug_trace"]["llm_provider_used"] == "ollama_local"


@pytest.mark.asyncio
async def test_gpu_smoke_test_cpu_fallback(async_client: AsyncClient):
    """Test GPU smoke test when Ollama reports model running on CPU (size_vram=0)."""
    mock_gen = {
        "response": "READY from CPU.",
        "_latency_ms": 1250.0,
    }
    mock_ps = {
        "models": [
            {
                "name": "qwen2.5:7b-instruct-q4_K_M",
                "size": 4683073984,
                "size_vram": 0,
            }
        ]
    }

    with (
        patch("app.routers.gpu.OllamaClient.generate", new_callable=AsyncMock) as mock_g,
        patch("app.routers.gpu.OllamaClient.get_running_processes", new_callable=AsyncMock) as mock_p,
    ):
        mock_g.return_value = mock_gen
        mock_p.return_value = mock_ps

        response = await async_client.get("/api/v1/ai/gpu-smoke-test")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "CPU_FALLBACK"
        assert data["is_gpu_accelerated"] is False
        assert data["vram_bytes_allocated"] == 0
        assert "requires: 1) NVIDIA Host Driver" in data["blackwell_rtx5060_notes"]


@pytest.mark.asyncio
async def test_gpu_smoke_test_ollama_unreachable_problem_detail(async_client: AsyncClient):
    """Test RFC 7807 ProblemDetail when Ollama daemon is unreachable."""
    req = httpx.Request("POST", "http://localhost:11434/api/generate")
    with patch(
        "app.routers.gpu.OllamaClient.generate",
        side_effect=httpx.ConnectError("Connection refused", request=req),
    ):
        response = await async_client.get(
            "/api/v1/ai/gpu-smoke-test",
            headers={"X-Trace-Id": "trace-unreachable-test"},
        )
        assert response.status_code == 503
        assert response.headers.get("content-type") == "application/problem+json"
        problem = response.json()
        assert problem["error_code"] == "OLLAMA_UNREACHABLE"
        assert problem["trace_id"] == "trace-unreachable-test"
        assert "remediation_hint" in problem
        assert "docker compose up -d ollama" in problem["remediation_hint"]
