package com.cadence.dto;

import com.cadence.model.CardState;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.UUID;

public record CardReviewResponse(
        @JsonProperty("card_id") UUID cardId,
        String title,
        int grade,
        double stability,
        double difficulty,
        @JsonProperty("scheduled_days") int scheduledDays,
        int reps,
        int lapses,
        CardState state,
        @JsonProperty("next_review_at") ZonedDateTime nextReviewAt,
        @JsonProperty("trace_id") String traceId
) {}
