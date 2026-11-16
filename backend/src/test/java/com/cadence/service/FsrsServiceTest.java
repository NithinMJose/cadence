package com.cadence.service;

import com.cadence.model.CardState;
import com.cadence.model.Rating;
import com.cadence.model.TopicCardEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FsrsServiceTest {

    private FsrsService fsrs;

    @BeforeEach
    void setUp() {
        fsrs = new FsrsService();
    }

    @Test
    @DisplayName("Verify FSRS-4.5 initial stability test vectors from open-spaced-repetition reference")
    void testInitialStability() {
        assertThat(fsrs.initStability(1)).isEqualTo(0.4);
        assertThat(fsrs.initStability(2)).isEqualTo(0.6);
        assertThat(fsrs.initStability(3)).isEqualTo(2.4);
        assertThat(fsrs.initStability(4)).isEqualTo(5.8);
    }

    @Test
    @DisplayName("Verify FSRS-4.5 initial difficulty test vectors")
    void testInitialDifficulty() {
        // D0(r) = clamp(4.93 - 0.94 * (r - 3), 1, 10)
        assertThat(fsrs.initDifficulty(3)).isEqualTo(4.93);
        assertThat(fsrs.initDifficulty(4)).isCloseTo(3.99, within(0.001));
        assertThat(fsrs.initDifficulty(2)).isCloseTo(5.87, within(0.001));
        assertThat(fsrs.initDifficulty(1)).isCloseTo(6.81, within(0.001));
    }

    @Test
    @DisplayName("Verify forgetting curve calculation: R(0, S) == 1.0 and R(S, S) == 0.9")
    void testForgettingCurve() {
        double stability = 2.4;
        assertThat(fsrs.forgettingCurve(0, stability)).isEqualTo(1.0);
        assertThat(fsrs.forgettingCurve((int) Math.round(stability), stability)).isCloseTo(0.9, within(0.02));
    }

    @Test
    @DisplayName("Verify New card review transitions matching reference implementation")
    void testNewCardTransitions() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 16, 12, 0, 0, 0, ZoneOffset.UTC);

        // 1. Rating GOOD on New card
        TopicCardEntity cardGood = new TopicCardEntity();
        cardGood.setId(UUID.randomUUID());
        cardGood.setState(CardState.NEW);

        fsrs.applyReview(cardGood, Rating.GOOD, now);
        assertThat(cardGood.getStability()).isEqualTo(2.4);
        assertThat(cardGood.getDifficulty()).isEqualTo(4.93);
        assertThat(cardGood.getState()).isEqualTo(CardState.LEARNING);
        assertThat(cardGood.getReps()).isEqualTo(1);
        assertThat(cardGood.getScheduledDays()).isEqualTo(2);

        // 2. Rating EASY on New card
        TopicCardEntity cardEasy = new TopicCardEntity();
        cardEasy.setId(UUID.randomUUID());
        cardEasy.setState(CardState.NEW);

        fsrs.applyReview(cardEasy, Rating.EASY, now);
        assertThat(cardEasy.getStability()).isEqualTo(5.8);
        assertThat(cardEasy.getDifficulty()).isCloseTo(3.99, within(0.001));
        assertThat(cardEasy.getState()).isEqualTo(CardState.REVIEW);
        assertThat(cardEasy.getScheduledDays()).isEqualTo(6);
    }

    @Test
    @DisplayName("Verify Review card lapse and recall transitions")
    void testReviewCardTransitions() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 16, 12, 0, 0, 0, ZoneOffset.UTC);

        TopicCardEntity card = new TopicCardEntity();
        card.setId(UUID.randomUUID());
        card.setState(CardState.REVIEW);
        card.setStability(2.4);
        card.setDifficulty(4.93);
        card.setLastReviewAt(now.minusDays(3));

        // Rating GOOD increases stability and keeps in REVIEW
        fsrs.applyReview(card, Rating.GOOD, now);
        assertThat(card.getState()).isEqualTo(CardState.REVIEW);
        assertThat(card.getStability()).isGreaterThan(2.4);
        assertThat(card.getScheduledDays()).isGreaterThanOrEqualTo(2);

        // Rating AGAIN causes lapse, increments lapses, and transitions to RELEARNING
        fsrs.applyReview(card, Rating.AGAIN, now.plusDays(7));
        assertThat(card.getState()).isEqualTo(CardState.RELEARNING);
        assertThat(card.getLapses()).isEqualTo(1);
        assertThat(card.getScheduledDays()).isEqualTo(0);
    }
}
