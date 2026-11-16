package com.cadence.service;

import com.cadence.model.*;
import com.cadence.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a 30-day curriculum with Applied AI Engineering, DSA, and Spring/Kotlin craft.
 */
@Service
public class CurriculumSeederService {

    public static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UserRepository userRepository;
    private final LearningGoalRepository goalRepository;
    private final TopicCardRepository cardRepository;
    private final TimeBlockRepository blockRepository;
    private final ScheduleConstraintRepository constraintRepository;
    private final TopicDurationStatRepository durationStatRepository;
    private final BurnoutTelemetryRepository burnoutRepository;
    private final ZoneId zoneId;

    public CurriculumSeederService(
            UserRepository userRepository,
            LearningGoalRepository goalRepository,
            TopicCardRepository cardRepository,
            TimeBlockRepository blockRepository,
            ScheduleConstraintRepository constraintRepository,
            TopicDurationStatRepository durationStatRepository,
            BurnoutTelemetryRepository burnoutRepository,
            ZoneId zoneId
    ) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.cardRepository = cardRepository;
        this.blockRepository = blockRepository;
        this.constraintRepository = constraintRepository;
        this.durationStatRepository = durationStatRepository;
        this.burnoutRepository = burnoutRepository;
        this.zoneId = zoneId;
    }

    @Transactional
    public void seedCurriculum() {
        // 1. Create or update Demo User
        UserEntity user = userRepository.findById(DEMO_USER_ID).orElseGet(() -> {
            UserEntity u = new UserEntity(DEMO_USER_ID, "developer@cadence.internal", zoneId.getId());
            u.setChronotypePeakHoursJson("[\"08:00-10:00\", \"19:00-21:30\"]");
            u.setProtectedLeisureJson("[\"13:00-14:00\", \"22:30-23:59\"]");
            return userRepository.save(u);
        });

        // 2. Clear existing demo data for clean idempotent seeding
        blockRepository.deleteAll(blockRepository.findByUserId(DEMO_USER_ID));
        cardRepository.deleteAll(cardRepository.findByUserId(DEMO_USER_ID));
        goalRepository.deleteAll(goalRepository.findByUserId(DEMO_USER_ID));
        constraintRepository.deleteAll(constraintRepository.findByUserId(DEMO_USER_ID));

        // 3. Seed Learning Goals
        LearningGoalEntity goalAi = goalRepository.save(new LearningGoalEntity(
                UUID.randomUUID(), DEMO_USER_ID, "Applied AI Engineering",
                "Master local LLM serving, OR-Tools CP-SAT constraint solvers, and prompt compilation."
        ));
        LearningGoalEntity goalDsa = goalRepository.save(new LearningGoalEntity(
                UUID.randomUUID(), DEMO_USER_ID, "Pattern-Based DSA (Java)",
                "Master monotonic stacks, sliding window patterns, and graph traversals in Java."
        ));
        LearningGoalEntity goalSpring = goalRepository.save(new LearningGoalEntity(
                UUID.randomUUID(), DEMO_USER_ID, "Spring Boot & Kotlin Craft",
                "Production-grade distributed systems, stateless token filters, and reactive pipelines."
        ));

        // 4. Seed Topic Cards
        TopicCardEntity cardKv = createCard(goalAi.getId(), "Transformer Attention & KV Caching",
                "Understanding Key-Value caching dynamics, memory overhead per token, and PagedAttention in vLLM.",
                "ai_eng", "Recall the formula for FP16 KV cache memory size given context length, layers, and hidden dim.",
                "{\"checkpoints\": [\"KV pairs stored per layer\", \"FP16 = 2 bytes per element\", \"Linear growth with sequence length\", \"Avoids redundant recomputation\"]}");

        TopicCardEntity cardCpSat = createCard(goalAi.getId(), "OR-Tools CP-SAT Interval Scheduling",
                "Formulating non-overlapping time windows using CpModel.newIntervalVar and AddNoOverlap.",
                "ai_eng", "What are the 3 mandatory parameters when defining an interval variable in OR-Tools?",
                "{\"checkpoints\": [\"Start variable\", \"Size / duration variable\", \"End variable\", \"Enforces start + size == end\"]}");

        TopicCardEntity cardStack = createCard(goalDsa.getId(), "Monotonic Stack for Next Greater Element",
                "Maintaining an invariant of decreasing elements to find the next greater item in O(N).",
                "dsa_java", "Trace 3 numbers [2, 1, 5] through a decreasing monotonic stack and state when pops occur.",
                "{\"checkpoints\": [\"Push indices/values while current <= top\", \"Pop when current > top\", \"Resolved elements get current as next greater\", \"Linear O(N) runtime\"]}");

        TopicCardEntity cardSecurity = createCard(goalSpring.getId(), "Stateless Security Filters & ThreadLocal MDC",
                "OncePerRequestFilter enforcing API keys with RFC 7807 problem details and MDC trace propagation.",
                "spring_craft", "Where must MDC.remove('traceId') be invoked in a servlet filter?",
                "{\"checkpoints\": [\"In the finally block of doFilterInternal\", \"Prevents ThreadLocal leakage to worker pool\", \"Ensures clean logging context for subsequent requests\", \"Propagates X-Trace-Id header in response\"]}");

        // 5. Seed Constraints
        createConstraint("HARD_BLOCK", "10:00", "18:00", "Work / Office Hours", true);
        createConstraint("LEISURE", "13:00", "14:00", "Protected Lunch & Walk", true);
        createConstraint("LEISURE", "22:30", "23:59", "Protected Sleep & Wind-down", true);

        // 6. Seed Current Week Time Blocks
        seedWeeklyBlocks(List.of(cardKv, cardCpSat, cardStack, cardSecurity));

        // 7. Seed Topic Duration Stats & Burnout Telemetry
        upsertDurationStat("ai_eng", 1.15, 6);
        upsertDurationStat("dsa_java", 0.90, 8);
        upsertDurationStat("spring_craft", 1.05, 5);

        BurnoutTelemetryEntity telemetry = burnoutRepository.findByUserId(DEMO_USER_ID)
                .orElseGet(() -> new BurnoutTelemetryEntity(UUID.randomUUID(), DEMO_USER_ID));
        telemetry.setRollingCompletionRate(0.92);
        telemetry.setFlexCreditsMinutes(45);
        telemetry.setRecoveryModeActive(false);
        telemetry.setConsecutiveSkippedDays(0);
        burnoutRepository.save(telemetry);
    }

    private void upsertDurationStat(String topicTag, double multiplier, int samples) {
        TopicDurationStatEntity stat = durationStatRepository.findByUserIdAndTopicTag(DEMO_USER_ID, topicTag)
                .orElseGet(() -> new TopicDurationStatEntity(UUID.randomUUID(), DEMO_USER_ID, topicTag, multiplier, samples));
        stat.setEmaMultiplier(multiplier);
        stat.setSampleCount(samples);
        stat.setUpdatedAt(ZonedDateTime.now());
        durationStatRepository.save(stat);
    }

    private TopicCardEntity createCard(UUID goalId, String title, String summary, String tag, String floor, String rubric) {
        TopicCardEntity c = new TopicCardEntity();
        c.setId(UUID.randomUUID());
        c.setUserId(DEMO_USER_ID);
        c.setGoalId(goalId);
        c.setTitle(title);
        c.setConceptSummary(summary);
        c.setTopicTag(tag);
        c.setStability(2.4);
        c.setDifficulty(4.93);
        c.setElapsedDays(1);
        c.setScheduledDays(2);
        c.setReps(1);
        c.setLapses(0);
        c.setState(CardState.REVIEW);
        c.setLastReviewAt(ZonedDateTime.now(zoneId).minusDays(1));
        c.setNextReviewAt(ZonedDateTime.now(zoneId).minusHours(2)); // Due now
        c.setEmergencyFloorPrompt(floor);
        c.setRubricJson(rubric);
        c.setCreatedAt(ZonedDateTime.now(zoneId));
        c.setUpdatedAt(ZonedDateTime.now(zoneId));
        return cardRepository.save(c);
    }

    private void createConstraint(String type, String start, String end, String reason, boolean hard) {
        ScheduleConstraintEntity sc = new ScheduleConstraintEntity();
        sc.setId(UUID.randomUUID());
        sc.setUserId(DEMO_USER_ID);
        sc.setConstraintType(type);
        sc.setWindowStart(start);
        sc.setWindowEnd(end);
        sc.setDaysOfWeekJson("[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\"]");
        sc.setReason(reason);
        sc.setHardBlock(hard);
        sc.setCreatedAt(ZonedDateTime.now(zoneId));
        constraintRepository.save(sc);
    }

    private void seedWeeklyBlocks(List<TopicCardEntity> cards) {
        LocalDate today = LocalDate.now(zoneId);
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            TopicCardEntity card = cards.get(i % cards.size());

            // Commute Block (Phone, 15 min)
            TimeBlockEntity commute = new TimeBlockEntity();
            commute.setId(UUID.randomUUID());
            commute.setUserId(DEMO_USER_ID);
            commute.setCardId(card.getId());
            commute.setTitle("Commute Flashcard Sprint: " + card.getTitle());
            commute.setTopicTag(card.getTopicTag());
            commute.setStartTime(ZonedDateTime.of(date, LocalTime.of(8, 45), zoneId));
            commute.setEndTime(ZonedDateTime.of(date, LocalTime.of(9, 0), zoneId));
            commute.setStatus(i == 0 ? BlockStatus.COMPLETED : BlockStatus.SCHEDULED);
            commute.setDeviceType(DeviceType.PHONE_COMMUTE);
            commute.setPlannedDurationMin(15);
            commute.setActualDurationMin(i == 0 ? 12 : null);
            commute.setEmergencyFloor(false);
            commute.setWarmupBriefJson("[\"Review previous lapse concepts\", \"Focus on core definitions\", \"Test self before checking answers\"]");
            commute.setCreatedAt(ZonedDateTime.now(zoneId));
            commute.setUpdatedAt(ZonedDateTime.now(zoneId));
            blockRepository.save(commute);

            // Evening Deep Block (Laptop, 60 min)
            TimeBlockEntity deep = new TimeBlockEntity();
            deep.setId(UUID.randomUUID());
            deep.setUserId(DEMO_USER_ID);
            deep.setCardId(card.getId());
            deep.setTitle("Deep Practice: " + card.getTitle());
            deep.setTopicTag(card.getTopicTag());
            deep.setStartTime(ZonedDateTime.of(date, LocalTime.of(19, 30), zoneId));
            deep.setEndTime(ZonedDateTime.of(date, LocalTime.of(20, 30), zoneId));
            deep.setStatus(BlockStatus.SCHEDULED);
            deep.setDeviceType(DeviceType.LAPTOP_DEEP);
            deep.setPlannedDurationMin(60);
            deep.setEmergencyFloor(false);
            deep.setWarmupBriefJson("[\"1. Key concept: " + card.getTitle() + "\", \"2. Focus: Implementation & Edge cases\", \"3. Floor available if energy drops: 3-min quiz\"]");
            deep.setCreatedAt(ZonedDateTime.now(zoneId));
            deep.setUpdatedAt(ZonedDateTime.now(zoneId));
            blockRepository.save(deep);
        }
    }
}
