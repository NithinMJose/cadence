package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Topic Card entity carrying FSRS-4.5 spaced repetition state and emergency floor prompt.
 */
@Entity
@Table(name = "topic_cards")
public class TopicCardEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "goal_id")
    private UUID goalId;

    @Column(nullable = false)
    private String title;

    @Column(name = "concept_summary", nullable = false, columnDefinition = "text")
    private String conceptSummary;

    @Column(name = "topic_tag", nullable = false)
    private String topicTag;

    @Column(nullable = false)
    private double stability = 0.0;

    @Column(nullable = false)
    private double difficulty = 0.0;

    @Column(name = "elapsed_days", nullable = false)
    private int elapsedDays = 0;

    @Column(name = "scheduled_days", nullable = false)
    private int scheduledDays = 0;

    @Column(nullable = false)
    private int reps = 0;

    @Column(nullable = false)
    private int lapses = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardState state = CardState.NEW;

    @Column(name = "last_review_at")
    private ZonedDateTime lastReviewAt;

    @Column(name = "next_review_at")
    private ZonedDateTime nextReviewAt;

    @Column(name = "emergency_floor_prompt", nullable = false, columnDefinition = "text")
    private String emergencyFloorPrompt;

    @Column(name = "rubric_json", columnDefinition = "text")
    private String rubricJson;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public TopicCardEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getConceptSummary() { return conceptSummary; }
    public void setConceptSummary(String conceptSummary) { this.conceptSummary = conceptSummary; }

    public String getTopicTag() { return topicTag; }
    public void setTopicTag(String topicTag) { this.topicTag = topicTag; }

    public double getStability() { return stability; }
    public void setStability(double stability) { this.stability = stability; }

    public double getDifficulty() { return difficulty; }
    public void setDifficulty(double difficulty) { this.difficulty = difficulty; }

    public int getElapsedDays() { return elapsedDays; }
    public void setElapsedDays(int elapsedDays) { this.elapsedDays = elapsedDays; }

    public int getScheduledDays() { return scheduledDays; }
    public void setScheduledDays(int scheduledDays) { this.scheduledDays = scheduledDays; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public int getLapses() { return lapses; }
    public void setLapses(int lapses) { this.lapses = lapses; }

    public CardState getState() { return state; }
    public void setState(CardState state) { this.state = state; }

    public ZonedDateTime getLastReviewAt() { return lastReviewAt; }
    public void setLastReviewAt(ZonedDateTime lastReviewAt) { this.lastReviewAt = lastReviewAt; }

    public ZonedDateTime getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(ZonedDateTime nextReviewAt) { this.nextReviewAt = nextReviewAt; }

    public String getEmergencyFloorPrompt() { return emergencyFloorPrompt; }
    public void setEmergencyFloorPrompt(String emergencyFloorPrompt) { this.emergencyFloorPrompt = emergencyFloorPrompt; }

    public String getRubricJson() { return rubricJson; }
    public void setRubricJson(String rubricJson) { this.rubricJson = rubricJson; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
