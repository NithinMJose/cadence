from typing import Dict, List
from app.solver.models import (
    BlockedWindow,
    DiffAction,
    LeisureWindow,
    ScheduleDiffItem,
    ScheduledBlock,
    SolverTask,
)


class DiffExplainer:
    """Generates Git-style schedule diffs with deterministic plain-English causal explanations."""

    @staticmethod
    def generate_diff(
        tasks: List[SolverTask],
        scheduled_blocks: List[ScheduledBlock],
        blocked_windows: List[BlockedWindow],
        leisure_windows: List[LeisureWindow],
    ) -> List[ScheduleDiffItem]:
        """Compares previous schedule against newly solved schedule and explains moves."""
        block_by_task: Dict[str, ScheduledBlock] = {b.task_id: b for b in scheduled_blocks}
        diff_items: List[ScheduleDiffItem] = []

        for task in tasks:
            block = block_by_task.get(task.id)

            # Case 1: Deferred task
            if block is None or block.was_deferred:
                diff_items.append(
                    ScheduleDiffItem(
                        task_id=task.id,
                        title=task.title,
                        old_start=task.original_start,
                        old_end=task.original_end,
                        new_start=None,
                        new_end=None,
                        action=DiffAction.DEFERRED,
                        causal_reason=(
                            f"Deferred '{task.title}' to next cycle to protect higher-urgency "
                            "FSRS reviews during calendar congestion."
                        ),
                    )
                )
                continue

            # Case 2: Downgraded to 3-min Emergency Floor
            if block.is_emergency_floor:
                diff_items.append(
                    ScheduleDiffItem(
                        task_id=task.id,
                        title=task.title,
                        old_start=task.original_start,
                        old_end=task.original_end,
                        new_start=block.start_time,
                        new_end=block.end_time,
                        action=DiffAction.DOWNGRADED_TO_FLOOR,
                        causal_reason=(
                            f"Downgraded '{task.title}' to 3-minute Emergency Phone Floor "
                            "to maintain streak while preventing overload."
                        ),
                    )
                )
                continue

            # Case 3: Newly scheduled task (no previous slot)
            if task.original_start is None:
                start_str = block.start_time.strftime("%a %H:%M")
                diff_items.append(
                    ScheduleDiffItem(
                        task_id=task.id,
                        title=task.title,
                        old_start=None,
                        old_end=None,
                        new_start=block.start_time,
                        new_end=block.end_time,
                        action=DiffAction.NEWLY_SCHEDULED,
                        causal_reason=f"Newly scheduled at {start_str} in optimal cognitive focus window.",
                    )
                )
                continue

            # Case 4: Preserved in same slot
            if task.original_start == block.start_time:
                diff_items.append(
                    ScheduleDiffItem(
                        task_id=task.id,
                        title=task.title,
                        old_start=task.original_start,
                        old_end=task.original_end,
                        new_start=block.start_time,
                        new_end=block.end_time,
                        action=DiffAction.UNCHANGED,
                        causal_reason="Preserved in original time slot with zero conflicting constraints.",
                    )
                )
                continue

            # Case 5: Moved to a different slot
            old_str = task.original_start.strftime("%a %H:%M")
            new_str = block.start_time.strftime("%a %H:%M")

            # Check if original slot overlapped with blocked window
            conflicting_block = next(
                (
                    b
                    for b in blocked_windows
                    if b.is_hard
                    and task.original_start < b.end
                    and (task.original_end or task.original_start) > b.start
                ),
                None,
            )
            if conflicting_block:
                reason = (
                    f"Moved from {old_str} -> {new_str} because original time was blocked by "
                    f"'{conflicting_block.reason}'."
                )
            else:
                # Check leisure conflict
                conflicting_leisure = next(
                    (
                        l
                        for l in leisure_windows
                        if l.is_protected
                        and task.original_start < l.end
                        and (task.original_end or task.original_start) > l.start
                    ),
                    None,
                )
                if conflicting_leisure:
                    reason = (
                        f"Moved from {old_str} -> {new_str} to protect leisure window "
                        f"'{conflicting_leisure.name}'."
                    )
                else:
                    reason = (
                        f"Moved from {old_str} -> {new_str} to optimize peak cognitive alignment "
                        "and eliminate schedule overlap."
                    )

            diff_items.append(
                ScheduleDiffItem(
                    task_id=task.id,
                    title=task.title,
                    old_start=task.original_start,
                    old_end=task.original_end,
                    new_start=block.start_time,
                    new_end=block.end_time,
                    action=DiffAction.MOVED,
                    causal_reason=reason,
                )
            )

        return diff_items
