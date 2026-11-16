package com.cadence.service;

import com.cadence.client.AiServiceClient;
import com.cadence.dto.GitHubAutopsyResult;
import com.cadence.exception.CadenceException;
import com.cadence.model.*;
import com.cadence.repository.TimeBlockRepository;
import com.cadence.repository.TopicCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Polls GitHub commits via REST API, triggers AI diff autopsy, and schedules generated cards.
 */
@Service
public class GitHubCommitPollerService {

    private static final Logger log = LoggerFactory.getLogger(GitHubCommitPollerService.class);

    private final AiServiceClient aiServiceClient;
    private final TopicCardRepository cardRepository;
    private final TimeBlockRepository blockRepository;
    private final ObjectMapper objectMapper;
    private final ZoneId zoneId;
    private final RestClient githubRestClient;

    @Value("${cadence.github.owner:NithinMJose}")
    private String defaultOwner;

    @Value("${cadence.github.repo:cadence}")
    private String defaultRepo;

    @Value("${cadence.github.token:}")
    private String githubToken;

    public GitHubCommitPollerService(
            AiServiceClient aiServiceClient,
            TopicCardRepository cardRepository,
            TimeBlockRepository blockRepository,
            ObjectMapper objectMapper,
            ZoneId zoneId
    ) {
        this.aiServiceClient = aiServiceClient;
        this.cardRepository = cardRepository;
        this.blockRepository = blockRepository;
        this.objectMapper = objectMapper;
        this.zoneId = zoneId;
        this.githubRestClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("User-Agent", "Cadence-Learning-Engine")
                .build();
    }

    /**
     * Polls the latest commit from GitHub REST API and performs diff autopsy.
     */
    @Transactional
    public GitHubAutopsyResult pollLatestCommit(String owner, String repo, String branch, String traceId) {
        String targetOwner = (owner != null && !owner.isBlank()) ? owner : defaultOwner;
        String targetRepo = (repo != null && !repo.isBlank()) ? repo : defaultRepo;

        try {
            var req = githubRestClient.get()
                    .uri("/repos/{owner}/{repo}/commits?per_page=1", targetOwner, targetRepo);
            if (githubToken != null && !githubToken.isBlank()) {
                req.header("Authorization", "Bearer " + githubToken);
            }

            JsonNode commits = req.retrieve().body(JsonNode.class);
            if (commits == null || !commits.isArray() || commits.isEmpty()) {
                throw new CadenceException(
                        HttpStatus.NOT_FOUND, "NO_COMMITS_FOUND",
                        "No commits found for " + targetOwner + "/" + targetRepo,
                        "Verify repository name and permissions."
                );
            }

            JsonNode latest = commits.get(0);
            String sha = latest.get("sha").asText();
            String message = latest.get("commit").get("message").asText();

            var diffReq = githubRestClient.get()
                    .uri("/repos/{owner}/{repo}/commits/{sha}", targetOwner, targetRepo, sha)
                    .accept(MediaType.parseMediaType("application/vnd.github.v3.diff"));
            if (githubToken != null && !githubToken.isBlank()) {
                diffReq.header("Authorization", "Bearer " + githubToken);
            }

            String diffText = diffReq.retrieve().body(String.class);
            return processRawDiff(diffText != null ? diffText : "", message, targetRepo, sha, traceId);
        } catch (CadenceException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("Failed to poll GitHub commit: {}", ex.getMessage());
            throw new CadenceException(
                    HttpStatus.BAD_GATEWAY, "GITHUB_API_ERROR",
                    "GitHub API request failed: " + ex.getMessage(),
                    "Check network connectivity or use POST /api/v1/integrations/github/analyze-raw-diff."
            );
        }
    }

    /**
     * Performs secret-scrubbed autopsy on raw diff text and persists new cards and warmup briefs.
     */
    @Transactional
    public GitHubAutopsyResult processRawDiff(
            String rawDiff, String message, String repo, String commitHash, String traceId
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("raw_diff", rawDiff);
        payload.put("commit_message", message);
        payload.put("repo_name", repo);

        JsonNode autopsy = aiServiceClient.callCommitAutopsy(payload, traceId);

        List<String> warmupBrief = new ArrayList<>();
        if (autopsy.has("session_warmup_brief")) {
            autopsy.get("session_warmup_brief").forEach(b -> warmupBrief.add(b.asText()));
        }

        String microquiz = autopsy.has("emergency_floor_microquiz")
                ? autopsy.get("emergency_floor_microquiz").asText() : "";

        List<UUID> savedCardIds = new ArrayList<>();
        UUID userId = CurriculumSeederService.DEMO_USER_ID;
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        if (autopsy.has("flashcards") && autopsy.get("flashcards").isArray()) {
            for (JsonNode cardNode : autopsy.get("flashcards")) {
                TopicCardEntity card = new TopicCardEntity();
                card.setId(UUID.randomUUID());
                card.setUserId(userId);
                card.setTitle(cardNode.path("title").asText("Code Diff Concept"));
                card.setTopicTag(cardNode.path("topic_tag").asText("engineering"));
                card.setConceptSummary(cardNode.path("summary").asText("Key architectural invariant"));
                card.setEmergencyFloorPrompt(cardNode.path("emergency_floor_prompt").asText(microquiz));
                card.setRubricJson(cardNode.path("rubric_checkpoints").toString());
                card.setState(CardState.NEW);
                card.setLastReviewAt(null);
                card.setNextReviewAt(now);
                card.setCreatedAt(now);
                card.setUpdatedAt(now);
                cardRepository.save(card);
                savedCardIds.add(card.getId());
            }
        }

        // Attach warm-up brief to next scheduled laptop or study block
        List<TimeBlockEntity> userBlocks = blockRepository.findByUserId(userId);
        userBlocks.stream()
                .filter(b -> b.getStatus() == BlockStatus.SCHEDULED && b.getStartTime().isAfter(now))
                .min(Comparator.comparing(TimeBlockEntity::getStartTime))
                .ifPresent(nextBlock -> {
                    try {
                        nextBlock.setWarmupBriefJson(objectMapper.writeValueAsString(warmupBrief));
                        if (!savedCardIds.isEmpty() && nextBlock.getCardId() == null) {
                            nextBlock.setCardId(savedCardIds.get(0));
                        }
                        blockRepository.save(nextBlock);
                    } catch (Exception e) {
                        log.warn("Failed to serialize warmup brief: {}", e.getMessage());
                    }
                });

        return new GitHubAutopsyResult(
                commitHash != null ? commitHash : UUID.randomUUID().toString().substring(0, 7),
                message != null ? message : "Diff autopsy",
                savedCardIds.size(),
                savedCardIds,
                warmupBrief,
                microquiz,
                traceId
        );
    }
}
