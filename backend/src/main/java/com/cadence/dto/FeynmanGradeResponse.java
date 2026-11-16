package com.cadence.dto;

import com.cadence.model.CardState;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evaluated Feynman explanation response with rubric coverage and updated FSRS state.
 */
public record FeynmanGradeResponse(
        UUID cardId,
        String cardTitle,
        int fsrsGrade,
        List<String> matchedCheckpoints,
        List<String> missingCheckpoints,
        String feedback,
        double stability,
        double difficulty,
        int scheduledDays,
        int reps,
        int lapses,
        CardState state,
        ZonedDateTime nextReviewAt,
        String traceId
) {}
