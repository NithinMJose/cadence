import logging
from fastapi import APIRouter, Request
from app.config import get_settings
from app.llm.router import LLMRouter
from app.models.intent import ParseIntentRequest, ParseIntentResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ai", tags=["Intent Compilation"])


@router.post("/parse-intent", response_model=ParseIntentResponse)
async def parse_user_intent(
    request: Request,
    payload: ParseIntentRequest,
) -> ParseIntentResponse:
    """Translate natural language user utterance into a structured constraint AST."""
    settings = get_settings()
    trace_id = getattr(request.state, "trace_id", "trace-parse-intent")
    router_instance = LLMRouter(settings)

    return await router_instance.parse_intent(
        raw_prompt=payload.prompt,
        reference_date_iso=payload.reference_date,
        tz_name=payload.timezone,
        user_context=payload.user_context,
        trace_id=trace_id,
    )
