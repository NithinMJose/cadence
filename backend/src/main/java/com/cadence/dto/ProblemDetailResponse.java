package com.cadence.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * RFC 7807 compliant problem detail response for client error reporting.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetailResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("remediation_hint") String remediationHint,
        @JsonProperty("invalid_params") Map<String, Object> invalidParams
) {
    public static ProblemDetailResponse of(
            int status,
            String errorCode,
            String detail,
            String instance,
            String traceId,
            String remediationHint
    ) {
        return new ProblemDetailResponse(
                "about:blank",
                errorCode,
                status,
                detail,
                instance,
                errorCode,
                traceId,
                remediationHint,
                null
        );
    }
}
