package com.cadence.service;

import com.cadence.model.TopicDurationStatEntity;
import com.cadence.repository.TopicDurationStatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Duration Bias Calibrator (Java EMA Multiplier).
 * Updates historical task estimate multipliers per topic tag using an Exponential Moving Average (alpha = 0.3).
 */
@Service
public class DurationCalibratorService {

    public static final double ALPHA = 0.3;
    public static final double MIN_MULTIPLIER = 0.7;
    public static final double MAX_MULTIPLIER = 2.5;

    private final TopicDurationStatRepository durationStatRepository;

    public DurationCalibratorService(TopicDurationStatRepository durationStatRepository) {
        this.durationStatRepository = durationStatRepository;
    }

    @Transactional
    public double recordBlockCompletion(UUID userId, String topicTag, int plannedMin, int actualMin) {
        if (plannedMin <= 0 || actualMin <= 0) {
            return getMultiplier(userId, topicTag);
        }

        double ratio = (double) actualMin / (double) plannedMin;
        TopicDurationStatEntity stat = durationStatRepository
                .findByUserIdAndTopicTag(userId, topicTag)
                .orElseGet(() -> new TopicDurationStatEntity(UUID.randomUUID(), userId, topicTag, 1.0, 0));

        double updatedMultiplier;
        if (stat.getSampleCount() == 0) {
            updatedMultiplier = ratio;
        } else {
            updatedMultiplier = (1.0 - ALPHA) * stat.getEmaMultiplier() + ALPHA * ratio;
        }

        // Clamp to safe bounds [0.7, 2.5]
        double clamped = Math.min(Math.max(updatedMultiplier, MIN_MULTIPLIER), MAX_MULTIPLIER);
        stat.setEmaMultiplier(clamped);
        stat.setSampleCount(stat.getSampleCount() + 1);
        stat.setUpdatedAt(ZonedDateTime.now());

        durationStatRepository.save(stat);
        return clamped;
    }

    public double getMultiplier(UUID userId, String topicTag) {
        return durationStatRepository
                .findByUserIdAndTopicTag(userId, topicTag)
                .map(TopicDurationStatEntity::getEmaMultiplier)
                .orElse(1.0);
    }
}
