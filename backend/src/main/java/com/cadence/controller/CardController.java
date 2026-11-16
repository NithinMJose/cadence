package com.cadence.controller;

import com.cadence.client.AiServiceClient;
import com.cadence.config.TraceIdFilter;
import com.cadence.dto.*;
import com.cadence.exception.CadenceException;
import com.cadence.model.Rating;
import com.cadence.model.TopicCardEntity;
import com.cadence.repository.TopicCardRepository;
import com.cadence.service.CurriculumSeederService;
import com.cadence.service.FsrsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Controller for FSRS flashcard reviews, Feynman grading, and due queue retrieval.
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final TopicCardRepository cardRepository;
    private final FsrsService fsrsService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final ZoneId zoneId;

    public CardController(
            TopicCardRepository cardRepository,
            FsrsService fsrsService,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper,
            ZoneId zoneId
    ) {
        this.cardRepository = cardRepository;
        this.fsrsService = fsrsService;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        this.zoneId = zoneId;
    }

    @GetMapping("/due")
    public ResponseEntity<List<TopicCardDto>> getDueCards(HttpServletRequest request) {
        UUID userId = CurriculumSeederService.DEMO_USER_ID;
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        List<TopicCardEntity> dueCards = cardRepository.findDueCards(userId, now);
        List<TopicCardDto> dtos = dueCards.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{cardId}/review")
    public ResponseEntity<CardReviewResponse> reviewCard(
            @PathVariable UUID cardId,
            @Valid @RequestBody CardReviewRequest body,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        TopicCardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CadenceException(
                        HttpStatus.NOT_FOUND,
                        "CARD_NOT_FOUND",
                        "Topic card with ID " + cardId + " was not found.",
                        "Verify card ID using GET /api/v1/cards/due."
                ));

        Rating rating = Rating.fromValue(body.grade());
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        TopicCardEntity updated = fsrsService.applyReview(card, rating, now);
        cardRepository.save(updated);

        CardReviewResponse response = new CardReviewResponse(
                updated.getId(),
                updated.getTitle(),
                body.grade(),
                Math.round(updated.getStability() * 1000.0) / 1000.0,
                Math.round(updated.getDifficulty() * 1000.0) / 1000.0,
                updated.getScheduledDays(),
                updated.getReps(),
                updated.getLapses(),
                updated.getState(),
                updated.getNextReviewAt(),
                traceId != null ? traceId : "trace-card-review"
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cardId}/feynman-grade")
    public ResponseEntity<FeynmanGradeResponse> gradeFeynmanExplanation(
            @PathVariable UUID cardId,
            @Valid @RequestBody FeynmanGradeRequest body,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        TopicCardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CadenceException(
                        HttpStatus.NOT_FOUND, "CARD_NOT_FOUND",
                        "Topic card with ID " + cardId + " was not found.",
                        "Verify card ID using GET /api/v1/cards/due."
                ));

        List<String> checkpoints = parseRubricCheckpoints(card.getRubricJson(), card.getTitle());

        Map<String, Object> aiPayload = new HashMap<>();
        aiPayload.put("card_title", card.getTitle());
        aiPayload.put("topic_tag", card.getTopicTag());
        aiPayload.put("concept_summary", card.getConceptSummary());
        aiPayload.put("rubric_checkpoints", checkpoints);
        aiPayload.put("transcript", body.transcript());

        JsonNode grading = aiServiceClient.callFeynmanGrade(
                aiPayload, traceId != null ? traceId : "trace-feynman-grade"
        );

        int fsrsGrade = grading.path("fsrs_grade").asInt(1);
        List<String> matched = new ArrayList<>();
        grading.path("matched_checkpoints").forEach(cp -> matched.add(cp.asText()));
        List<String> missing = new ArrayList<>();
        grading.path("missing_checkpoints").forEach(cp -> missing.add(cp.asText()));
        String feedback = grading.path("feedback").asText("Keep practicing this concept.");

        Rating rating = Rating.fromValue(fsrsGrade);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        TopicCardEntity updated = fsrsService.applyReview(card, rating, now);
        cardRepository.save(updated);

        return ResponseEntity.ok(new FeynmanGradeResponse(
                updated.getId(),
                updated.getTitle(),
                fsrsGrade,
                matched,
                missing,
                feedback,
                Math.round(updated.getStability() * 1000.0) / 1000.0,
                Math.round(updated.getDifficulty() * 1000.0) / 1000.0,
                updated.getScheduledDays(),
                updated.getReps(),
                updated.getLapses(),
                updated.getState(),
                updated.getNextReviewAt(),
                traceId != null ? traceId : "trace-feynman"
        ));
    }

    private List<String> parseRubricCheckpoints(String rubricJson, String fallbackTitle) {
        List<String> checkpoints = new ArrayList<>();
        if (rubricJson != null && !rubricJson.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(rubricJson);
                if (root.isArray()) {
                    root.forEach(cp -> checkpoints.add(cp.asText()));
                } else if (root.has("checkpoints")) {
                    root.get("checkpoints").forEach(cp -> checkpoints.add(cp.asText()));
                }
            } catch (Exception e) {
                // fall through to default
            }
        }
        if (checkpoints.isEmpty()) {
            checkpoints.add("1. Explain the fundamental mechanism of " + fallbackTitle);
            checkpoints.add("2. State the key architectural invariant");
            checkpoints.add("3. Describe the potential edge case or failure mode");
            checkpoints.add("4. Detail the time or memory complexity implications");
        }
        return checkpoints;
    }

    private TopicCardDto toDto(TopicCardEntity c) {
        return new TopicCardDto(
                c.getId(),
                c.getGoalId(),
                c.getTitle(),
                c.getConceptSummary(),
                c.getTopicTag(),
                c.getStability(),
                c.getDifficulty(),
                c.getElapsedDays(),
                c.getScheduledDays(),
                c.getReps(),
                c.getLapses(),
                c.getState(),
                c.getLastReviewAt(),
                c.getNextReviewAt(),
                c.getEmergencyFloorPrompt(),
                c.getRubricJson()
        );
    }
}
