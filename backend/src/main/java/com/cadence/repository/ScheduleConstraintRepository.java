package com.cadence.repository;

import com.cadence.model.ScheduleConstraintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleConstraintRepository extends JpaRepository<ScheduleConstraintEntity, UUID> {
    List<ScheduleConstraintEntity> findByUserId(UUID userId);
}
