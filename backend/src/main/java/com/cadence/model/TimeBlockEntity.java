package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Scheduled learning time block allocated to a specific time slot on laptop or phone.
 */
@Entity
@Table(name = "time_blocks")
public class TimeBlockEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(nullable = false)
    private String title;

    @Column(name = "topic_tag", nullable = false)
    private String topicTag;

    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private ZonedDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlockStatus status = BlockStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType = DeviceType.LAPTOP_DEEP;

    @Column(name = "planned_duration_min", nullable = false)
    private int plannedDurationMin;

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin;

    @Column(name = "is_emergency_floor", nullable = false)
    private boolean isEmergencyFloor = false;

    @Column(name = "warmup_brief_json", columnDefinition = "text")
    private String warmupBriefJson;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public TimeBlockEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTopicTag() { return topicTag; }
    public void setTopicTag(String topicTag) { this.topicTag = topicTag; }

    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }

    public ZonedDateTime getEndTime() { return endTime; }
    public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }

    public BlockStatus getStatus() { return status; }
    public void setStatus(BlockStatus status) { this.status = status; }

    public DeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }

    public int getPlannedDurationMin() { return plannedDurationMin; }
    public void setPlannedDurationMin(int plannedDurationMin) { this.plannedDurationMin = plannedDurationMin; }

    public Integer getActualDurationMin() { return actualDurationMin; }
    public void setActualDurationMin(Integer actualDurationMin) { this.actualDurationMin = actualDurationMin; }

    public boolean isEmergencyFloor() { return isEmergencyFloor; }
    public void setEmergencyFloor(boolean emergencyFloor) { isEmergencyFloor = emergencyFloor; }

    public String getWarmupBriefJson() { return warmupBriefJson; }
    public void setWarmupBriefJson(String warmupBriefJson) { this.warmupBriefJson = warmupBriefJson; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
