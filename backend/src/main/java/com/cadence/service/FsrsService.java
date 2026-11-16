package com.cadence.service;

import com.cadence.model.CardState;
import com.cadence.model.Rating;
import com.cadence.model.TopicCardEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Reference-verified FSRS-4.5 (Free Spaced Repetition Scheduler) implementation.
 * Ported from official reference open-spaced-repetition/py-fsrs v2.0.0.
 */
@Service
public class FsrsService {

    // Default 17 weights (w0..w16) of FSRS-4.5
    public static final double[] DEFAULT_WEIGHTS = {
            0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01,
            1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
    };

    public static final double DECAY = -0.5;
    public static final double FACTOR = Math.pow(0.9, 1.0 / DECAY) - 1.0; // 0.9^(-2) - 1 = 19/81 ~= 0.2345679

    private final double[] w;
    private final double requestRetention;
    private final int maximumInterval;

    public FsrsService() {
        this(DEFAULT_WEIGHTS, 0.9, 36500);
    }

    public FsrsService(double[] weights, double requestRetention, int maximumInterval) {
        this.w = weights.clone();
        this.requestRetention = requestRetention;
        this.maximumInterval = maximumInterval;
    }

    public TopicCardEntity applyReview(TopicCardEntity card, Rating rating, ZonedDateTime reviewTime) {
        int elapsedDays = 0;
        if (card.getLastReviewAt() != null) {
            elapsedDays = (int) Math.max(0, Duration.between(card.getLastReviewAt(), reviewTime).toDays());
        }
        card.setElapsedDays(elapsedDays);
        card.setReps(card.getReps() + 1);

        if (card.getState() == CardState.NEW) {
            handleNewCard(card, rating);
        } else if (card.getState() == CardState.LEARNING || card.getState() == CardState.RELEARNING) {
            handleLearningCard(card, rating);
        } else {
            handleReviewCard(card, rating, elapsedDays);
        }

        card.setLastReviewAt(reviewTime);
        if (card.getScheduledDays() > 0) {
            card.setNextReviewAt(reviewTime.plusDays(card.getScheduledDays()));
        } else {
            card.setNextReviewAt(reviewTime.plusMinutes(10));
        }
        card.setUpdatedAt(ZonedDateTime.now());
        return card;
    }

    private void handleNewCard(TopicCardEntity card, Rating rating) {
        card.setStability(initStability(rating.getValue()));
        card.setDifficulty(initDifficulty(rating.getValue()));
        if (rating == Rating.EASY) {
            card.setState(CardState.REVIEW);
            card.setScheduledDays(nextInterval(card.getStability()));
        } else {
            card.setState(CardState.LEARNING);
            card.setScheduledDays(rating == Rating.GOOD ? nextInterval(card.getStability()) : 0);
        }
    }

    private void handleLearningCard(TopicCardEntity card, Rating rating) {
        if (rating == Rating.AGAIN) {
            card.setLapses(card.getLapses() + 1);
            card.setScheduledDays(0);
        } else {
            card.setState(CardState.REVIEW);
            card.setScheduledDays(nextInterval(card.getStability()));
        }
    }

    private void handleReviewCard(TopicCardEntity card, Rating rating, int elapsedDays) {
        double retrievability = forgettingCurve(elapsedDays, card.getStability());
        double lastD = card.getDifficulty();
        double lastS = card.getStability();

        card.setDifficulty(nextDifficulty(lastD, rating.getValue()));
        if (rating == Rating.AGAIN) {
            card.setStability(nextForgetStability(lastD, lastS, retrievability));
            card.setLapses(card.getLapses() + 1);
            card.setState(CardState.RELEARNING);
            card.setScheduledDays(0);
        } else {
            card.setStability(nextRecallStability(lastD, lastS, retrievability, rating.getValue()));
            card.setScheduledDays(nextInterval(card.getStability()));
        }
    }

    public double initStability(int rating) {
        return Math.max(w[rating - 1], 0.1);
    }

    public double initDifficulty(int rating) {
        return Math.min(Math.max(w[4] - w[5] * (rating - 3), 1.0), 10.0);
    }

    public double forgettingCurve(int elapsedDays, double stability) {
        if (stability <= 0.0) return 0.0;
        return Math.pow(1.0 + FACTOR * elapsedDays / stability, DECAY);
    }

    public int nextInterval(double stability) {
        double newInterval = (stability / FACTOR) * (Math.pow(requestRetention, 1.0 / DECAY) - 1.0);
        return (int) Math.min(Math.max(Math.round(newInterval), 1), maximumInterval);
    }

    public double nextDifficulty(double d, int rating) {
        double nextD = d - w[6] * (rating - 3);
        double meanReverted = w[7] * w[4] + (1.0 - w[7]) * nextD;
        return Math.min(Math.max(meanReverted, 1.0), 10.0);
    }

    public double nextRecallStability(double d, double s, double r, int rating) {
        double hardPenalty = (rating == Rating.HARD.getValue()) ? w[15] : 1.0;
        double easyBonus = (rating == Rating.EASY.getValue()) ? w[16] : 1.0;
        return s * (1.0 + Math.exp(w[8]) * (11.0 - d) * Math.pow(s, -w[9])
                * (Math.exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus);
    }

    public double nextForgetStability(double d, double s, double r) {
        return w[11] * Math.pow(d, -w[12]) * (Math.pow(s + 1.0, w[13]) - 1.0)
                * Math.exp((1.0 - r) * w[14]);
    }
}
