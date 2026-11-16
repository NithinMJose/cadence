from pydantic import BaseModel, ConfigDict, Field
from app.models.trace import DebugTrace


class GpuSmokeTestResponse(BaseModel):
    """Response model for the RTX 5060 Blackwell GPU smoke test."""

    model_config = ConfigDict(populate_by_name=True)

    status: str = Field(
        description="Overall test status: SUCCESS | CPU_FALLBACK | OLLAMA_UNREACHABLE"
    )
    is_gpu_accelerated: bool = Field(
        description="True if model layers are loaded into GPU VRAM (size_vram > 0)"
    )
    vram_bytes_allocated: int = Field(
        default=0,
        description="VRAM bytes reported by Ollama /api/ps",
    )
    total_model_bytes: int = Field(
        default=0,
        description="Total model bytes reported by Ollama /api/ps",
    )
    offload_percentage: float = Field(
        default=0.0,
        description="Percentage of model weights offloaded to VRAM (0-100%)",
    )
    generation_latency_ms: float = Field(
        default=0.0,
        description="End-to-end roundtrip latency for sample generation in ms",
    )
    model: str = Field(
        description="Model name queried during test",
    )
    generated_text: str = Field(
        default="",
        description="Text returned by model during generation probe",
    )
    blackwell_rtx5060_notes: str = Field(
        description="Diagnostic information regarding RTX 5060 Blackwell driver/CUDA compatibility",
    )
    debug_trace: DebugTrace = Field(
        alias="_debug_trace",
        description="Flight recorder telemetry for the test execution",
    )
