from datetime import datetime
from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field
from app.models.trace import DebugTrace


class DeviceVenue(str, Enum):
    """Device or venue requirement for a study block."""
    LAPTOP_DEEP = "LAPTOP_DEEP"
    PHONE_COMMUTE = "PHONE_COMMUTE"
    ANY = "ANY"


class DiffAction(str, Enum):
    """Action performed on a scheduled task relative to previous schedule."""
    UNCHANGED = "UNCHANGED"
    MOVED = "MOVED"
    DOWNGRADED_TO_FLOOR = "DOWNGRADED_TO_FLOOR"
    DEFERRED = "DEFERRED"
    NEWLY_SCHEDULED = "NEWLY_SCHEDULED"


class SolverTask(BaseModel):
    """A learning task to be placed into the schedule."""
    model_config = ConfigDict(extra="ignore")

    id: str = Field(description="Unique task or card identifier")
    title: str = Field(description="Human-readable title of the learning session")
    topic_tag: str = Field(description="Curriculum domain or topic tag")
    duration_min: int = Field(default=45, description="Full planned duration in minutes")
    emergency_floor_duration_min: int = Field(default=3, description="Minimum Viable Session floor in minutes")
    device_venue: DeviceVenue = Field(default=DeviceVenue.LAPTOP_DEEP, description="Device/venue requirement")
    urgency_score: float = Field(default=5.0, description="Urgency score (0.0 to 10.0), e.g. from FSRS due date")
    cognitive_weight: float = Field(default=3.0, description="Cognitive difficulty weight (1.0 light to 5.0 heavy)")
    original_start: Optional[datetime] = Field(default=None, description="Previous start time if already scheduled")
    original_end: Optional[datetime] = Field(default=None, description="Previous end time if already scheduled")


class BlockedWindow(BaseModel):
    """An immovable commitment where no tasks can be scheduled."""
    model_config = ConfigDict(extra="ignore")

    start: datetime = Field(description="Window start timestamp")
    end: datetime = Field(description="Window end timestamp")
    reason: str = Field(description="Reason for block, e.g. 'Team Standup', 'Doctor Appointment'")
    is_hard: bool = Field(default=True, description="True if block cannot be violated")


class LeisureWindow(BaseModel):
    """Protected leisure or personal time."""
    model_config = ConfigDict(extra="ignore")

    start: datetime = Field(description="Leisure start timestamp")
    end: datetime = Field(description="Leisure end timestamp")
    name: str = Field(description="Name of leisure window, e.g. 'Gym', 'Family Dinner'")
    is_protected: bool = Field(default=True, description="True if protected leisure cannot be overwritten")


class PeakCognitiveWindow(BaseModel):
    """Daily recurring high-focus window for heavy cognitive tasks."""
    model_config = ConfigDict(extra="ignore")

    start_hour: int = Field(default=9, description="Start hour (0-23)")
    end_hour: int = Field(default=12, description="End hour (0-23)")
    boost: float = Field(default=1.5, description="Preference multiplier")


class ScheduledBlock(BaseModel):
    """Output assigned time block for a task."""
    model_config = ConfigDict(extra="ignore")

    task_id: str = Field(description="Identifier of the scheduled task")
    title: str = Field(description="Title of the task")
    topic_tag: str = Field(description="Curriculum topic tag")
    start_time: datetime = Field(description="Scheduled start time")
    end_time: datetime = Field(description="Scheduled end time")
    device_venue: DeviceVenue = Field(description="Device/venue assigned")
    is_emergency_floor: bool = Field(default=False, description="True if downgraded to 3-min Emergency Floor")
    was_deferred: bool = Field(default=False, description="True if task could not fit and was deferred")
    duration_min: int = Field(description="Actual assigned duration in minutes")


class ScheduleDiffItem(BaseModel):
    """Git-style schedule diff item explaining what changed and why."""
    model_config = ConfigDict(extra="ignore")

    task_id: str = Field(description="Task identifier")
    title: str = Field(description="Task title")
    old_start: Optional[datetime] = Field(default=None, description="Old start time before replan")
    old_end: Optional[datetime] = Field(default=None, description="Old end time before replan")
    new_start: Optional[datetime] = Field(default=None, description="New start time after replan")
    new_end: Optional[datetime] = Field(default=None, description="New end time after replan")
    action: DiffAction = Field(description="Diff action: UNCHANGED, MOVED, DOWNGRADED_TO_FLOOR, DEFERRED, NEWLY_SCHEDULED")
    causal_reason: str = Field(description="Plain-English explanation of why this change occurred")


class TradeoffCardType(str, Enum):
    """Capacity negotiation tradeoff options."""
    DOWNGRADE_FLOOR = "DOWNGRADE_FLOOR"
    DEFER_LOW_URGENCY = "DEFER_LOW_URGENCY"
    SPEND_FLEX_CREDITS = "SPEND_FLEX_CREDITS"


class TradeoffCard(BaseModel):
    """Slack-based capacity negotiation card presented when schedule is overloaded."""
    model_config = ConfigDict(extra="ignore")

    card_type: TradeoffCardType = Field(description="Type of tradeoff option")
    title: str = Field(description="Concise plain-English option title")
    description: str = Field(description="Clear explanation of the tradeoff")
    impact_summary: str = Field(description="Impact on workload and retention")
    recovered_minutes: int = Field(description="Minutes of deep-work pressure relieved")


class SolverRequest(BaseModel):
    """Input payload for the constraint scheduler."""
    model_config = ConfigDict(extra="ignore")

    tasks: List[SolverTask] = Field(description="Tasks to be scheduled")
    blocked_windows: List[BlockedWindow] = Field(default_factory=list, description="Hard blocked windows")
    leisure_windows: List[LeisureWindow] = Field(default_factory=list, description="Protected leisure windows")
    peak_windows: List[PeakCognitiveWindow] = Field(default_factory=list, description="Peak cognitive hours")
    horizon_start: datetime = Field(description="Start timestamp of the scheduling horizon")
    horizon_days: int = Field(default=7, description="Number of days in the scheduling horizon")
    flex_credits_available_min: int = Field(default=0, description="Available flex credits from Burnout Guard")
    timezone: str = Field(default="Asia/Kolkata", description="Target IANA timezone")


class SolverResponse(BaseModel):
    """Result of combinatorial scheduling optimization."""
    model_config = ConfigDict(populate_by_name=True)

    status: str = Field(description="Solver status: OPTIMAL | FEASIBLE | OVERLOADED_SOLVED_WITH_SLACK")
    scheduled_blocks: List[ScheduledBlock] = Field(default_factory=list, description="Final scheduled blocks")
    diff_items: List[ScheduleDiffItem] = Field(default_factory=list, description="Git-style diff and causal explanations")
    tradeoff_cards: List[TradeoffCard] = Field(default_factory=list, description="Capacity negotiation tradeoff cards if overloaded")
    total_scheduled_minutes: int = Field(default=0, description="Total minutes successfully scheduled")
    total_slack_minutes: int = Field(default=0, description="Minutes resolved via floor downgrade or deferral")
    churn_score: int = Field(default=0, description="Total slot churn distance from original schedule")
    debug_trace: Optional[DebugTrace] = Field(default=None, alias="_debug_trace", description="End-to-end execution flight recorder")
