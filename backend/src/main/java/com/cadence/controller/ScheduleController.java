package com.cadence.controller;

import com.cadence.config.TraceIdFilter;
import com.cadence.dto.BlockCompleteRequest;
import com.cadence.dto.BlockCompleteResponse;
import com.cadence.dto.TimeBlockDto;
import com.cadence.dto.WeeklyScheduleResponse;
import com.cadence.exception.CadenceException;
import com.cadence.model.BlockStatus;
import com.cadence.model.BurnoutTelemetryEntity;
import com.cadence.model.TimeBlockEntity;
import com.cadence.repository.TimeBlockRepository;
import com.cadence.service.BurnoutGuardService;
import com.cadence.service.CurriculumSeederService;
import com.cadence.service.DurationCalibratorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller for weekly schedule queries and block completion logging.
 */
@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final TimeBlockRepository blockRepository;
    private final DurationCalibratorService durationCalibrator;
    private final BurnoutGuardService burnoutGuard;
    private final ZoneId zoneId;

    public ScheduleController(
            TimeBlockRepository blockRepository,
            DurationCalibratorService durationCalibrator,
            BurnoutGuardService burnoutGuard,
            ZoneId zoneId
    ) {
        this.blockRepository = blockRepository;
        this.durationCalibrator = durationCalibrator;
        this.burnoutGuard = burnoutGuard;
        this.zoneId = zoneId;
    }

    @GetMapping("/schedule/week")
    public ResponseEntity<WeeklyScheduleResponse> getWeeklySchedule(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        UUID userId = CurriculumSeederService.DEMO_USER_ID;

        LocalDate today = LocalDate.now(zoneId);
        ZonedDateTime start = ZonedDateTime.of(today, LocalTime.MIN, zoneId);
        ZonedDateTime end = ZonedDateTime.of(today.plusDays(7), LocalTime.MAX, zoneId);

        List<TimeBlockEntity> blocks = blockRepository.findBlocksInWindow(userId, start, end);
        List<TimeBlockDto> dtos = blocks.stream()
                .map(this::toDto)
                .toList();

        BurnoutTelemetryEntity telemetry = burnoutGuard.getOrCreateTelemetry(userId);

        WeeklyScheduleResponse response = new WeeklyScheduleResponse(
                start,
                end,
                zoneId.getId(),
                dtos.size(),
                dtos,
                telemetry.getFlexCreditsMinutes(),
                telemetry.isRecoveryModeActive(),
                traceId != null ? traceId : "trace-week-schedule"
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/blocks/{blockId}/complete")
    public ResponseEntity<BlockCompleteResponse> completeBlock(
            @PathVariable UUID blockId,
            @Valid @RequestBody BlockCompleteRequest body,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.MDC_KEY);
        TimeBlockEntity block = blockRepository.findById(blockId)
                .orElseThrow(() -> new CadenceException(
                        HttpStatus.NOT_FOUND,
                        "BLOCK_NOT_FOUND",
                        "Time block with ID " + blockId + " was not found.",
                        "Verify block ID using GET /api/v1/schedule/week."
                ));

        block.setStatus(BlockStatus.COMPLETED);
        block.setActualDurationMin(body.actualDurationMin());
        block.setUpdatedAt(ZonedDateTime.now(zoneId));
        blockRepository.save(block);

        // Update Duration Bias Calibrator EMA
        double newEma = durationCalibrator.recordBlockCompletion(
                block.getUserId(),
                block.getTopicTag(),
                block.getPlannedDurationMin(),
                body.actualDurationMin()
        );

        // Update Burnout Guard and Flex-time credits
        BurnoutTelemetryEntity telemetry = burnoutGuard.recordBlockCompletion(
                block.getUserId(),
                block.getPlannedDurationMin(),
                body.actualDurationMin()
        );

        BlockCompleteResponse response = new BlockCompleteResponse(
                block.getId(),
                block.getStatus(),
                block.getPlannedDurationMin(),
                body.actualDurationMin(),
                Math.round(newEma * 100.0) / 100.0,
                telemetry.getFlexCreditsMinutes(),
                telemetry.isRecoveryModeActive(),
                traceId != null ? traceId : "trace-block-complete"
        );
        return ResponseEntity.ok(response);
    }

    private TimeBlockDto toDto(TimeBlockEntity b) {
        return new TimeBlockDto(
                b.getId(),
                b.getCardId(),
                b.getTitle(),
                b.getTopicTag(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus(),
                b.getDeviceType(),
                b.getPlannedDurationMin(),
                b.getActualDurationMin(),
                b.isEmergencyFloor(),
                b.getWarmupBriefJson()
        );
    }
}
