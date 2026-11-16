import time
from datetime import datetime
from zoneinfo import ZoneInfo
from fastapi import APIRouter, Request
from app.llm.date_resolver import SymbolicDateResolver
from app.llm.router import LLMRouter
from app.models.ast import ActionType, ConstraintMutationAST
from app.models.replan import ReplanPipelineRequest, ReplanPipelineResponse
from app.models.trace import DebugTrace
from app.solver.cpsat_scheduler import CadenceCpSatSolver
from app.solver.models import BlockedWindow, LeisureWindow, SolverRequest

router = APIRouter(prefix="/api/v1/ai", tags=["AI Replan Pipeline"])
llm_router = LLMRouter()


@router.post("/replan", response_model=ReplanPipelineResponse)
async def replan_schedule(request: Request, payload: ReplanPipelineRequest) -> ReplanPipelineResponse:
    """Executes the full Replan Pipeline: Intent compilation -> Date resolution -> CP-SAT optimization."""
    trace_id = getattr(request.state, "trace_id", "trace-replan")
    t0 = time.perf_counter()
    tz = ZoneInfo(payload.timezone)

    now_tz = datetime.now(tz)
    ref_date = now_tz.date()
    horizon_start = payload.horizon_start or now_tz.replace(minute=0, second=0, microsecond=0)

    ast: ConstraintMutationAST | None = payload.constraint_ast
    llm_latency_ms = 0.0
    llm_provider = None
    redacted_count = 0

    if payload.user_message and ast is None:
        intent_resp = await llm_router.parse_intent(
            raw_prompt=payload.user_message,
            reference_date_iso=horizon_start.isoformat(),
            tz_name=payload.timezone,
            trace_id=trace_id,
        )
        ast = intent_resp.ast
        llm_latency_ms = intent_resp.debug_trace.llm_latency_ms or 0.0
        llm_provider = intent_resp.debug_trace.llm_provider_used
        redacted_count = intent_resp.debug_trace.secrets_redacted_count

    blocked_windows = list(payload.existing_blocked_windows)
    leisure_windows = list(payload.existing_leisure_windows)
    ast_summary = ast.summary if ast else "Direct schedule optimization request."

    if ast and not ast.is_ambiguous:
        resolver = SymbolicDateResolver(payload.timezone)
        resolved_windows = resolver.resolve_constraints(
            constraints=ast.constraints,
            reference_date_iso=horizon_start.isoformat(),
            tz_name=payload.timezone,
        )
        for rw in resolved_windows:
            st = datetime.fromisoformat(rw.start_time)
            et = datetime.fromisoformat(rw.end_time)
            if rw.action in (ActionType.BLOCK, ActionType.RESCHEDULE):
                blocked_windows.append(
                    BlockedWindow(start=st, end=et, reason=rw.reason, is_hard=rw.is_hard_constraint)
                )
            elif rw.action == ActionType.PROTECT_LEISURE:
                leisure_windows.append(
                    LeisureWindow(start=st, end=et, name=rw.reason, is_protected=True)
                )

    solver_req = SolverRequest(
        tasks=payload.tasks,
        blocked_windows=blocked_windows,
        leisure_windows=leisure_windows,
        peak_windows=payload.peak_windows,
        horizon_start=horizon_start,
        horizon_days=payload.horizon_days,
        flex_credits_available_min=payload.flex_credits_available_min,
        timezone=payload.timezone,
    )

    t_solver = time.perf_counter()
    solver = CadenceCpSatSolver(solver_req)
    solver_resp = solver.solve()
    solver_ms = (time.perf_counter() - t_solver) * 1000.0

    active_constraints = [
        f"tasks:{len(payload.tasks)}",
        f"blocked_windows:{len(blocked_windows)}",
        f"leisure_windows:{len(leisure_windows)}",
        "cp_sat_no_overlap",
        "sleep_hours_protected",
    ]

    debug_trace = DebugTrace(
        trace_id=trace_id,
        llm_provider_used=llm_provider,
        llm_latency_ms=llm_latency_ms,
        secrets_redacted_count=redacted_count,
        solver_status=solver_resp.status,
        solver_wall_time_ms=solver_ms,
        active_constraints_triggered=active_constraints,
    )

    return ReplanPipelineResponse(
        status=solver_resp.status,
        ast_summary=ast_summary,
        scheduled_blocks=solver_resp.scheduled_blocks,
        diff_items=solver_resp.diff_items,
        tradeoff_cards=solver_resp.tradeoff_cards,
        total_scheduled_minutes=solver_resp.total_scheduled_minutes,
        total_slack_minutes=solver_resp.total_slack_minutes,
        churn_score=solver_resp.churn_score,
        debug_trace=debug_trace,
    )
