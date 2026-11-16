package com.cadence.repository;

import com.cadence.model.TopicCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TopicCardRepository extends JpaRepository<TopicCardEntity, UUID> {
    List<TopicCardEntity> findByUserId(UUID userId);

    @Query("SELECT c FROM TopicCardEntity c WHERE c.userId = :userId AND (c.nextReviewAt IS NULL OR c.nextReviewAt <= :cutoff) ORDER BY c.nextReviewAt ASC NULLS FIRST")
    List<TopicCardEntity> findDueCards(@Param("userId") UUID userId, @Param("cutoff") ZonedDateTime cutoff);
}
