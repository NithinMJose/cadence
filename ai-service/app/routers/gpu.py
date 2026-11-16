import logging
from typing import Any, Dict
from fastapi import APIRouter, Request
import httpx
from app.config import get_settings
from app.models.gpu_test import GpuSmokeTestResponse
from app.models.problem_detail import CadenceServiceException
from app.models.trace import DebugTrace
from app.services.ollama_client import OllamaClient

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ai", tags=["GPU & LLM Diagnostics"])


def _build_blackwell_diagnostics(is_gpu: bool, vram_bytes: int, total_bytes: int) -> str:
    """Produce clear diagnostic notes for NVIDIA GeForce RTX 5060 Blackwell GPU."""
    if is_gpu:
        pct = (vram_bytes / total_bytes * 100.0) if total_bytes > 0 else 100.0
        return (
            f"RTX 5060 Blackwell GPU verified: {pct:.1f}% ({vram_bytes / (1024**2):.1f} MB) "
            "of model weights offloaded to VRAM. CUDA acceleration active."
        )
    return (
        "Execution ran on CPU (size_vram=0). RTX 5060 Blackwell (sm_120) requires: "
        "1) NVIDIA Host Driver >= 570.xx; "
        "2) CUDA >= 12.8 compatible container runtime; "
        "3) NVIDIA Container Toolkit with 'capabilities: [gpu]' in docker-compose.yml."
    )


@router.get("/gpu-smoke-test", response_model=GpuSmokeTestResponse)
async def gpu_smoke_test(request: Request) -> GpuSmokeTestResponse:
    """Verify local Ollama connectivity and confirm GPU/VRAM residency."""
    settings = get_settings()
    trace_id = getattr(request.state, "trace_id", "trace-gpu-smoke-test")
    client = OllamaClient(settings)

    prompt = "Ping test for Cadence GPU residency. Output exactly: READY."
    try:
        gen_result = await client.generate(prompt=prompt, timeout_seconds=15.0)
    except httpx.ConnectError:
        raise CadenceServiceException(
            status_code=503,
            error_code="OLLAMA_UNREACHABLE",
            message=f"Cannot connect to Ollama at {settings.ollama_base_url}",
            remediation_hint=(
                "Ensure the Ollama container is running ('docker compose up -d ollama') "
                "or local Ollama daemon is active ('ollama serve')."
            ),
        )
    except Exception as exc:
        raise CadenceServiceException(
            status_code=500,
            error_code="LLM_GENERATION_FAILED",
            message=f"Generation failed during smoke test: {str(exc)}",
            remediation_hint="Check Ollama container logs with 'docker compose logs ollama'.",
        )

    latency_ms = gen_result.get("_latency_ms", 0.0)
    generated_text = gen_result.get("response", "").strip()

    # Query active process list for VRAM allocation
    vram_bytes = 0
    total_bytes = 0
    is_gpu = False
    try:
        ps_data = await client.get_running_processes()
        models = ps_data.get("models", [])
        for m in models:
            if settings.ollama_model in m.get("name", "") or len(models) == 1:
                vram_bytes = m.get("size_vram", 0)
                total_bytes = m.get("size", 0)
                is_gpu = vram_bytes > 0
                break
    except Exception as exc:
        logger.warning("Failed to query Ollama /api/ps: %s", exc)

    offload_pct = (vram_bytes / total_bytes * 100.0) if total_bytes > 0 else (100.0 if is_gpu else 0.0)
    status_str = "SUCCESS" if is_gpu else "CPU_FALLBACK"
    notes = _build_blackwell_diagnostics(is_gpu, vram_bytes, total_bytes)

    debug_trace = DebugTrace(
        trace_id=trace_id,
        llm_provider_used="ollama_local",
        llm_model=settings.ollama_model,
        llm_latency_ms=round(latency_ms, 2),
        secrets_redacted_count=0,
        validation_retries_used=0,
        extracted_ast_summary={"response": generated_text[:50]},
    )

    return GpuSmokeTestResponse(
        status=status_str,
        is_gpu_accelerated=is_gpu,
        vram_bytes_allocated=vram_bytes,
        total_model_bytes=total_bytes,
        offload_percentage=round(offload_pct, 2),
        generation_latency_ms=round(latency_ms, 2),
        model=settings.ollama_model,
        generated_text=generated_text,
        blackwell_rtx5060_notes=notes,
        _debug_trace=debug_trace,
    )
