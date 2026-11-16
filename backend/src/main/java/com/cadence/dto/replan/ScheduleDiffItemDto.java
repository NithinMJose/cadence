package com.cadence.dto.replan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

/**
 * Git-style schedule diff item explaining what slot changed and why.
 */
public record ScheduleDiffItemDto(
        @JsonProperty("task_id")
        String taskId,

        @JsonProperty("title")
        String title,

        @JsonProperty("old_start")
        ZonedDateTime oldStart,

        @JsonProperty("old_end")
        ZonedDateTime oldEnd,

        @JsonProperty("new_start")
        ZonedDateTime newStart,

        @JsonProperty("new_end")
        ZonedDateTime newEnd,

        @JsonProperty("action")
        String action,

        @JsonProperty("causal_reason")
        String causalReason
) {}
