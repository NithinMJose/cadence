package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Schedule proposal holding proposed replans for two-phase confirmation.
 */
@Entity
@Table(name = "schedule_proposals")
public class ScheduleProposalEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "diff_json", columnDefinition = "text")
    private String diffJson;

    @Column(name = "tradeoff_cards_json", columnDefinition = "text")
    private String tradeoffCardsJson;

    @Column(name = "schedule_version", nullable = false)
    private int scheduleVersion = 1;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "confirmed_at")
    private ZonedDateTime confirmedAt;

    public ScheduleProposalEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }

    public String getDiffJson() { return diffJson; }
    public void setDiffJson(String diffJson) { this.diffJson = diffJson; }

    public String getTradeoffCardsJson() { return tradeoffCardsJson; }
    public void setTradeoffCardsJson(String tradeoffCardsJson) { this.tradeoffCardsJson = tradeoffCardsJson; }

    public int getScheduleVersion() { return scheduleVersion; }
    public void setScheduleVersion(int scheduleVersion) { this.scheduleVersion = scheduleVersion; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(ZonedDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
