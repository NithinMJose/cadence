package com.cadence.dto;

import com.cadence.model.BlockStatus;
import com.cadence.model.DeviceType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.UUID;

public record TimeBlockDto(
        UUID id,
        @JsonProperty("card_id") UUID cardId,
        String title,
        @JsonProperty("topic_tag") String topicTag,
        @JsonProperty("start_time") ZonedDateTime startTime,
        @JsonProperty("end_time") ZonedDateTime endTime,
        BlockStatus status,
        @JsonProperty("device_type") DeviceType deviceType,
        @JsonProperty("planned_duration_min") int plannedDurationMin,
        @JsonProperty("actual_duration_min") Integer actualDurationMin,
        @JsonProperty("is_emergency_floor") boolean isEmergencyFloor,
        @JsonProperty("warmup_brief_json") String warmupBriefJson
) {}
