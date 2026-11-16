import time
from fastapi import APIRouter, Request
from app.models.trace import DebugTrace
from app.solver.cpsat_scheduler import CadenceCpSatSolver
from app.solver.models import SolverRequest, SolverResponse

router = APIRouter(prefix="/api/v1/solver", tags=["Constraint Solver"])


@router.post("/solve", response_model=SolverResponse)
async def solve_schedule(request: Request, payload: SolverRequest) -> SolverResponse:
    """Solves the combinatorial learning schedule with Google OR-Tools CP-SAT."""
    trace_id = getattr(request.state, "trace_id", "trace-solver")
    t0 = time.perf_counter()

    solver = CadenceCpSatSolver(payload)
    response = solver.solve()
    elapsed_ms = (time.perf_counter() - t0) * 1000.0

    active_constraints = [
        f"tasks_count:{len(payload.tasks)}",
        f"blocked_windows:{len(payload.blocked_windows)}",
        f"leisure_windows:{len(payload.leisure_windows)}",
        "no_overlap_interval",
        "night_sleep_protection",
    ]

    response.debug_trace = DebugTrace(
        trace_id=trace_id,
        solver_status=response.status,
        solver_wall_time_ms=elapsed_ms,
        active_constraints_triggered=active_constraints,
    )
    return response
