package com.cadence.repository;

import com.cadence.model.BurnoutTelemetryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BurnoutTelemetryRepository extends JpaRepository<BurnoutTelemetryEntity, UUID> {
    Optional<BurnoutTelemetryEntity> findByUserId(UUID userId);
}
