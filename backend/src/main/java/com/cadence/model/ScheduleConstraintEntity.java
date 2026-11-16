package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Schedule constraint defining blocked time windows or protected leisure.
 */
@Entity
@Table(name = "schedule_constraints")
public class ScheduleConstraintEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "constraint_type", nullable = false)
    private String constraintType;

    @Column(name = "window_start", nullable = false)
    private String windowStart;

    @Column(name = "window_end", nullable = false)
    private String windowEnd;

    @Column(name = "days_of_week_json", columnDefinition = "text")
    private String daysOfWeekJson;

    @Column(nullable = false)
    private String reason;

    @Column(name = "is_hard_block", nullable = false)
    private boolean hardBlock = true;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    public ScheduleConstraintEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getConstraintType() { return constraintType; }
    public void setConstraintType(String constraintType) { this.constraintType = constraintType; }

    public String getWindowStart() { return windowStart; }
    public void setWindowStart(String windowStart) { this.windowStart = windowStart; }

    public String getWindowEnd() { return windowEnd; }
    public void setWindowEnd(String windowEnd) { this.windowEnd = windowEnd; }

    public String getDaysOfWeekJson() { return daysOfWeekJson; }
    public void setDaysOfWeekJson(String daysOfWeekJson) { this.daysOfWeekJson = daysOfWeekJson; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isHardBlock() { return hardBlock; }
    public void setHardBlock(boolean hardBlock) { this.hardBlock = hardBlock; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
