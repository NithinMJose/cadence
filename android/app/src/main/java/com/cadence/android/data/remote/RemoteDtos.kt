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

data class ReplanProposeRequestDto(
    @SerializedName("user_message") val userMessage: String? = null
)

data class ReplanProposalResponseDto(
    @SerializedName("proposal_id") val proposalId: String,
    @SerializedName("status") val status: String,
    @SerializedName("ast_summary") val astSummary: String?,
    @SerializedName("total_scheduled_minutes") val totalScheduledMinutes: Int
)

data class ReplanConfirmResponseDto(
    @SerializedName("proposal_id") val proposalId: String,
    @SerializedName("status") val status: String,
    @SerializedName("updated_blocks_count") val updatedBlocksCount: Int
)
