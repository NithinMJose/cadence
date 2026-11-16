from typing import List, Optional
from pydantic import BaseModel, ConfigDict, Field
from app.models.trace import DebugTrace


class GeneratedFlashcard(BaseModel):
    """An FSRS flashcard synthesized from code diff analysis."""

    title: str = Field(description="Concise topic/concept title")
    topic_tag: str = Field(default="engineering", description="Topic identifier tag")
    summary: str = Field(description="Core insight or edge case discovered in diff")
    emergency_floor_prompt: str = Field(description="3-minute micro-quiz question")
    rubric_checkpoints: List[str] = Field(
        min_length=4,
        max_length=4,
        description="Exactly 4 grading checkpoints for Feynman evaluation",
    )


class CommitAutopsyRequest(BaseModel):
    """Input payload containing git diff to inspect."""

    raw_diff: str = Field(description="Raw git diff text, potentially containing secrets")
    commit_message: Optional[str] = Field(default=None, description="Optional commit message")
    repo_name: Optional[str] = Field(default=None, description="Repository name")
    author: Optional[str] = Field(default=None, description="Commit author")


class CommitAutopsyResponse(BaseModel):
    """Structured learning artifacts derived from git diff."""

    model_config = ConfigDict(populate_by_name=True)

    session_warmup_brief: List[str] = Field(
        min_length=3,
        max_length=3,
        description="3-bullet context refresher for next deep coding session",
    )
    emergency_floor_microquiz: str = Field(
        description="3-minute bug spotting or edge case micro-quiz",
    )
    flashcards: List[GeneratedFlashcard] = Field(
        min_length=1,
        max_length=2,
        description="1-2 flashcards targeting complexity or edge cases",
    )
    secrets_redacted_count: int = Field(
        default=0,
        description="Total secrets redacted by SecretScrubber before LLM analysis",
    )
    debug_trace: DebugTrace = Field(
        alias="_debug_trace",
        description="End-to-end flight recorder trace metadata",
    )
