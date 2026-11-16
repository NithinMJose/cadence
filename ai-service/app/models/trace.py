from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class DebugTrace(BaseModel):
    """End-to-End Flight Recorder object attached to every AI and Solver response."""

    trace_id: str = Field(description="Unique request trace identifier")
    llm_provider_used: Optional[str] = Field(
        default=None,
        description="Provider utilized: 'ollama_local' | 'gemini_fallback' | 'deterministic_mock'",
    )
    llm_model: Optional[str] = Field(
        default=None,
        description="Exact model name used for generation",
    )
    llm_latency_ms: Optional[float] = Field(
        default=None,
        description="Total round-trip LLM generation latency in milliseconds",
    )
    secrets_redacted_count: int = Field(
        default=0,
        description="Number of credential/key tokens redacted by SecretScrubber",
    )
    validation_retries_used: int = Field(
        default=0,
        description="Number of self-healing retries needed to produce valid schema",
    )
    extracted_ast_summary: Optional[Dict[str, Any]] = Field(
        default=None,
        description="Compact summary of extracted constraints or entities",
    )
    solver_status: Optional[str] = Field(
        default=None,
        description="OR-Tools CP-SAT status: OPTIMAL | FEASIBLE | OVERLOADED_SOLVED_WITH_SLACK",
    )
    solver_wall_time_ms: Optional[float] = Field(
        default=None,
        description="Execution wall time of the CP-SAT solver in milliseconds",
    )
    active_constraints_triggered: List[str] = Field(
        default_factory=list,
        description="List of constraint identifiers active in solver execution",
    )
