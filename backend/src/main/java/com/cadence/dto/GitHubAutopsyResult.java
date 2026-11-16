package com.cadence.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result of GitHub commit diff analysis including generated flashcards and warmup briefs.
 */
public record GitHubAutopsyResult(
        String commitHash,
        String commitMessage,
        int cardsGenerated,
        List<UUID> cardIds,
        List<String> warmupBrief,
        String emergencyFloorQuiz,
        String traceId
) {}
