package com.cadence.dto.replan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response after atomic confirmation and application of a proposed replan.
 */
public record ReplanConfirmResponse(
        @JsonProperty("proposal_id")
        UUID proposalId,

        @JsonProperty("status")
        String status,

        @JsonProperty("schedule_version")
        int scheduleVersion,

        @JsonProperty("updated_blocks_count")
        int updatedBlocksCount,

        @JsonProperty("confirmed_at")
        ZonedDateTime confirmedAt
) {}
