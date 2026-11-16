import math
import time
from datetime import datetime, timedelta
from typing import List, Tuple
from zoneinfo import ZoneInfo
from ortools.sat.python import cp_model
from app.solver.diff_explainer import DiffExplainer
from app.solver.models import (
    BlockedWindow,
    LeisureWindow,
    ScheduledBlock,
    SolverRequest,
    SolverResponse,
    SolverTask,
)
from app.solver.tradeoff_generator import TradeoffGenerator

SLOT_MINUTES = 15


class CadenceCpSatSolver:
    """Combinatorial learning schedule optimizer powered by Google OR-Tools CP-SAT."""

    def __init__(self, request: SolverRequest):
        self.req = request
        self.tz = ZoneInfo(request.timezone)
        self.start_dt = request.horizon_start.astimezone(self.tz)
        self.num_slots = request.horizon_days * 24 * (60 // SLOT_MINUTES)
        self.model = cp_model.CpModel()
        self.intervals: List[cp_model.IntervalVar] = []

    def _dt_to_slot(self, dt: datetime) -> int:
        local_dt = dt.astimezone(self.tz)
        secs = (local_dt - self.start_dt).total_seconds()
        return max(0, min(self.num_slots - 1, int(secs // (SLOT_MINUTES * 60))))

    def _slot_to_dt(self, slot: int) -> datetime:
        return self.start_dt + timedelta(minutes=slot * SLOT_MINUTES)

    def _merge_ranges(self, raw: List[Tuple[int, int]]) -> List[Tuple[int, int]]:
        if not raw:
            return []
        sorted_r = sorted(raw, key=lambda x: x[0])
        merged = [sorted_r[0]]
        for cur_s, cur_e in sorted_r[1:]:
            prev_s, prev_e = merged[-1]
            if cur_s <= prev_e:
                merged[-1] = (prev_s, max(prev_e, cur_e))
            else:
                merged.append((cur_s, cur_e))
        return merged

    def _collect_night_ranges(self) -> List[Tuple[int, int]]:
        ranges = []
        cur = self.start_dt
        for day in range(self.req.horizon_days):
            day_start = cur + timedelta(days=day)
            s_night = self._dt_to_slot(day_start.replace(hour=23, minute=0, second=0))
            s_mid = self._dt_to_slot(day_start.replace(hour=23, minute=59, second=59)) + 1
            ranges.append((s_night, s_mid))
            s_morn_start = self._dt_to_slot(day_start.replace(hour=0, minute=0, second=0))
            s_morn_end = self._dt_to_slot(day_start.replace(hour=7, minute=0, second=0))
            ranges.append((s_morn_start, s_morn_end))
        return ranges

    def solve(self) -> SolverResponse:
        t_start = time.perf_counter()
        raw_blocked = self._collect_night_ranges()

        # 1. Enforce hard blocked windows
        for bw in self.req.blocked_windows:
            if bw.is_hard:
                s1 = self._dt_to_slot(bw.start)
                s2 = min(self.num_slots, int(math.ceil((bw.end.astimezone(self.tz) - self.start_dt).total_seconds() / (SLOT_MINUTES * 60))))
                raw_blocked.append((s1, max(s1 + 1, s2)))

        # 2. Enforce protected leisure windows
        for lw in self.req.leisure_windows:
            if lw.is_protected:
                s1 = self._dt_to_slot(lw.start)
                s2 = min(self.num_slots, int(math.ceil((lw.end.astimezone(self.tz) - self.start_dt).total_seconds() / (SLOT_MINUTES * 60))))
                raw_blocked.append((s1, max(s1 + 1, s2)))

        # Merge overlapping unavailable ranges into disjoint immovable blocks
        for raw_s1, raw_s2 in self._merge_ranges(raw_blocked):
            s1 = max(0, min(self.num_slots - 1, raw_s1))
            s2 = max(s1 + 1, min(self.num_slots, raw_s2))
            size = s2 - s1
            if size > 0:
                self.intervals.append(self.model.NewIntervalVar(s1, size, s2, f"bl_{s1}_{s2}"))

        # 3. Create task decision variables & slack variables
        task_vars = {}
        objective_terms = []

        for t in self.req.tasks:
            dur_full = max(1, math.ceil(t.duration_min / SLOT_MINUTES))
            dur_floor = 1

            b_full = self.model.NewBoolVar(f"full_{t.id}")
            b_floor = self.model.NewBoolVar(f"floor_{t.id}")
            b_def = self.model.NewBoolVar(f"def_{t.id}")
            self.model.Add(b_full + b_floor + b_def == 1)

            s_full = self.model.NewIntVar(0, self.num_slots - dur_full, f"start_full_{t.id}")
            e_full = self.model.NewIntVar(dur_full, self.num_slots, f"end_full_{t.id}")
            iv_full = self.model.NewOptionalIntervalVar(s_full, dur_full, e_full, b_full, f"iv_full_{t.id}")
            self.intervals.append(iv_full)

            s_floor = self.model.NewIntVar(0, self.num_slots - dur_floor, f"start_floor_{t.id}")
            e_floor = self.model.NewIntVar(dur_floor, self.num_slots, f"end_floor_{t.id}")
            iv_floor = self.model.NewOptionalIntervalVar(s_floor, dur_floor, e_floor, b_floor, f"iv_floor_{t.id}")
            self.intervals.append(iv_floor)

            task_vars[t.id] = (b_full, b_floor, b_def, s_full, s_floor, dur_full)

            # Objective: Maximize full sessions & urgency, penalize deferral
            urgency_factor = int(t.urgency_score * 20)
            objective_terms.append(2000 * b_full + 600 * b_floor + urgency_factor * (2 * b_full + b_floor))

            # Churn penalty: minimize deviation from original slot if previously scheduled
            if t.original_start is not None:
                orig_slot = self._dt_to_slot(t.original_start)
                churn = self.model.NewIntVar(0, self.num_slots, f"churn_{t.id}")
                self.model.Add(churn >= s_full - orig_slot)
                self.model.Add(churn >= orig_slot - s_full)
                objective_terms.append(-1 * churn)

        # 4. Enforce strict non-overlap across all active blocks and blocked windows
        self.model.AddNoOverlap(self.intervals)
        self.model.Maximize(sum(objective_terms))

        # 5. Solve CP-SAT model
        solver = cp_model.CpSolver()
        solver.parameters.max_time_in_seconds = 8.0
        solver.parameters.num_workers = 2
        status_code = solver.Solve(self.model)

        # 6. Extract scheduled results
        scheduled_blocks: List[ScheduledBlock] = []
        total_sched_min = 0
        total_slack_min = 0
        had_slack = False
        churn_total = 0

        for t in self.req.tasks:
            b_full, b_floor, b_def, s_full, s_floor, dur_full = task_vars[t.id]
            is_full = bool(solver.Value(b_full))
            is_floor = bool(solver.Value(b_floor))

            if is_full:
                slot = int(solver.Value(s_full))
                st = self._slot_to_dt(slot)
                et = st + timedelta(minutes=dur_full * SLOT_MINUTES)
                scheduled_blocks.append(
                    ScheduledBlock(
                        task_id=t.id,
                        title=t.title,
                        topic_tag=t.topic_tag,
                        start_time=st,
                        end_time=et,
                        device_venue=t.device_venue,
                        is_emergency_floor=False,
                        was_deferred=False,
                        duration_min=dur_full * SLOT_MINUTES,
                    )
                )
                total_sched_min += dur_full * SLOT_MINUTES
                if t.original_start is not None:
                    churn_total += abs(slot - self._dt_to_slot(t.original_start))
            elif is_floor:
                had_slack = True
                slot = int(solver.Value(s_floor))
                st = self._slot_to_dt(slot)
                et = st + timedelta(minutes=t.emergency_floor_duration_min)
                scheduled_blocks.append(
                    ScheduledBlock(
                        task_id=t.id,
                        title=t.title,
                        topic_tag=t.topic_tag,
                        start_time=st,
                        end_time=et,
                        device_venue=t.device_venue,
                        is_emergency_floor=True,
                        was_deferred=False,
                        duration_min=t.emergency_floor_duration_min,
                    )
                )
                total_sched_min += t.emergency_floor_duration_min
                total_slack_min += max(0, t.duration_min - t.emergency_floor_duration_min)
            else:
                had_slack = True
                total_slack_min += t.duration_min
                scheduled_blocks.append(
                    ScheduledBlock(
                        task_id=t.id,
                        title=t.title,
                        topic_tag=t.topic_tag,
                        start_time=self.start_dt,
                        end_time=self.start_dt,
                        device_venue=t.device_venue,
                        is_emergency_floor=False,
                        was_deferred=True,
                        duration_min=0,
                    )
                )

        elapsed_ms = (time.perf_counter() - t_start) * 1000.0
        final_status = (
            "OVERLOADED_SOLVED_WITH_SLACK"
            if had_slack
            else ("OPTIMAL" if status_code == cp_model.OPTIMAL else "FEASIBLE")
        )

        diffs = DiffExplainer.generate_diff(
            self.req.tasks, scheduled_blocks, self.req.blocked_windows, self.req.leisure_windows
        )
        tradeoff_cards = (
            TradeoffGenerator.generate_tradeoff_cards(
                self.req.tasks, scheduled_blocks, self.req.flex_credits_available_min
            )
            if had_slack
            else []
        )

        return SolverResponse(
            status=final_status,
            scheduled_blocks=[b for b in scheduled_blocks if not b.was_deferred],
            diff_items=diffs,
            tradeoff_cards=tradeoff_cards,
            total_scheduled_minutes=total_sched_min,
            total_slack_minutes=total_slack_min,
            churn_score=churn_total,
        )
