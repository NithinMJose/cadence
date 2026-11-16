package com.cadence.dto;

/**
 * Request payload to poll latest commit from a GitHub repository.
 */
public record GitHubSyncRequest(
        String owner,
        String repo,
        String branch
) {}
