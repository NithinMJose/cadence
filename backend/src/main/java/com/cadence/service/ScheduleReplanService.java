package com.cadence.service;

import com.cadence.client.AiServiceClient;
import com.cadence.dto.replan.*;
import com.cadence.exception.CadenceException;
import com.cadence.model.*;
import com.cadence.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Orchestrates two-phase schedule replanning: propose -> confirm or reject.
 */
@Service
public class ScheduleReplanService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleReplanService.class);

    private final UserRepository userRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final ScheduleConstraintRepository scheduleConstraintRepository;
    private final ScheduleProposalRepository scheduleProposalRepository;
    private final BurnoutTelemetryRepository burnoutTelemetryRepository;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    public ScheduleReplanService(
            UserRepository userRepository,
            TimeBlockRepository timeBlockRepository,
            ScheduleConstraintRepository scheduleConstraintRepository,
            ScheduleProposalRepository scheduleProposalRepository,
            BurnoutTelemetryRepository burnoutTelemetryRepository,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.scheduleConstraintRepository = scheduleConstraintRepository;
        this.scheduleProposalRepository = scheduleProposalRepository;
        this.burnoutTelemetryRepository = burnoutTelemetryRepository;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReplanProposalResponse proposeReplan(UUID userId, ReplanProposeRequest request, String traceId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CadenceException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found", "Seed demo user first."));

        ZoneId userTz = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(userTz);
        ZonedDateTime horizonStart = now.truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime horizonEnd = horizonStart.plusDays(7);

        List<TimeBlockEntity> existingBlocks = timeBlockRepository.findBlocksInWindow(userId, horizonStart, horizonEnd);
        List<ScheduleConstraintEntity> constraints = scheduleConstraintRepository.findByUserId(userId);
        int flexCredits = burnoutTelemetryRepository.findByUserId(userId)
                .map(BurnoutTelemetryEntity::getFlexCreditsMinutes).orElse(0);

        Map<String, Object> payload = buildAiPayload(user, request, existingBlocks, constraints, horizonStart, flexCredits, userTz);
        JsonNode aiResponse = aiServiceClient.callReplanPipeline(payload, traceId);

        UUID proposalId = UUID.randomUUID();
        int nextVersion = scheduleProposalRepository.findFirstByUserIdOrderByScheduleVersionDesc(userId)
                .map(p -> p.getScheduleVersion() + 1).orElse(1);

        JsonNode diffJson = aiResponse.path("diff_items");
        JsonNode tradeoffJson = aiResponse.path("tradeoff_cards");
        JsonNode blocksJson = aiResponse.path("scheduled_blocks");

        ScheduleProposalEntity proposal = new ScheduleProposalEntity();
        proposal.setId(proposalId);
        proposal.setUserId(userId);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setScheduleVersion(nextVersion);
        proposal.setDiffJson(diffJson.toString());
        proposal.setTradeoffCardsJson(tradeoffJson.toString());
        proposal.setProposedBlocksJson(blocksJson.toString());
        proposal.setCreatedAt(ZonedDateTime.now());
        scheduleProposalRepository.save(proposal);

        List<ScheduleDiffItemDto> diffs = parseDiffItems(diffJson);
        List<TradeoffCardDto> tradeoffs = parseTradeoffCards(tradeoffJson);
        Map<String, Object> debugTrace = objectMapper.convertValue(
                aiResponse.path("_debug_trace"), new TypeReference<>() {}
        );

        return new ReplanProposalResponse(
                proposalId,
                ProposalStatus.PENDING.name(),
                nextVersion,
                aiResponse.path("ast_summary").asText("Schedule replanned"),
                diffs,
                tradeoffs,
                aiResponse.path("total_scheduled_minutes").asInt(0),
                aiResponse.path("total_slack_minutes").asInt(0),
                aiResponse.path("churn_score").asInt(0),
                debugTrace
        );
    }

    @Transactional
    public ReplanConfirmResponse confirmReplan(UUID userId, UUID proposalId) {
        ScheduleProposalEntity proposal = scheduleProposalRepository.findById(proposalId)
                .orElseThrow(() -> new CadenceException(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "Proposal " + proposalId + " not found", "Check proposalId."));

        if (!proposal.getUserId().equals(userId)) {
            throw new CadenceException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Unauthorized access to proposal", "Access own proposals only.");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new CadenceException(HttpStatus.CONFLICT, "INVALID_PROPOSAL_STATUS", "Proposal is already " + proposal.getStatus(), "Only PENDING proposals can be confirmed.");
        }

        int updatedCount = 0;
        try {
            JsonNode blocksNode = objectMapper.readTree(proposal.getProposedBlocksJson());
            if (blocksNode.isArray()) {
                for (JsonNode b : blocksNode) {
                    UUID blockId = UUID.fromString(b.path("task_id").asText());
                    TimeBlockEntity block = timeBlockRepository.findById(blockId).orElseGet(() -> {
                        TimeBlockEntity nb = new TimeBlockEntity();
                        nb.setId(blockId);
                        nb.setUserId(userId);
                        nb.setTitle(b.path("title").asText());
                        nb.setTopicTag(b.path("topic_tag").asText());
                        nb.setCreatedAt(ZonedDateTime.now());
                        return nb;
                    });

                    block.setStartTime(ZonedDateTime.parse(b.path("start_time").asText()));
                    block.setEndTime(ZonedDateTime.parse(b.path("end_time").asText()));
                    block.setEmergencyFloor(b.path("is_emergency_floor").asBoolean(false));
                    block.setPlannedDurationMin(b.path("duration_min").asInt(45));
                    block.setStatus(BlockStatus.SCHEDULED);
                    block.setUpdatedAt(ZonedDateTime.now());
                    timeBlockRepository.save(block);
                    updatedCount++;
                }
            }
        } catch (Exception ex) {
            log.error("Failed to apply proposed blocks: {}", ex.getMessage());
            throw new CadenceException(HttpStatus.INTERNAL_SERVER_ERROR, "PROPOSAL_APPLY_ERROR", "Failed to commit proposed schedule: " + ex.getMessage(), "Retry proposal confirmation.");
        }

        proposal.setStatus(ProposalStatus.CONFIRMED);
        proposal.setConfirmedAt(ZonedDateTime.now());
        scheduleProposalRepository.save(proposal);

        return new ReplanConfirmResponse(proposalId, ProposalStatus.CONFIRMED.name(), proposal.getScheduleVersion(), updatedCount, proposal.getConfirmedAt());
    }

    @Transactional
    public ReplanRejectResponse rejectReplan(UUID userId, UUID proposalId) {
        ScheduleProposalEntity proposal = scheduleProposalRepository.findById(proposalId)
                .orElseThrow(() -> new CadenceException(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "Proposal " + proposalId + " not found", "Check proposalId."));

        if (!proposal.getUserId().equals(userId)) {
            throw new CadenceException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Unauthorized access to proposal", "Access own proposals only.");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new CadenceException(HttpStatus.CONFLICT, "INVALID_PROPOSAL_STATUS", "Proposal is already " + proposal.getStatus(), "Only PENDING proposals can be rejected.");
        }

        proposal.setStatus(ProposalStatus.REJECTED);
        scheduleProposalRepository.save(proposal);
        return new ReplanRejectResponse(proposalId, ProposalStatus.REJECTED.name(), "Proposal was rejected and discarded.");
    }

    private Map<String, Object> buildAiPayload(UserEntity user, ReplanProposeRequest req, List<TimeBlockEntity> blocks, List<ScheduleConstraintEntity> constraints, ZonedDateTime horizonStart, int flexCredits, ZoneId userTz) {
        Map<String, Object> p = new HashMap<>();
        p.put("user_message", req.userMessage());
        p.put("constraint_ast", req.constraintAst());
        p.put("timezone", user.getTimezone());
        p.put("horizon_start", horizonStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        p.put("horizon_days", 7);
        p.put("flex_credits_available_min", flexCredits);

        List<Map<String, Object>> taskList = blocks.stream().map(b -> Map.<String, Object>of(
                "id", b.getId().toString(),
                "title", b.getTitle(),
                "topic_tag", b.getTopicTag(),
                "duration_min", b.getPlannedDurationMin(),
                "emergency_floor_duration_min", 3,
                "device_venue", b.getDeviceType().name(),
                "urgency_score", 5.0,
                "cognitive_weight", 3.0,
                "original_start", b.getStartTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "original_end", b.getEndTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )).toList();
        p.put("tasks", taskList);
        p.put("existing_blocked_windows", ConstraintExpansionHelper.expandBlocked(constraints, horizonStart, userTz));
        p.put("existing_leisure_windows", ConstraintExpansionHelper.expandLeisure(constraints, horizonStart, userTz));
        return p;
    }

    private List<ScheduleDiffItemDto> parseDiffItems(JsonNode diffNode) {
        List<ScheduleDiffItemDto> list = new ArrayList<>();
        if (diffNode != null && diffNode.isArray()) {
            for (JsonNode d : diffNode) {
                list.add(new ScheduleDiffItemDto(
                        d.path("task_id").asText(),
                        d.path("title").asText(),
                        d.hasNonNull("old_start") ? ZonedDateTime.parse(d.path("old_start").asText()) : null,
                        d.hasNonNull("old_end") ? ZonedDateTime.parse(d.path("old_end").asText()) : null,
                        d.hasNonNull("new_start") ? ZonedDateTime.parse(d.path("new_start").asText()) : null,
                        d.hasNonNull("new_end") ? ZonedDateTime.parse(d.path("new_end").asText()) : null,
                        d.path("action").asText("UNCHANGED"),
                        d.path("causal_reason").asText("")
                ));
            }
        }
        return list;
    }

    private List<TradeoffCardDto> parseTradeoffCards(JsonNode cardNode) {
        List<TradeoffCardDto> list = new ArrayList<>();
        if (cardNode != null && cardNode.isArray()) {
            for (JsonNode c : cardNode) {
                list.add(new TradeoffCardDto(
                        c.path("card_type").asText(),
                        c.path("title").asText(),
                        c.path("description").asText(),
                        c.path("impact_summary").asText(),
                        c.path("recovered_minutes").asInt(0)
                ));
            }
        }
        return list;
    }
}
