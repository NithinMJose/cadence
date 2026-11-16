package com.cadence.controller;

import com.cadence.config.TraceIdFilter;
import com.cadence.dto.replan.ReplanConfirmResponse;
import com.cadence.dto.replan.ReplanProposalResponse;
import com.cadence.dto.replan.ReplanProposeRequest;
import com.cadence.dto.replan.ReplanRejectResponse;
import com.cadence.service.CurriculumSeederService;
import com.cadence.service.ScheduleReplanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller handling two-phase schedule replanning (propose -> confirm/reject).
 */
@RestController
@RequestMapping("/api/v1/schedule/replan")
public class ScheduleReplanController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleReplanController.class);

    private final ScheduleReplanService replanService;

    public ScheduleReplanController(ScheduleReplanService replanService) {
        this.replanService = replanService;
    }

    /**
     * Phase 1: Propose replan options based on natural language or symbolic constraint AST.
     */
    @PostMapping("/propose")
    public ResponseEntity<ReplanProposalResponse> proposeReplan(
            @Valid @RequestBody ReplanProposeRequest request,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdFilter.MDC_KEY);
        UUID userId = CurriculumSeederService.DEMO_USER_ID;
        log.info("Proposing schedule replan for userId={} traceId={}", userId, traceId);

        ReplanProposalResponse response = replanService.proposeReplan(userId, request, traceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 2A: Confirm and commit proposed schedule changes into time_blocks atomically.
     */
    @PostMapping("/{proposalId}/confirm")
    public ResponseEntity<ReplanConfirmResponse> confirmReplan(
            @PathVariable UUID proposalId,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdFilter.MDC_KEY);
        UUID userId = CurriculumSeederService.DEMO_USER_ID;
        log.info("Confirming schedule proposalId={} for userId={} traceId={}", proposalId, userId, traceId);

        ReplanConfirmResponse response = replanService.confirmReplan(userId, proposalId);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 2B: Discard and reject proposed schedule changes.
     */
    @PostMapping("/{proposalId}/reject")
    public ResponseEntity<ReplanRejectResponse> rejectReplan(
            @PathVariable UUID proposalId,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdFilter.MDC_KEY);
        UUID userId = CurriculumSeederService.DEMO_USER_ID;
        log.info("Rejecting schedule proposalId={} for userId={} traceId={}", proposalId, userId, traceId);

        ReplanRejectResponse response = replanService.rejectReplan(userId, proposalId);
        return ResponseEntity.ok(response);
    }
}
