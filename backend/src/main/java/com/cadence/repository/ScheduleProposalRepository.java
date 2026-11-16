package com.cadence.repository;

import com.cadence.model.ScheduleProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleProposalRepository extends JpaRepository<ScheduleProposalEntity, UUID> {
    List<ScheduleProposalEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ScheduleProposalEntity> findFirstByUserIdOrderByScheduleVersionDesc(UUID userId);
}
