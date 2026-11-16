from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field


class ActionType(str, Enum):
    """Semantic action requested by the user's intent."""
    BLOCK = "BLOCK"
    UNBLOCK = "UNBLOCK"
    RESCHEDULE = "RESCHEDULE"
    PREFER_WINDOW = "PREFER_WINDOW"
    PROTECT_LEISURE = "PROTECT_LEISURE"
    DOWNGRADE_FLOOR = "DOWNGRADE_FLOOR"


class TargetWindow(str, Enum):
    """Symbolic daily time window."""
    MORNING = "MORNING"       # 08:00 - 12:00
    AFTERNOON = "AFTERNOON"   # 12:00 - 17:00
    EVENING = "EVENING"       # 17:00 - 21:00
    NIGHT = "NIGHT"           # 21:00 - 23:59
    ALL_DAY = "ALL_DAY"       # 00:00 - 23:59
    CUSTOM = "CUSTOM"


class DeviceRequirement(str, Enum):
    """Device constraint for the scheduled block."""
    ANY = "ANY"
    LAPTOP_ONLY = "LAPTOP_ONLY"
    PHONE_ONLY = "PHONE_ONLY"


class ConstraintItem(BaseModel):
    """Individual symbolic constraint or schedule mutation."""
    model_config = ConfigDict(extra="ignore")

    action: ActionType = Field(description="Action to perform: BLOCK | UNBLOCK | RESCHEDULE etc.")
    day_offset: Optional[int] = Field(default=None, description="Day offset relative to reference date (0=today, 1=tomorrow)")
    target_weekday: Optional[str] = Field(default=None, description="Target weekday in English (e.g. 'MONDAY', 'FRIDAY')")
    window: TargetWindow = Field(default=TargetWindow.ALL_DAY, description="Symbolic window: MORNING, AFTERNOON, EVENING, NIGHT, ALL_DAY, CUSTOM")
    custom_start_time: Optional[str] = Field(default=None, description="Custom start time in HH:MM format if window is CUSTOM")
    custom_end_time: Optional[str] = Field(default=None, description="Custom end time in HH:MM format if window is CUSTOM")
    target_topic_tag: Optional[str] = Field(default=None, description="Target topic tag if action applies to specific subject")
    device_requirement: DeviceRequirement = Field(default=DeviceRequirement.ANY, description="LAPTOP_ONLY, PHONE_ONLY, or ANY")
    reason: str = Field(description="Reason explaining the constraint or move")
    is_hard_constraint: bool = Field(default=True, description="True if block cannot be violated; False if soft preference")


class ConstraintMutationAST(BaseModel):
    """Abstract Syntax Tree produced by the LLM Intent Compiler."""
    model_config = ConfigDict(extra="ignore")

    is_ambiguous: bool = Field(default=False, description="True if user prompt lacks clear temporal or actionable constraints")
    ambiguity_reason: Optional[str] = Field(default=None, description="Explanation why the utterance is ambiguous")
    clarification_options: List[str] = Field(default_factory=list, description="1-2 concrete clarification suggestions if ambiguous")
    constraints: List[ConstraintItem] = Field(default_factory=list, description="List of extracted constraint mutations")
    summary: str = Field(description="One-sentence plain-English summary of what this mutation represents")
