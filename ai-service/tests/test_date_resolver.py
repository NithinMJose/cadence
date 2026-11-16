from app.llm.date_resolver import SymbolicDateResolver
from app.models.ast import ActionType, ConstraintItem, DeviceRequirement, TargetWindow


def test_resolve_today_evening():
    resolver = SymbolicDateResolver(default_timezone="Asia/Kolkata")
    ref_iso = "2026-11-16T10:00:00+05:30"  # A Monday

    constraints = [
        ConstraintItem(
            action=ActionType.BLOCK,
            day_offset=0,
            window=TargetWindow.EVENING,
            reason="Dinner with family",
        )
    ]
    resolved = resolver.resolve_constraints(constraints, reference_date_iso=ref_iso)
    assert len(resolved) == 1
    w = resolved[0]
    assert w.action == ActionType.BLOCK
    assert "2026-11-16T17:00:00+05:30" in w.start_time
    assert "2026-11-16T21:00:00+05:30" in w.end_time
    assert w.reason == "Dinner with family"


def test_resolve_target_weekday_friday():
    resolver = SymbolicDateResolver(default_timezone="Asia/Kolkata")
    ref_iso = "2026-11-16T10:00:00+05:30"  # Monday, Nov 16, 2026

    constraints = [
        ConstraintItem(
            action=ActionType.BLOCK,
            target_weekday="FRIDAY",
            window=TargetWindow.MORNING,
            reason="Team hackathon",
        )
    ]
    resolved = resolver.resolve_constraints(constraints, reference_date_iso=ref_iso)
    assert len(resolved) == 1
    w = resolved[0]
    # Friday is Nov 20, 2026
    assert "2026-11-20T08:00:00+05:30" in w.start_time
    assert "2026-11-20T12:00:00+05:30" in w.end_time


def test_resolve_custom_window():
    resolver = SymbolicDateResolver(default_timezone="Asia/Kolkata")
    ref_iso = "2026-11-16T10:00:00+05:30"

    constraints = [
        ConstraintItem(
            action=ActionType.BLOCK,
            day_offset=1,
            window=TargetWindow.CUSTOM,
            custom_start_time="14:30",
            custom_end_time="16:00",
            device_requirement=DeviceRequirement.PHONE_ONLY,
            reason="Bus commute",
        )
    ]
    resolved = resolver.resolve_constraints(constraints, reference_date_iso=ref_iso)
    assert len(resolved) == 1
    w = resolved[0]
    # Tuesday is Nov 17, 2026
    assert "2026-11-17T14:30:00+05:30" in w.start_time
    assert "2026-11-17T16:00:00+05:30" in w.end_time
    assert w.device_requirement == DeviceRequirement.PHONE_ONLY
