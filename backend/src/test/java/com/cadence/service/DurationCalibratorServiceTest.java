package com.cadence.service;

import com.cadence.model.TopicDurationStatEntity;
import com.cadence.repository.TopicDurationStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DurationCalibratorServiceTest {

    @Mock
    private TopicDurationStatRepository repository;

    private DurationCalibratorService service;
    private final UUID userId = UUID.randomUUID();
    private final String topic = "dsa_java";

    @BeforeEach
    void setUp() {
        service = new DurationCalibratorService(repository);
    }

    @Test
    @DisplayName("Verify first task completion sets initial ratio directly")
    void testFirstCompletionSetsRatio() {
        when(repository.findByUserIdAndTopicTag(userId, topic)).thenReturn(Optional.empty());

        // Planned 60 min, Actual 90 min -> Ratio 1.5
        double multiplier = service.recordBlockCompletion(userId, topic, 60, 90);

        assertThat(multiplier).isEqualTo(1.5);
        ArgumentCaptor<TopicDurationStatEntity> captor = ArgumentCaptor.forClass(TopicDurationStatEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSampleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify subsequent completion applies EMA alpha=0.3 smoothing")
    void testSubsequentCompletionAppliesEma() {
        TopicDurationStatEntity existing = new TopicDurationStatEntity(UUID.randomUUID(), userId, topic, 1.2, 3);
        when(repository.findByUserIdAndTopicTag(userId, topic)).thenReturn(Optional.of(existing));

        // Planned 50 min, Actual 25 min -> Ratio 0.5
        // EMA: (1 - 0.3) * 1.2 + 0.3 * 0.5 = 0.84 + 0.15 = 0.99
        double multiplier = service.recordBlockCompletion(userId, topic, 50, 25);

        assertThat(multiplier).isCloseTo(0.99, within(0.001));
    }

    @Test
    @DisplayName("Verify duration multiplier clamps to bounds [0.7, 2.5]")
    void testClampingBounds() {
        when(repository.findByUserIdAndTopicTag(userId, topic)).thenReturn(Optional.empty());

        // Massive overrun: Planned 10 min, Actual 100 min -> 10.0 clamped to 2.5
        double maxClamped = service.recordBlockCompletion(userId, topic, 10, 100);
        assertThat(maxClamped).isEqualTo(2.5);

        // Massive underrun: Planned 100 min, Actual 5 min -> 0.05 clamped to 0.7
        double minClamped = service.recordBlockCompletion(userId, topic, 100, 5);
        assertThat(minClamped).isEqualTo(0.7);
    }
}
