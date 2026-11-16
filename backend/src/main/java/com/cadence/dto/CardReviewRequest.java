package com.cadence.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CardReviewRequest(
        @Min(1) @Max(4) int grade
) {}
