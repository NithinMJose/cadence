from datetime import datetime, time, timedelta
from typing import List, Optional, Tuple
from zoneinfo import ZoneInfo
from app.models.ast import ConstraintItem, TargetWindow
from app.models.intent import ResolvedWindow

WEEKDAY_NAME_TO_INT = {
    "MONDAY": 0,
    "TUESDAY": 1,
    "WEDNESDAY": 2,
    "THURSDAY": 3,
    "FRIDAY": 4,
    "SATURDAY": 5,
    "SUNDAY": 6,
}

WINDOW_HOURS: dict[TargetWindow, Tuple[time, time]] = {
    TargetWindow.MORNING: (time(8, 0), time(12, 0)),
    TargetWindow.AFTERNOON: (time(12, 0), time(17, 0)),
    TargetWindow.EVENING: (time(17, 0), time(21, 0)),
    TargetWindow.NIGHT: (time(21, 0), time(23, 59)),
    TargetWindow.ALL_DAY: (time(0, 0), time(23, 59)),
}


class SymbolicDateResolver:
    """Deterministic resolver translating symbolic LLM anchors into exact timezone-aware timestamps."""

    def __init__(self, default_timezone: str = "Asia/Kolkata"):
        self.default_timezone = default_timezone

    def resolve_constraints(
        self,
        constraints: List[ConstraintItem],
        reference_date_iso: Optional[str] = None,
        tz_name: Optional[str] = None,
    ) -> List[ResolvedWindow]:
        """Convert a list of symbolic ConstraintItems into exact ISO-8601 ResolvedWindows."""
        target_tz = ZoneInfo(tz_name or self.default_timezone)

        if reference_date_iso:
            try:
                base_dt = datetime.fromisoformat(reference_date_iso)
                if base_dt.tzinfo is None:
                    base_dt = base_dt.replace(tzinfo=target_tz)
                else:
                    base_dt = base_dt.astimezone(target_tz)
            except ValueError:
                base_dt = datetime.now(target_tz)
        else:
            base_dt = datetime.now(target_tz)

        resolved_windows: List[ResolvedWindow] = []
        for item in constraints:
            target_date = self._compute_target_date(base_dt, item)
            start_time, end_time = self._compute_window_times(item)

            start_dt = datetime.combine(target_date, start_time, tzinfo=target_tz)
            end_dt = datetime.combine(target_date, end_time, tzinfo=target_tz)

            resolved_windows.append(
                ResolvedWindow(
                    action=item.action,
                    start_time=start_dt.isoformat(),
                    end_time=end_dt.isoformat(),
                    topic_tag=item.target_topic_tag,
                    device_requirement=item.device_requirement,
                    is_hard_constraint=item.is_hard_constraint,
                    reason=item.reason,
                )
            )

        return resolved_windows

    def _compute_target_date(self, base_dt: datetime, item: ConstraintItem):
        """Determine target calendar date using day_offset or target_weekday."""
        if item.day_offset is not None:
            return (base_dt + timedelta(days=item.day_offset)).date()

        if item.target_weekday:
            target_dow = WEEKDAY_NAME_TO_INT.get(item.target_weekday.upper())
            if target_dow is not None:
                current_dow = base_dt.weekday()
                days_ahead = (target_dow - current_dow) % 7
                # If target is today and current time is late, or caller explicitly asked for that weekday
                return (base_dt + timedelta(days=days_ahead)).date()

        return base_dt.date()

    def _compute_window_times(self, item: ConstraintItem) -> Tuple[time, time]:
        """Compute start and end time boundaries for the item's target window."""
        if item.window == TargetWindow.CUSTOM:
            try:
                start_h, start_m = map(int, (item.custom_start_time or "09:00").split(":"))
                end_h, end_m = map(int, (item.custom_end_time or "18:00").split(":"))
                return time(start_h, start_m), time(end_h, end_m)
            except Exception:
                return time(9, 0), time(18, 0)

        return WINDOW_HOURS.get(item.window, (time(0, 0), time(23, 59)))
