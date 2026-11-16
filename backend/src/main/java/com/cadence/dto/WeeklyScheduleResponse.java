package com.cadence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

public record WeeklyScheduleResponse(
        @JsonProperty("start_date") ZonedDateTime startDate,
        @JsonProperty("end_date") ZonedDateTime endDate,
        String timezone,
        @JsonProperty("total_blocks") int totalBlocks,
        List<TimeBlockDto> blocks,
        @JsonProperty("flex_credits_minutes") int flexCreditsMinutes,
        @JsonProperty("recovery_mode_active") boolean recoveryModeActive,
        @JsonProperty("trace_id") String traceId
) {}
