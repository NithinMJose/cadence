from typing import List
from pydantic import BaseModel, ConfigDict, Field
from app.models.trace import DebugTrace


class FeynmanGradeRequest(BaseModel):
    """Input payload with user spoken explanation transcript and card rubric."""

    card_title: str = Field(description="Title of the topic card being explained")
    topic_tag: str = Field(description="Topic category tag")
    concept_summary: str = Field(description="Summary of ground-truth concept")
    rubric_checkpoints: List[str] = Field(
        min_length=1,
        max_length=6,
        description="Rubric checkpoints expected in explanation",
    )
    transcript: str = Field(description="User spoken explanation transcript (up to 60s)")


class FeynmanGradeResponse(BaseModel):
    """Feynman evaluation output with FSRS grade and rubric breakdown."""

    model_config = ConfigDict(populate_by_name=True)

    fsrs_grade: int = Field(
        ge=1,
        le=4,
        description="Calculated FSRS grade: 1=Again, 2=Hard, 3=Good, 4=Easy",
    )
    matched_checkpoints: List[str] = Field(
        description="Checkpoints successfully articulated by the user",
    )
    missing_checkpoints: List[str] = Field(
        description="Checkpoints omitted or misunderstood in explanation",
    )
    feedback: str = Field(
        description="Constructive coaching feedback highlighting key omissions",
    )
    debug_trace: DebugTrace = Field(
        alias="_debug_trace",
        description="End-to-end flight recorder trace metadata",
    )
