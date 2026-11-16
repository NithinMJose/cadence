from typing import List
from app.solver.models import ScheduledBlock, SolverTask, TradeoffCard, TradeoffCardType


class TradeoffGenerator:
    """Generates Slack-Based Overload Tradeoff Cards when schedule capacity is exceeded."""

    @staticmethod
    def generate_tradeoff_cards(
        tasks: List[SolverTask],
        scheduled_blocks: List[ScheduledBlock],
        flex_credits_available_min: int = 0,
    ) -> List[TradeoffCard]:
        """Constructs three actionable capacity negotiation cards for the user."""
        cards: List[TradeoffCard] = []

        floor_blocks = [b for b in scheduled_blocks if b.is_emergency_floor]
        deferred_blocks = [b for b in scheduled_blocks if b.was_deferred]

        floor_relief_min = sum(
            max(0, 45 - b.duration_min) for b in floor_blocks
        )
        deferred_relief_min = sum(
            t.duration_min for t in tasks if any(d.task_id == t.id for d in deferred_blocks)
        )

        # Card 1: Downgrade to Emergency Floor
        if floor_blocks or deferred_blocks:
            downgrade_count = len(floor_blocks) if floor_blocks else min(len(tasks), 2)
            est_relief = floor_relief_min if floor_relief_min > 0 else downgrade_count * 40
            cards.append(
                TradeoffCard(
                    card_type=TradeoffCardType.DOWNGRADE_FLOOR,
                    title="One-Tap Downgrade to Emergency Phone Floor",
                    description=(
                        f"Convert {downgrade_count} demanding laptop sessions into 3-minute "
                        "phone micro-quizzes to protect your habit without burnout."
                    ),
                    impact_summary=f"Saves ~{est_relief} minutes while keeping your daily learning streak alive.",
                    recovered_minutes=est_relief,
                )
            )

        # Card 2: Defer lowest-urgency topic
        if deferred_blocks or len(tasks) > 3:
            defer_count = len(deferred_blocks) if deferred_blocks else 1
            est_relief = deferred_relief_min if deferred_relief_min > 0 else 45
            cards.append(
                TradeoffCard(
                    card_type=TradeoffCardType.DEFER_LOW_URGENCY,
                    title="Defer Lowest-Urgency Topic to Next Cycle",
                    description=(
                        f"Postpone {defer_count} lowest-priority concept to next week "
                        "so high-urgency FSRS due reviews receive full focus."
                    ),
                    impact_summary=f"Recovers ~{est_relief} minutes of deep-work capacity.",
                    recovered_minutes=est_relief,
                )
            )

        # Card 3: Spend banked Flex Credits
        if flex_credits_available_min > 0:
            usable_credits = min(flex_credits_available_min, max(30, floor_relief_min + deferred_relief_min))
            cards.append(
                TradeoffCard(
                    card_type=TradeoffCardType.SPEND_FLEX_CREDITS,
                    title="Redeem Banked Flex-Time Credits",
                    description=(
                        f"Apply {usable_credits} minutes of your banked flex credits "
                        "(earned by completing deep work early) to offset this week's load."
                    ),
                    impact_summary=f"Absorbs {usable_credits} minutes without sacrificing curriculum goals.",
                    recovered_minutes=usable_credits,
                )
            )

        return cards
