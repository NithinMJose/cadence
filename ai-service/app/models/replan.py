from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field
from app.models.ast import ConstraintMutationAST
from app.models.trace import DebugTrace
from app.solver.models import (
    BlockedWindow,
    LeisureWindow,
    PeakCognitiveWindow,
    ScheduleDiffItem,
    ScheduledBlock,
    SolverTask,
    TradeoffCard,
)


class ReplanPipelineRequest(BaseModel):
    """Request payload for the end-to-end AI Replan pipeline."""
    model_config = ConfigDict(extra="ignore")

    user_message: Optional[str] = Field(default=None, description="Natural language prompt from user or voice transcript")
    constraint_ast: Optional[ConstraintMutationAST] = Field(default=None, description="Direct AST if pre-compiled")
    tasks: List[SolverTask] = Field(default_factory=list, description="Active curriculum tasks or cards to schedule")
    existing_blocked_windows: List[BlockedWindow] = Field(default_factory=list, description="Existing calendar blocks")
    existing_leisure_windows: List[LeisureWindow] = Field(default_factory=list, description="Existing protected leisure")
    peak_windows: List[PeakCognitiveWindow] = Field(default_factory=list, description="User peak focus hours")
    horizon_start: Optional[datetime] = Field(default=None, description="Horizon start timestamp")
    horizon_days: int = Field(default=7, description="Scheduling window in days")
    flex_credits_available_min: int = Field(default=0, description="Available banked flex credits")
    timezone: str = Field(default="Asia/Kolkata", description="IANA timezone")


class ReplanPipelineResponse(BaseModel):
    """Response containing proposed schedule changes, diff, tradeoff cards, and flight recorder."""
    model_config = ConfigDict(populate_by_name=True)

    status: str = Field(description="OPTIMAL | FEASIBLE | OVERLOADED_SOLVED_WITH_SLACK")
    ast_summary: Optional[str] = Field(default=None, description="Summary of parsed constraints if message was provided")
    scheduled_blocks: List[ScheduledBlock] = Field(default_factory=list, description="Proposed new schedule")
    diff_items: List[ScheduleDiffItem] = Field(default_factory=list, description="Git-style diff with causal reasons")
    tradeoff_cards: List[TradeoffCard] = Field(default_factory=list, description="Capacity negotiation cards if overloaded")
    total_scheduled_minutes: int = Field(default=0, description="Total minutes scheduled")
    total_slack_minutes: int = Field(default=0, description="Minutes absorbed by slack variables")
    churn_score: int = Field(default=0, description="Schedule churn score")
    debug_trace: DebugTrace = Field(alias="_debug_trace", description="Flight recorder telemetry")
