import logging
import uuid
from fastapi import APIRouter, Header, HTTPException
from app.models.autopsy import CommitAutopsyRequest, CommitAutopsyResponse
from app.models.problem_detail import ProblemDetail
from app.services.commit_autopsy_service import CommitAutopsyService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai", tags=["Commit Autopsy"])
autopsy_service = CommitAutopsyService()


@router.post(
    "/commit-autopsy",
    response_model=CommitAutopsyResponse,
    responses={400: {"model": ProblemDetail}, 500: {"model": ProblemDetail}},
    summary="Analyze code diff and generate flashcards, warm-up briefs, and micro-quizzes",
)
async def analyze_commit_diff(
    request: CommitAutopsyRequest,
    x_trace_id: str = Header(default=None, alias="X-Trace-Id"),
) -> CommitAutopsyResponse:
    trace_id = x_trace_id or f"trace-{uuid.uuid4().hex[:12]}"
    if not request.raw_diff or not request.raw_diff.strip():
        raise HTTPException(
            status_code=400,
            detail="Diff content cannot be empty for commit autopsy analysis.",
        )

    try:
        return await autopsy_service.analyze_diff(request, trace_id=trace_id)
    except Exception as e:
        logger.error("Commit autopsy failed: %s", e, exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Commit diff analysis failed: {str(e)}",
        )
