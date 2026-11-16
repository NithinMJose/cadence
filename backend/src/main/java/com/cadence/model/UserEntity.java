package com.cadence.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * User entity storing timezone, chronotype peak hours, and protected leisure windows.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    @Column(name = "chronotype_peak_hours_json", columnDefinition = "text")
    private String chronotypePeakHoursJson;

    @Column(name = "protected_leisure_json", columnDefinition = "text")
    private String protectedLeisureJson;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public UserEntity() {}

    public UserEntity(UUID id, String email, String timezone) {
        this.id = id;
        this.email = email;
        this.timezone = timezone;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getChronotypePeakHoursJson() { return chronotypePeakHoursJson; }
    public void setChronotypePeakHoursJson(String chronotypePeakHoursJson) {
        this.chronotypePeakHoursJson = chronotypePeakHoursJson;
    }

    public String getProtectedLeisureJson() { return protectedLeisureJson; }
    public void setProtectedLeisureJson(String protectedLeisureJson) {
        this.protectedLeisureJson = protectedLeisureJson;
    }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
