package com.cadence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

/**
 * Diagnostic response for /api/v1/system/auth-check confirming valid credentials.
 */
public record AuthCheckResponse(
        boolean authenticated,
        String principal,
        @JsonProperty("token_source") String tokenSource,
        ZonedDateTime timestamp,
        @JsonProperty("trace_id") String traceId
) {}
