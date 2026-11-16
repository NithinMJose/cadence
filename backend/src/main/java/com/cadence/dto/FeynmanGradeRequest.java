package com.cadence.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload containing user's spoken explanation transcript for Feynman grading.
 */
public record FeynmanGradeRequest(
        @NotBlank(message = "Spoken explanation transcript cannot be blank.")
        String transcript
) {}
