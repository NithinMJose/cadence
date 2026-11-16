package com.cadence.dto.replan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Response when a proposed replan is discarded.
 */
public record ReplanRejectResponse(
        @JsonProperty("proposal_id")
        UUID proposalId,

        @JsonProperty("status")
        String status,

        @JsonProperty("message")
        String message
) {}
