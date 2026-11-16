package com.cadence.service;

import com.cadence.model.BurnoutTelemetryEntity;
import com.cadence.repository.BurnoutTelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Burnout Guard & Flex-Time Banking Service.
 * Tracks 7-day rolling completion ratio, triggers recovery mode on consecutive skips,
 * and banks flex-time credits when deep-work tasks finish early.
 */
@Service
public class BurnoutGuardService {

    private final BurnoutTelemetryRepository burnoutRepository;

    public BurnoutGuardService(BurnoutTelemetryRepository burnoutRepository) {
        this.burnoutRepository = burnoutRepository;
    }

    @Transactional
    public BurnoutTelemetryEntity getOrCreateTelemetry(UUID userId) {
        return burnoutRepository.findByUserId(userId)
                .orElseGet(() -> burnoutRepository.save(new BurnoutTelemetryEntity(UUID.randomUUID(), userId)));
    }

    @Transactional
    public BurnoutTelemetryEntity recordBlockCompletion(UUID userId, int plannedMin, int actualMin) {
        BurnoutTelemetryEntity telemetry = getOrCreateTelemetry(userId);

        // Bank flex credits if completed early
        if (actualMin > 0 && actualMin < plannedMin) {
            int saved = plannedMin - actualMin;
            telemetry.setFlexCreditsMinutes(telemetry.getFlexCreditsMinutes() + saved);
        }

        // Reset consecutive skipped days and improve rolling rate
        telemetry.setConsecutiveSkippedDays(0);
        double updatedRate = 0.85 * telemetry.getRollingCompletionRate() + 0.15 * 1.0;
        telemetry.setRollingCompletionRate(Math.min(1.0, updatedRate));

        if (telemetry.isRecoveryModeActive() && updatedRate >= 0.80) {
            telemetry.setRecoveryModeActive(false);
        }

        telemetry.setUpdatedAt(ZonedDateTime.now());
        return burnoutRepository.save(telemetry);
    }

    @Transactional
    public BurnoutTelemetryEntity recordBlockSkipped(UUID userId) {
        BurnoutTelemetryEntity telemetry = getOrCreateTelemetry(userId);
        telemetry.setConsecutiveSkippedDays(telemetry.getConsecutiveSkippedDays() + 1);

        double updatedRate = 0.85 * telemetry.getRollingCompletionRate() + 0.15 * 0.0;
        telemetry.setRollingCompletionRate(Math.max(0.0, updatedRate));

        // Activate recovery mode after 2+ consecutive skips
        if (telemetry.getConsecutiveSkippedDays() >= 2) {
            telemetry.setRecoveryModeActive(true);
        }

        telemetry.setUpdatedAt(ZonedDateTime.now());
        return burnoutRepository.save(telemetry);
    }
}
