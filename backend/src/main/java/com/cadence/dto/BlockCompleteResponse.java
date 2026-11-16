package com.cadence.dto;

import com.cadence.model.BlockStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record BlockCompleteResponse(
        @JsonProperty("block_id") UUID blockId,
        BlockStatus status,
        @JsonProperty("planned_duration_min") int plannedDurationMin,
        @JsonProperty("actual_duration_min") int actualDurationMin,
        @JsonProperty("new_topic_ema_multiplier") double newTopicEmaMultiplier,
        @JsonProperty("flex_credits_banked") int flexCreditsBanked,
        @JsonProperty("recovery_mode_active") boolean recoveryModeActive,
        @JsonProperty("trace_id") String traceId
) {}
