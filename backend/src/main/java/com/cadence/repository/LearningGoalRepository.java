package com.cadence.repository;

import com.cadence.model.LearningGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningGoalRepository extends JpaRepository<LearningGoalEntity, UUID> {
    List<LearningGoalEntity> findByUserId(UUID userId);
}
