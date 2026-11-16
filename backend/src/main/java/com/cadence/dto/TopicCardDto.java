package com.cadence.dto;

import com.cadence.model.CardState;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.UUID;

public record TopicCardDto(
        UUID id,
        @JsonProperty("goal_id") UUID goalId,
        String title,
        @JsonProperty("concept_summary") String conceptSummary,
        @JsonProperty("topic_tag") String topicTag,
        double stability,
        double difficulty,
        @JsonProperty("elapsed_days") int elapsedDays,
        @JsonProperty("scheduled_days") int scheduledDays,
        int reps,
        int lapses,
        CardState state,
        @JsonProperty("last_review_at") ZonedDateTime lastReviewAt,
        @JsonProperty("next_review_at") ZonedDateTime nextReviewAt,
        @JsonProperty("emergency_floor_prompt") String emergencyFloorPrompt,
        @JsonProperty("rubric_json") String rubricJson
) {}
