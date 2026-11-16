package com.cadence.dto.replan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two-phase proposal response containing schedule diff, tradeoff cards, and flight recorder.
 */
public record ReplanProposalResponse(
        @JsonProperty("proposal_id")
        UUID proposalId,

        @JsonProperty("status")
        String status,

        @JsonProperty("schedule_version")
        int scheduleVersion,

        @JsonProperty("ast_summary")
        String astSummary,

        @JsonProperty("diff_items")
        List<ScheduleDiffItemDto> diffItems,

        @JsonProperty("tradeoff_cards")
        List<TradeoffCardDto> tradeoffCards,

        @JsonProperty("total_scheduled_minutes")
        int totalScheduledMinutes,

        @JsonProperty("total_slack_minutes")
        int totalSlackMinutes,

        @JsonProperty("churn_score")
        int churnScore,

        @JsonProperty("_debug_trace")
        Map<String, Object> debugTrace
) {}
