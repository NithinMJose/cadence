from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.solver.cpsat_scheduler import CadenceCpSatSolver
from app.solver.diff_explainer import DiffExplainer
from app.solver.models import (
    BlockedWindow,
    DiffAction,
    LeisureWindow,
    SolverRequest,
    SolverTask,
)

client = TestClient(app)
TZ = ZoneInfo("Asia/Kolkata")
HORIZON_START = datetime(2026, 11, 16, 8, 0, tzinfo=TZ)


def test_cpsat_solver_no_overlap():
    """Verify that OR-Tools CP-SAT guarantees 0 overlapping task intervals."""
    tasks = [
        SolverTask(id=f"task-{i}", title=f"Study Session {i}", topic_tag="dsa", duration_min=45)
        for i in range(5)
    ]
    req = SolverRequest(
        tasks=tasks,
        horizon_start=HORIZON_START,
        horizon_days=2,
        timezone="Asia/Kolkata",
    )
    solver = CadenceCpSatSolver(req)
    resp = solver.solve()

    assert resp.status in ("OPTIMAL", "FEASIBLE")
    assert len(resp.scheduled_blocks) == 5

    # Check pair-wise non-overlap
    blocks = sorted(resp.scheduled_blocks, key=lambda b: b.start_time)
    for i in range(len(blocks) - 1):
        assert blocks[i].end_time <= blocks[i + 1].start_time, (
            f"Overlap detected: Block {blocks[i].title} ends at {blocks[i].end_time} "
            f"but next block starts at {blocks[i + 1].start_time}"
        )


def test_cpsat_solver_respects_blocked_and_leisure_windows():
    """Verify scheduled tasks never land on hard blocked or protected leisure slots."""
    blocked = [
        BlockedWindow(
            start=HORIZON_START + timedelta(hours=2),
            end=HORIZON_START + timedelta(hours=6),
            reason="Company All-Hands Meeting",
            is_hard=True,
        )
    ]
    leisure = [
        LeisureWindow(
            start=HORIZON_START + timedelta(hours=8),
            end=HORIZON_START + timedelta(hours=10),
            name="Gym & Dinner",
            is_protected=True,
        )
    ]
    tasks = [
        SolverTask(id=f"task-{i}", title=f"Core Work {i}", topic_tag="ai", duration_min=60)
        for i in range(4)
    ]
    req = SolverRequest(
        tasks=tasks,
        blocked_windows=blocked,
        leisure_windows=leisure,
        horizon_start=HORIZON_START,
        horizon_days=2,
        timezone="Asia/Kolkata",
    )
    solver = CadenceCpSatSolver(req)
    resp = solver.solve()

    assert len(resp.scheduled_blocks) == 4
    for b in resp.scheduled_blocks:
        # Check against blocked window
        assert not (b.start_time < blocked[0].end and b.end_time > blocked[0].start), (
            f"Block {b.title} ({b.start_time}-{b.end_time}) overlapped with blocked window!"
        )
        # Check against leisure window
        assert not (b.start_time < leisure[0].end and b.end_time > leisure[0].start), (
            f"Block {b.title} ({b.start_time}-{b.end_time}) overlapped with leisure window!"
        )


def test_cpsat_solver_overload_slack_and_tradeoff_cards():
    """Verify solver prevents INFEASIBLE crash when overloaded by using slack & generating cards."""
    # Create 20 long tasks in a short 1-day horizon (impossible to schedule fully)
    tasks = [
        SolverTask(
            id=f"heavy-{i}",
            title=f"Heavy Topic {i}",
            topic_tag="system_design",
            duration_min=90,
            urgency_score=float(i),
        )
        for i in range(15)
    ]
    req = SolverRequest(
        tasks=tasks,
        horizon_start=HORIZON_START,
        horizon_days=1,
        flex_credits_available_min=60,
        timezone="Asia/Kolkata",
    )
    solver = CadenceCpSatSolver(req)
    resp = solver.solve()

    assert resp.status == "OVERLOADED_SOLVED_WITH_SLACK"
    assert resp.total_slack_minutes > 0
    assert len(resp.tradeoff_cards) >= 2

    # Check tradeoff cards contain plain-English options
    card_types = [c.card_type for c in resp.tradeoff_cards]
    assert "DOWNGRADE_FLOOR" in card_types
    assert "SPEND_FLEX_CREDITS" in card_types


def test_diff_explainer_causal_reasons():
    """Verify Git-style diff generator explains slot movement causality."""
    old_start = HORIZON_START + timedelta(hours=1)
    old_end = old_start + timedelta(minutes=45)

    task = SolverTask(
        id="moved-task",
        title="Spring Security OAuth2",
        topic_tag="backend",
        duration_min=45,
        original_start=old_start,
        original_end=old_end,
    )
    blocked = [
        BlockedWindow(
            start=old_start - timedelta(minutes=15),
            end=old_end + timedelta(minutes=15),
            reason="Dentist Appointment",
            is_hard=True,
        )
    ]
    new_start = HORIZON_START + timedelta(hours=4)
    new_end = new_start + timedelta(minutes=45)

    from app.solver.models import ScheduledBlock
    scheduled = [
        ScheduledBlock(
            task_id="moved-task",
            title="Spring Security OAuth2",
            topic_tag="backend",
            start_time=new_start,
            end_time=new_end,
            device_venue="LAPTOP_DEEP",
            is_emergency_floor=False,
            was_deferred=False,
            duration_min=45,
        )
    ]

    diffs = DiffExplainer.generate_diff([task], scheduled, blocked, [])
    assert len(diffs) == 1
    diff = diffs[0]
    assert diff.action == DiffAction.MOVED
    assert "Dentist Appointment" in diff.causal_reason
    assert "Moved from" in diff.causal_reason


def test_solver_api_endpoint():
    """Verify POST /api/v1/solver/solve returns valid SolverResponse with debug trace."""
    payload = {
        "tasks": [
            {
                "id": "card-1",
                "title": "FSRS Math Port",
                "topic_tag": "math",
                "duration_min": 45,
                "urgency_score": 8.5,
            }
        ],
        "horizon_start": HORIZON_START.isoformat(),
        "horizon_days": 2,
        "timezone": "Asia/Kolkata",
    }
    resp = client.post("/api/v1/solver/solve", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] in ("OPTIMAL", "FEASIBLE")
    assert len(data["scheduled_blocks"]) == 1
    assert data["_debug_trace"]["solver_status"] in ("OPTIMAL", "FEASIBLE")
    assert data["_debug_trace"]["solver_wall_time_ms"] > 0


def test_replan_pipeline_api_endpoint():
    """Verify POST /api/v1/ai/replan runs end-to-end intent, resolver, and CP-SAT scheduler."""
    payload = {
        "user_message": "Block Friday evening from 6pm to 9pm for team dinner",
        "tasks": [
            {
                "id": "task-algo",
                "title": "Dynamic Programming",
                "topic_tag": "dsa",
                "duration_min": 45,
                "urgency_score": 7.0,
            }
        ],
        "horizon_start": HORIZON_START.isoformat(),
        "horizon_days": 7,
        "timezone": "Asia/Kolkata",
    }
    resp = client.post("/api/v1/ai/replan", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert "scheduled_blocks" in data
    assert len(data["scheduled_blocks"]) == 1
    assert "_debug_trace" in data
    assert data["_debug_trace"]["solver_status"] in ("OPTIMAL", "FEASIBLE")
