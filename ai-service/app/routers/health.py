from datetime import datetime, timezone
from fastapi import APIRouter, Request
from app.config import get_settings

router = APIRouter(tags=["Health"])


@router.get("/health")
async def health_check(request: Request):
    """Public healthcheck endpoint for orchestration and load balancers."""
    settings = get_settings()
    trace_id = getattr(request.state, "trace_id", "trace-health-probe")
    return {
        "status": "UP",
        "service": settings.app_name,
        "version": settings.app_version,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "timezone": settings.default_user_timezone,
        "trace_id": trace_id,
    }
