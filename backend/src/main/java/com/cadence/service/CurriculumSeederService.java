package com.cadence.service;

import com.cadence.model.*;
import com.cadence.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

/**
 * Seeds everyday lifestyle & learning schedule: wake up, gym, breakfast, games, work, lunch, upskill, sleep.
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
        userRepository.findById(DEMO_USER_ID).orElseGet(() -> {
            UserEntity u = new UserEntity(DEMO_USER_ID, "developer@cadence.internal", zoneId.getId());
            u.setChronotypePeakHoursJson("[\"09:00-11:00\", \"19:00-21:00\"]");
            u.setProtectedLeisureJson("[\"13:00-14:00\", \"22:30-23:59\"]");
            return userRepository.save(u);
        });

        blockRepository.deleteAll(blockRepository.findByUserId(DEMO_USER_ID));
        cardRepository.deleteAll(cardRepository.findByUserId(DEMO_USER_ID));
        goalRepository.deleteAll(goalRepository.findByUserId(DEMO_USER_ID));
        constraintRepository.deleteAll(constraintRepository.findByUserId(DEMO_USER_ID));

        LearningGoalEntity goal = goalRepository.save(new LearningGoalEntity(
                UUID.randomUUID(), DEMO_USER_ID, "Daily Growth & Engineering Craft", "Balanced daily rhythm combining work, fitness, gaming, and upskilling."
        ));

        TopicCardEntity cardCache = createCard(goal.getId(), "System Design: Distributed Caching",
                "Understanding Redis in-memory caching, TTL expiration, and cache-aside pattern.", "upskill",
                "What is cache-aside pattern and how does it prevent stale data?",
                "{\"checkpoints\": [\"Read cache first\", \"On miss, query database\", \"Write result to cache with TTL\", \"Reduces database latency\"]}");

        TopicCardEntity cardPrompt = createCard(goal.getId(), "AI Basics: Context Window & Tokens",
                "How LLM context windows work, token limits, and prompt optimization.", "upskill",
                "What happens when a prompt exceeds the context window?",
                "{\"checkpoints\": [\"Context window is fixed maximum tokens\", \"Tokens represent ~4 characters\", \"Truncation drops earlier messages\", \"Requires compaction or sliding window\"]}");

        TopicCardEntity cardSolid = createCard(goal.getId(), "Clean Code: SOLID Principles",
                "Applying Single Responsibility, Open-Closed, and Dependency Inversion in daily coding.", "upskill",
                "Explain the Single Responsibility Principle with one clear example.",
                "{\"checkpoints\": [\"One reason to change per class\", \"Separates business logic from I/O\", \"Improves unit testability\", \"Reduces merge conflicts\"]}");

        createConstraint("LEISURE", "13:00", "14:00", "Protected Lunch & Walk", true);
        createConstraint("LEISURE", "22:30", "23:59", "Protected Sleep & Rest", true);

        seedDailyRoutine(List.of(cardCache, cardPrompt, cardSolid));

        upsertDurationStat("upskill", 1.05, 5);
        upsertDurationStat("work", 1.0, 10);

        BurnoutTelemetryEntity telemetry = burnoutRepository.findByUserId(DEMO_USER_ID)
                .orElseGet(() -> new BurnoutTelemetryEntity(UUID.randomUUID(), DEMO_USER_ID));
        telemetry.setRollingCompletionRate(0.95);
        telemetry.setFlexCreditsMinutes(60);
        telemetry.setRecoveryModeActive(false);
        telemetry.setConsecutiveSkippedDays(0);
        burnoutRepository.save(telemetry);
    }

    private void seedDailyRoutine(List<TopicCardEntity> cards) {
        LocalDate today = LocalDate.now(zoneId);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        record Template(String title, String tag, int sh, int sm, int eh, int em, DeviceType dev, boolean isFloor, String brief) {}
        List<Template> templates = List.of(
                new Template("🌅 Wake Up & Morning Routine", "routine", 7, 0, 7, 30, DeviceType.PHONE_COMMUTE, false, "[\"Hydrate with water\", \"Quick stretch\", \"Review today's plan\"]"),
                new Template("🏋️ Gym & Workout", "fitness", 7, 30, 8, 30, DeviceType.PHONE_COMMUTE, false, "[\"Dynamic warm-up\", \"Focus on form\", \"Post-workout cooldown\"]"),
                new Template("🍳 Breakfast & Morning News", "wellness", 8, 30, 9, 15, DeviceType.PHONE_COMMUTE, false, "[\"High-protein breakfast\", \"Check morning headlines\"]"),
                new Template("🎮 Morning Gaming / Free Time", "gaming", 9, 15, 10, 0, DeviceType.LAPTOP_DEEP, false, "[\"Casual game session\", \"Disconnect before focus work\"]"),
                new Template("💼 Work: Core Project Focus", "work", 10, 0, 13, 0, DeviceType.LAPTOP_DEEP, false, "[\"High-priority tickets\", \"Architecture & deep coding\", \"Zero distraction mode\"]"),
                new Template("🥗 Lunch Break & Quick Walk", "wellness", 13, 0, 14, 0, DeviceType.PHONE_COMMUTE, false, "[\"Healthy lunch\", \"Outdoor walk\", \"Step away from screens\"]"),
                new Template("💻 Work: Collaboration & Delivery", "work", 14, 0, 18, 0, DeviceType.LAPTOP_DEEP, false, "[\"Team syncs & PR reviews\", \"Feature implementation\", \"Wrap up daily commitments\"]"),
                new Template("🎮 Evening Gaming & Chill", "gaming", 18, 0, 19, 30, DeviceType.LAPTOP_DEEP, false, "[\"Multiplayer with friends\", \"Unwind from workday\"]"),
                new Template("📚 Upskill: Tech & AI Concepts", "upskill", 19, 30, 20, 30, DeviceType.LAPTOP_DEEP, true, "[\"1. Key concept review\", \"2. Hands-on coding exercise\", \"3. Floor available: 3-min quiz\"]"),
                new Template("🍕 Dinner & Evening Free Time", "leisure", 20, 30, 22, 30, DeviceType.PHONE_COMMUTE, false, "[\"Dinner with family/friends\", \"Casual reading & relaxation\"]"),
                new Template("😴 Sleep & Restorative Rest", "sleep", 22, 30, 23, 59, DeviceType.PHONE_COMMUTE, false, "[\"Phone on silent\", \"Dark room, restorative rest\"]")
        );

        for (int day = 0; day < 7; day++) {
            LocalDate date = today.plusDays(day);
            for (Template t : templates) {
                ZonedDateTime start = ZonedDateTime.of(date, LocalTime.of(t.sh, t.sm), zoneId);
                ZonedDateTime end = ZonedDateTime.of(date, LocalTime.of(t.eh, t.em), zoneId);
                int duration = (int) Duration.between(start, end).toMinutes();

                TimeBlockEntity b = new TimeBlockEntity();
                b.setId(UUID.randomUUID());
                b.setUserId(DEMO_USER_ID);
                b.setTitle(t.title);
                b.setTopicTag(t.tag);
                b.setStartTime(start);
                b.setEndTime(end);
                b.setDeviceType(t.dev);
                b.setPlannedDurationMin(duration);
                b.setEmergencyFloor(false);
                b.setWarmupBriefJson(t.brief);
                b.setCreatedAt(now);
                b.setUpdatedAt(now);

                if ("upskill".equals(t.tag)) {
                    b.setCardId(cards.get(day % cards.size()).getId());
                }

                if (day == 0 && end.isBefore(now)) {
                    b.setStatus(BlockStatus.COMPLETED);
                    b.setActualDurationMin(duration);
                } else {
                    b.setStatus(BlockStatus.SCHEDULED);
                }
                blockRepository.save(b);
            }
        }
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
        c.setNextReviewAt(ZonedDateTime.now(zoneId).minusHours(1));
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
        sc.setDaysOfWeekJson("[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\", \"SATURDAY\", \"SUNDAY\"]");
        sc.setReason(reason);
        sc.setHardBlock(hard);
        sc.setCreatedAt(ZonedDateTime.now(zoneId));
        constraintRepository.save(sc);
    }
}
