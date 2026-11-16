package com.cadence.service;

import com.cadence.model.BurnoutTelemetryEntity;
import com.cadence.repository.BurnoutTelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BurnoutGuardServiceTest {

    @Mock
    private BurnoutTelemetryRepository repository;

    private BurnoutGuardService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BurnoutGuardService(repository);
    }

    @Test
    @DisplayName("Verify early completion banks flex credits for saved minutes")
    void testEarlyCompletionBanksFlexCredits() {
        BurnoutTelemetryEntity entity = new BurnoutTelemetryEntity(UUID.randomUUID(), userId);
        entity.setFlexCreditsMinutes(10);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Planned 60 min, completed in 40 min -> 20 min saved
        BurnoutTelemetryEntity result = service.recordBlockCompletion(userId, 60, 40);

        assertThat(result.getFlexCreditsMinutes()).isEqualTo(30);
        assertThat(result.getConsecutiveSkippedDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify 2 consecutive skipped blocks activates BURNOUT_RECOVERY_MODE")
    void testConsecutiveSkipsTriggerRecoveryMode() {
        BurnoutTelemetryEntity entity = new BurnoutTelemetryEntity(UUID.randomUUID(), userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Skip 1
        BurnoutTelemetryEntity afterFirstSkip = service.recordBlockSkipped(userId);
        assertThat(afterFirstSkip.getConsecutiveSkippedDays()).isEqualTo(1);
        assertThat(afterFirstSkip.isRecoveryModeActive()).isFalse();

        // Skip 2
        BurnoutTelemetryEntity afterSecondSkip = service.recordBlockSkipped(userId);
        assertThat(afterSecondSkip.getConsecutiveSkippedDays()).isEqualTo(2);
        assertThat(afterSecondSkip.isRecoveryModeActive()).isTrue();
    }
}
