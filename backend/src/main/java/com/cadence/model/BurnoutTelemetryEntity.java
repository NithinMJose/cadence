package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Tracks burnout telemetry, rolling completion rates, and banked flex-time credits.
 */
@Entity
@Table(name = "burnout_telemetry", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
public class BurnoutTelemetryEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "rolling_completion_rate", nullable = false)
    private double rollingCompletionRate = 1.0;

    @Column(name = "flex_credits_minutes", nullable = false)
    private int flexCreditsMinutes = 0;

    @Column(name = "recovery_mode_active", nullable = false)
    private boolean recoveryModeActive = false;

    @Column(name = "consecutive_skipped_days", nullable = false)
    private int consecutiveSkippedDays = 0;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public BurnoutTelemetryEntity() {}

    public BurnoutTelemetryEntity(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.rollingCompletionRate = 1.0;
        this.flexCreditsMinutes = 0;
        this.recoveryModeActive = false;
        this.consecutiveSkippedDays = 0;
        this.updatedAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public double getRollingCompletionRate() { return rollingCompletionRate; }
    public void setRollingCompletionRate(double rollingCompletionRate) {
        this.rollingCompletionRate = rollingCompletionRate;
    }

    public int getFlexCreditsMinutes() { return flexCreditsMinutes; }
    public void setFlexCreditsMinutes(int flexCreditsMinutes) { this.flexCreditsMinutes = flexCreditsMinutes; }

    public boolean isRecoveryModeActive() { return recoveryModeActive; }
    public void setRecoveryModeActive(boolean recoveryModeActive) { this.recoveryModeActive = recoveryModeActive; }

    public int getConsecutiveSkippedDays() { return consecutiveSkippedDays; }
    public void setConsecutiveSkippedDays(int consecutiveSkippedDays) {
        this.consecutiveSkippedDays = consecutiveSkippedDays;
    }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
