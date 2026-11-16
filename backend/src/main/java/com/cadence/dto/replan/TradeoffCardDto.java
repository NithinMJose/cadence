package com.cadence.dto.replan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Capacity negotiation card presented to the user when schedule is overloaded.
 */
public record TradeoffCardDto(
        @JsonProperty("card_type")
        String cardType,

        @JsonProperty("title")
        String title,

        @JsonProperty("description")
        String description,

        @JsonProperty("impact_summary")
        String impactSummary,

        @JsonProperty("recovered_minutes")
        int recoveredMinutes
) {}
