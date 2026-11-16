package com.cadence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

/**
 * Health telemetry response returned by /api/v1/system/health.
 */
public record SystemHealthResponse(
        String status,
        String service,
        String version,
        ZonedDateTime timestamp,
        String timezone,
        @JsonProperty("trace_id") String traceId
) {}
