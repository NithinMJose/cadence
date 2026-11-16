import time
import uuid
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.config import get_settings
from app.models.problem_detail import CadenceServiceException, ProblemDetail
from app.routers import gpu, health

settings = get_settings()

app = FastAPI(
    title="Cadence AI & Solver Service",
    description="Stateless Intent Extraction, Commit Autopsy & Constraint Scheduling Engine",
    version=settings.app_version,
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def trace_and_timing_middleware(request: Request, call_next):
    """Enforce X-Trace-Id propagation and execution timing across all requests."""
    trace_id = request.headers.get("X-Trace-Id") or f"trace-{uuid.uuid4().hex[:12]}"
    request.state.trace_id = trace_id

    start_time = time.perf_counter()
    response = await call_next(request)
    elapsed_ms = (time.perf_counter() - start_time) * 1000.0

    response.headers["X-Trace-Id"] = trace_id
    response.headers["X-Response-Time-Ms"] = f"{elapsed_ms:.2f}"
    return response


@app.exception_handler(CadenceServiceException)
async def cadence_service_exception_handler(
    request: Request, exc: CadenceServiceException
) -> JSONResponse:
    """Serialize application errors into standard RFC 7807 ProblemDetail format."""
    trace_id = getattr(request.state, "trace_id", "unknown-trace")
    problem = ProblemDetail(
        title=exc.error_code,
        status=exc.status_code,
        detail=exc.message,
        instance=str(request.url.path),
        error_code=exc.error_code,
        trace_id=trace_id,
        remediation_hint=exc.remediation_hint,
        invalid_params=exc.invalid_params,
    )
    return JSONResponse(
        status_code=exc.status_code,
        content=problem.model_dump(exclude_none=True),
        headers={"Content-Type": "application/problem+json", "X-Trace-Id": trace_id},
    )


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Catch-all handler converting uncaught errors to RFC 7807 ProblemDetail."""
    trace_id = getattr(request.state, "trace_id", "unknown-trace")
    problem = ProblemDetail(
        title="INTERNAL_SERVER_ERROR",
        status=500,
        detail=str(exc),
        instance=str(request.url.path),
        error_code="INTERNAL_ERROR",
        trace_id=trace_id,
        remediation_hint="Inspect ai-service logs with the provided trace_id.",
    )
    return JSONResponse(
        status_code=500,
        content=problem.model_dump(exclude_none=True),
        headers={"Content-Type": "application/problem+json", "X-Trace-Id": trace_id},
    )


# Mount routers
app.include_router(health.router)
app.include_router(gpu.router)
