from typing import Any, Dict, Optional
from pydantic import BaseModel, Field


class ProblemDetail(BaseModel):
    """RFC 7807 compliant problem detail response for errors."""

    type: str = Field(
        default="about:blank",
        description="URI reference identifying the problem type",
    )
    title: str = Field(description="Short human-readable summary of problem")
    status: int = Field(description="HTTP status code")
    detail: str = Field(description="Human-readable explanation of error")
    instance: Optional[str] = Field(
        default=None,
        description="URI reference that identifies the specific occurrence",
    )
    error_code: str = Field(description="Machine-readable application error code")
    trace_id: str = Field(description="Trace ID for debugging and correlation")
    remediation_hint: str = Field(
        description="Actionable instruction on how the caller can remediate the error",
    )
    invalid_params: Optional[Dict[str, Any]] = Field(
        default=None,
        description="Detailed field-level validation errors if applicable",
    )


class CadenceServiceException(Exception):
    """Application-level exception mapping directly to RFC 7807 ProblemDetail."""

    def __init__(
        self,
        status_code: int,
        error_code: str,
        message: str,
        remediation_hint: str,
        invalid_params: Optional[Dict[str, Any]] = None,
    ):
        super().__init__(message)
        self.status_code = status_code
        self.error_code = error_code
        self.message = message
        self.remediation_hint = remediation_hint
        self.invalid_params = invalid_params
