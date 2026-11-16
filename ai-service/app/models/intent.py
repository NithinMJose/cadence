from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field
from app.models.ast import ActionType, ConstraintMutationAST, DeviceRequirement
from app.models.trace import DebugTrace


class ParseIntentRequest(BaseModel):
    """Input payload for natural language intent compilation."""
    model_config = ConfigDict(extra="ignore")

    prompt: str = Field(description="Raw natural language or transcribed voice prompt from user")
    reference_date: Optional[str] = Field(
        default=None,
        description="ISO-8601 anchor timestamp (defaults to current time in user's timezone)",
    )
    timezone: str = Field(
        default="Asia/Kolkata",
        description="User's IANA timezone for temporal grounding",
    )
    user_context: Optional[Dict[str, Any]] = Field(
        default=None,
        description="Minimal context slice of current tasks and schedule constraints",
    )


class ResolvedWindow(BaseModel):
    """Deterministically resolved timezone-aware time window."""
    model_config = ConfigDict(extra="ignore")

    action: ActionType
    start_time: str = Field(description="ISO-8601 timestamp with timezone offset")
    end_time: str = Field(description="ISO-8601 timestamp with timezone offset")
    topic_tag: Optional[str] = None
    device_requirement: DeviceRequirement
    is_hard_constraint: bool
    reason: str


class ParseIntentResponse(BaseModel):
    """Output response carrying AST, resolved timestamps, and flight recorder telemetry."""
    model_config = ConfigDict(populate_by_name=True)

    status: str = Field(description="Status of parsing: RESOLVED | AMBIGUOUS")
    ast: ConstraintMutationAST = Field(description="Structured schema AST produced by Intent Compiler")
    resolved_windows: List[ResolvedWindow] = Field(
        default_factory=list,
        description="Resolved ISO-8601 time windows ready for CP-SAT solver",
    )
    clarification_options: List[str] = Field(
        default_factory=list,
        description="1-2 one-tap clarification prompts when input is ambiguous",
    )
    debug_trace: DebugTrace = Field(
        alias="_debug_trace",
        description="End-to-end flight recorder telemetry",
    )
