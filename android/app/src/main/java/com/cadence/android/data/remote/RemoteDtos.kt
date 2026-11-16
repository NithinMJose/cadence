package com.cadence.android.data.remote

import com.google.gson.annotations.SerializedName

data class WeeklyScheduleDto(
    @SerializedName("window_start") val windowStart: String,
    @SerializedName("window_end") val windowEnd: String,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("total_blocks") val totalBlocks: Int,
    @SerializedName("blocks") val blocks: List<TimeBlockRemoteDto>,
    @SerializedName("flex_credits_minutes") val flexCreditsMinutes: Int,
    @SerializedName("recovery_mode_active") val recoveryModeActive: Boolean,
    @SerializedName("trace_id") val traceId: String?
)

data class TimeBlockRemoteDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("topic_tag") val topicTag: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("status") val status: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("planned_duration_min") val plannedDurationMin: Int,
    @SerializedName("is_emergency_floor") val isEmergencyFloor: Boolean,
    @SerializedName("warmup_brief_json") val warmupBriefJson: String?
)

data class CompleteBlockRequestDto(
    @SerializedName("actual_duration_min") val actualDurationMin: Int,
    @SerializedName("rating") val rating: Int = 3
)

data class CompleteBlockResponseDto(
    @SerializedName("block_id") val blockId: String,
    @SerializedName("status") val status: String,
    @SerializedName("flex_credits_earned") val flexCreditsEarned: Int,
    @SerializedName("total_flex_credits") val totalFlexCredits: Int
)

data class TopicCardRemoteDto(
    @SerializedName("id") val id: String,
    @SerializedName("goal_id") val goalId: String?,
    @SerializedName("title") val title: String,
    @SerializedName("concept_summary") val conceptSummary: String,
    @SerializedName("topic_tag") val topicTag: String,
    @SerializedName("stability") val stability: Double,
    @SerializedName("difficulty") val difficulty: Double,
    @SerializedName("elapsed_days") val elapsedDays: Int,
    @SerializedName("scheduled_days") val scheduledDays: Int,
    @SerializedName("reps") val reps: Int,
    @SerializedName("lapses") val lapses: Int,
    @SerializedName("state") val state: String,
    @SerializedName("last_review_at") val lastReviewAt: String?,
    @SerializedName("next_review_at") val nextReviewAt: String?,
    @SerializedName("emergency_floor_prompt") val emergencyFloorPrompt: String,
    @SerializedName("rubric_json") val rubricJson: String?
)

data class CardReviewRequestDto(
    @SerializedName("grade") val grade: Int
)

data class CardReviewResponseDto(
    @SerializedName("card_id") val cardId: String,
    @SerializedName("title") val title: String,
    @SerializedName("grade") val grade: Int,
    @SerializedName("stability") val stability: Double,
    @SerializedName("difficulty") val difficulty: Double,
    @SerializedName("scheduled_days") val scheduledDays: Int,
    @SerializedName("reps") val reps: Int,
    @SerializedName("lapses") val lapses: Int,
    @SerializedName("state") val state: String,
    @SerializedName("next_review_at") val nextReviewAt: String,
    @SerializedName("trace_id") val traceId: String?
)

data class ReplanProposeRequestDto(
    @SerializedName("user_message") val userMessage: String? = null
)

data class ScheduleDiffItemRemoteDto(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("title") val title: String,
    @SerializedName("old_start") val oldStart: String?,
    @SerializedName("old_end") val oldEnd: String?,
    @SerializedName("new_start") val newStart: String?,
    @SerializedName("new_end") val newEnd: String?,
    @SerializedName("action") val action: String,
    @SerializedName("causal_reason") val causalReason: String
)

data class TradeoffCardRemoteDto(
    @SerializedName("card_type") val cardType: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("impact_summary") val impactSummary: String,
    @SerializedName("recovered_minutes") val recoveredMinutes: Int
)

data class ReplanProposalResponseDto(
    @SerializedName("proposal_id") val proposalId: String,
    @SerializedName("status") val status: String,
    @SerializedName("schedule_version") val scheduleVersion: Int = 1,
    @SerializedName("ast_summary") val astSummary: String?,
    @SerializedName("diff_items") val diffItems: List<ScheduleDiffItemRemoteDto> = emptyList(),
    @SerializedName("tradeoff_cards") val tradeoffCards: List<TradeoffCardRemoteDto> = emptyList(),
    @SerializedName("total_scheduled_minutes") val totalScheduledMinutes: Int = 0,
    @SerializedName("total_slack_minutes") val totalSlackMinutes: Int = 0,
    @SerializedName("churn_score") val churnScore: Int = 0
)

data class ReplanConfirmResponseDto(
    @SerializedName("proposal_id") val proposalId: String,
    @SerializedName("status") val status: String,
    @SerializedName("updated_blocks_count") val updatedBlocksCount: Int
)
