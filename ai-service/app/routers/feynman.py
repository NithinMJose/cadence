import logging
import uuid
from fastapi import APIRouter, Header, HTTPException
from app.models.feynman import FeynmanGradeRequest, FeynmanGradeResponse
from app.models.problem_detail import ProblemDetail
from app.services.feynman_grader_service import FeynmanGraderService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai", tags=["Feynman Voice Grader"])
feynman_service = FeynmanGraderService()


@router.post(
    "/feynman-grade",
    response_model=FeynmanGradeResponse,
    responses={400: {"model": ProblemDetail}, 500: {"model": ProblemDetail}},
    summary="Grade a 60-second Feynman concept explanation transcript against rubrics",
)
async def grade_feynman_explanation(
    request: FeynmanGradeRequest,
    x_trace_id: str = Header(default=None, alias="X-Trace-Id"),
) -> FeynmanGradeResponse:
    trace_id = x_trace_id or f"trace-{uuid.uuid4().hex[:12]}"
    if not request.rubric_checkpoints:
        raise HTTPException(
            status_code=400,
            detail="At least one rubric checkpoint is required for Feynman evaluation.",
        )

    try:
        return await feynman_service.grade_explanation(request, trace_id=trace_id)
    except Exception as e:
        logger.error("Feynman grading failed: %s", e, exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Feynman grading failed: {str(e)}",
        )
