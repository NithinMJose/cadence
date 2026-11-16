package com.cadence.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload containing raw git diff text for immediate autopsy.
 */
public record GitHubRawDiffRequest(
        @NotBlank(message = "Raw diff text cannot be blank.")
        String rawDiff,
        String commitMessage,
        String repoName
) {}
