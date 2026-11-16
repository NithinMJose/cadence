package com.cadence.repository;

import com.cadence.model.TimeBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlockEntity, UUID> {
    List<TimeBlockEntity> findByUserId(UUID userId);

    @Query("SELECT b FROM TimeBlockEntity b WHERE b.userId = :userId AND b.startTime >= :start AND b.endTime <= :end ORDER BY b.startTime ASC")
    List<TimeBlockEntity> findBlocksInWindow(
            @Param("userId") UUID userId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );
}
