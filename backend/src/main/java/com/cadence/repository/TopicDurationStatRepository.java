package com.cadence.repository;

import com.cadence.model.TopicDurationStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicDurationStatRepository extends JpaRepository<TopicDurationStatEntity, UUID> {
    Optional<TopicDurationStatEntity> findByUserIdAndTopicTag(UUID userId, String topicTag);
    java.util.List<TopicDurationStatEntity> findByUserId(UUID userId);
}
