package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Tracks historical duration bias per topic tag using an Exponential Moving Average (EMA).
 */
@Entity
@Table(name = "topic_duration_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "topic_tag"})
})
public class TopicDurationStatEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "topic_tag", nullable = false)
    private String topicTag;

    @Column(name = "ema_multiplier", nullable = false)
    private double emaMultiplier = 1.0;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount = 0;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public TopicDurationStatEntity() {}

    public TopicDurationStatEntity(UUID id, UUID userId, String topicTag, double emaMultiplier, int sampleCount) {
        this.id = id;
        this.userId = userId;
        this.topicTag = topicTag;
        this.emaMultiplier = emaMultiplier;
        this.sampleCount = sampleCount;
        this.updatedAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTopicTag() { return topicTag; }
    public void setTopicTag(String topicTag) { this.topicTag = topicTag; }

    public double getEmaMultiplier() { return emaMultiplier; }
    public void setEmaMultiplier(double emaMultiplier) { this.emaMultiplier = emaMultiplier; }

    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
