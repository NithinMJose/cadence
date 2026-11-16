package com.cadence.controller;

import com.cadence.config.TraceIdFilter;
import com.cadence.dto.CardReviewRequest;
import com.cadence.dto.CardReviewResponse;
import com.cadence.dto.TopicCardDto;
import com.cadence.exception.CadenceException;
import com.cadence.model.Rating;
import com.cadence.model.TopicCardEntity;
import com.cadence.repository.TopicCardRepository;
import com.cadence.service.CurriculumSeederService;
import com.cadence.service.FsrsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller for FSRS flashcard reviews and due queue retrieval.
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final TopicCardRepository cardRepository;
    private final FsrsService fsrsService;
    private final ZoneId zoneId;

    public CardController(
            TopicCardRepository cardRepository,
            FsrsService fsrsService,
            ZoneId zoneId
    ) {
        this.cardRepository = cardRepository;
        this.fsrsService = fsrsService;
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
