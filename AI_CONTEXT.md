# Cadence AI Technical Map & Architecture Context

## Current Phase Status: PHASE 1 COMPLETE (Awaiting User Verification)
- **Active Phase:** Phase 1 (Spring Boot System of Record, Auth, Timezone Anchoring & FSRS-4.5 Engine)
- **Next Phase:** Phase 2A (Python AI Intent Compiler & Secret Scrubber)
- **FSRS-4.5 Spec:** 17 parameters ($w_0..w_{16}$), default weights from `open-spaced-repetition/py-fsrs` v2.0.0, power-law forgetting curve $R(t, S) = (1 + F \cdot \frac{t}{S})^C$ with $C = -0.5, F = 0.9^{-2} - 1 \approx 0.234568$. Runs on Java backend only.

---

## Active Monorepo File Tree
```
cadence/
├── .aiignore                           # AI context exclusions (lockfiles, binaries)
├── .cursorignore                       # Cursor/AI agent token context exclusions
├── .env.example                        # Canonical template for environment variables
├── .gitignore                          # Git exclusions (preserves lockfiles)
├── AI_CONTEXT.md                       # Compact technical map (<220 lines)
├── CODING_GUIDELINES.md                # Engineering laws, anti-slop rules, git conventions
├── PHASE_ROADMAP.md                    # Roadmap across Phases 0-6 with checkboxes
├── README.md                           # Live status dashboard & verification guide
├── docker-compose.yml                  # Postgres 16, Ollama (GPU), Ollama-pull, AI, Backend
├── ai-service/                         # Python 3.12 FastAPI microservice
│   ├── app/ (config, main, models, routers, services)
│   ├── tests/ (test_gpu_smoke, test_health)
│   ├── pyproject.toml / requirements.txt / uv.lock
└── backend/                            # Java 21 / Spring Boot 3.4 core backend
    ├── src/main/java/com/cadence/
    │   ├── config/ (ApiKeyAuthFilter, SecurityConfig, TimezoneConfig, TraceIdFilter)
    │   ├── controller/
    │   │   ├── CardController.java             # GET /api/v1/cards/due, POST /api/v1/cards/{id}/review
    │   │   ├── DemoSeederController.java       # POST /api/v1/demo/seed
    │   │   ├── ScheduleController.java         # GET /api/v1/schedule/week, POST /api/v1/blocks/{id}/complete
    │   │   └── SystemController.java           # GET /api/v1/system/health, GET /api/v1/system/auth-check
    │   ├── dto/ (BlockComplete, CardReview, ProblemDetail, WeeklySchedule, TimeBlockDto, TopicCardDto)
    │   ├── exception/ (CadenceException, GlobalExceptionHandler)
    │   ├── model/ (Users, Goals, TopicCards, Constraints, TimeBlocks, Proposals, Stats, Telemetry, Enums)
    │   ├── repository/ (8 JPA repositories for all domain entities)
    │   └── service/
    │       ├── BurnoutGuardService.java        # 7-day rolling rate, flex credits banking, recovery mode
    │       ├── CurriculumSeederService.java    # 30-day curriculum seeder (AI, DSA, Spring/Kotlin)
    │       ├── DurationCalibratorService.java  # EMA duration multiplier (alpha=0.3, [0.7, 2.5])
    │       └── FsrsService.java                # Reference-verified FSRS-4.5 engine (17 parameters)
    ├── src/main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__init_cadence_schema.sql  # 8 PostgreSQL tables with TIMESTAMPTZ
    ├── src/test/ (FsrsServiceTest, DurationCalibratorTest, BurnoutGuardTest, SystemControllerTest, IntegrationTest)
    ├── Dockerfile, mvnw, mvnw.cmd, pom.xml
```

---

## Active API Endpoints (Phase 0 + Phase 1)
| Method | Endpoint | Service | Auth | Description |
|---|---|---|---|---|
| `GET` | `/health` | `ai-service:8000` | No | AI service readiness & trace probe |
| `GET` | `/api/v1/ai/gpu-smoke-test` | `ai-service:8000` | No | Ollama generation + `/api/ps` VRAM residency check |
| `GET` | `/api/v1/system/health` | `backend:8080` | No | Backend service readiness & timezone check |
| `GET` | `/api/v1/system/auth-check` | `backend:8080` | Yes | Validates API key / Bearer token |
| `POST` | `/api/v1/demo/seed` | `backend:8080` | Yes | Seeds 30-day curriculum & user goals |
| `GET` | `/api/v1/schedule/week` | `backend:8080` | Yes | Confirmed 7-day plan with warm-up briefs & floors |
| `POST` | `/api/v1/blocks/{id}/complete` | `backend:8080` | Yes | Completes block, updates EMA duration & banks flex |
| `GET` | `/api/v1/cards/due` | `backend:8080` | Yes | Fetches due FSRS cards for review |
| `POST` | `/api/v1/cards/{id}/review` | `backend:8080` | Yes | Applies FSRS-4.5 math to grade (1..4) |

---

## PostgreSQL 16 Schema Map
- `users`: `id`, `email`, `timezone` (`Asia/Kolkata`), `chronotype_peak_hours_json`, `protected_leisure_json`.
- `learning_goals`: `id`, `user_id`, `title`, `description`, `target_date`.
- `topic_cards`: `id`, `user_id`, `goal_id`, `title`, `concept_summary`, `topic_tag`, `stability`, `difficulty`, `elapsed_days`, `scheduled_days`, `reps`, `lapses`, `state`, `last_review_at`, `next_review_at`, `emergency_floor_prompt`, `rubric_json`.
- `schedule_constraints`: `id`, `user_id`, `constraint_type`, `window_start`, `window_end`, `days_of_week_json`, `reason`, `is_hard_block`.
- `time_blocks`: `id`, `user_id`, `card_id`, `title`, `topic_tag`, `start_time`, `end_time`, `status`, `device_type`, `planned_duration_min`, `actual_duration_min`, `is_emergency_floor`, `warmup_brief_json`.
- `schedule_proposals`: `id`, `user_id`, `status`, `diff_json`, `tradeoff_cards_json`, `schedule_version`.
- `topic_duration_stats`: `id`, `user_id`, `topic_tag`, `ema_multiplier`, `sample_count`.
- `burnout_telemetry`: `id`, `user_id`, `rolling_completion_rate`, `flex_credits_minutes`, `recovery_mode_active`, `consecutive_skipped_days`.
