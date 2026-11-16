package com.cadence.controller;

import com.cadence.config.TraceIdFilter;
import com.cadence.dto.GitHubAutopsyResult;
import com.cadence.dto.GitHubRawDiffRequest;
import com.cadence.dto.GitHubSyncRequest;
import com.cadence.service.GitHubCommitPollerService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for GitHub commit polling, raw diff autopsy, and webhook ingestion.
 */
@RestController
@RequestMapping("/api/v1/integrations/github")
public class GitHubIntegrationController {

    private final GitHubCommitPollerService pollerService;

    public GitHubIntegrationController(GitHubCommitPollerService pollerService) {
        this.pollerService = pollerService;
    }

    @PostMapping("/sync-latest")
    public ResponseEntity<GitHubAutopsyResult> syncLatestCommit(
            @RequestBody(required = false) GitHubSyncRequest body,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        String owner = body != null ? body.owner() : null;
        String repo = body != null ? body.repo() : null;
        String branch = body != null ? body.branch() : null;

        GitHubAutopsyResult result = pollerService.pollLatestCommit(
                owner, repo, branch, traceId != null ? traceId : "trace-github-sync"
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze-raw-diff")
    public ResponseEntity<GitHubAutopsyResult> analyzeRawDiff(
            @Valid @RequestBody GitHubRawDiffRequest body,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        GitHubAutopsyResult result = pollerService.processRawDiff(
                body.rawDiff(),
                body.commitMessage() != null ? body.commitMessage() : "Manual Diff Autopsy",
                body.repoName() != null ? body.repoName() : "cadence",
                UUID.randomUUID().toString().substring(0, 7),
                traceId != null ? traceId : "trace-raw-diff"
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/webhook")
    public ResponseEntity<GitHubAutopsyResult> handleWebhook(
            @RequestBody JsonNode payload,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        String afterSha = payload.path("after").asText(UUID.randomUUID().toString().substring(0, 7));
        String message = payload.path("head_commit").path("message").asText("Webhook commit");
        String repo = payload.path("repository").path("name").asText("cadence");

        GitHubAutopsyResult result = pollerService.processRawDiff(
                "", message, repo, afterSha, traceId != null ? traceId : "trace-gh-webhook"
        );
        return ResponseEntity.ok(result);
    }
}
