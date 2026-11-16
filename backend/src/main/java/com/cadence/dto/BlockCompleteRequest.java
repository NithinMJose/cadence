package com.cadence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;

public record BlockCompleteRequest(
        @Positive
        @JsonProperty("actual_duration_min") int actualDurationMin
) {}
