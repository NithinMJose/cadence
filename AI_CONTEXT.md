# Cadence AI Technical Map & Architecture Context

## Current Phase Status: PHASE 4 COMPLETE (Phone-First MVP 100% Usable)
- **Active Phase:** Phase 4 (GitHub Commit Polling, Diff-to-Flashcard Pipeline & Feynman Voice Grader)
- **Next Phase:** Phase 5 (Next.js 15 Web Control Center, Telemetry Dashboard & Recruiter Sandbox)
- **Android Target:** Android 16 (API 36), compileSdk 36, minSdk 26. Jetpack Compose Material 3, Room SQLite, WorkManager, AlarmManager, Retrofit.
- **Autonomous Offline Architecture:** 100% offline schedule execution via `CachedWeekSnapshotEntity` + exact alarms (`AlarmManager.setExactAndAllowWhileIdle`) + `BootCompletedReceiver` + offline outbox queue (`PendingCommandEntity`).
- **On-Device Fast-Path Router:** Executes `done`, `snooze 15m/30m`, and `emergency floor` directly in local Room SQLite in <10ms with visual latency telemetry.
- **Commute Flashcards & Feynman Mode:** Swipeable/flippable FSRS-4.5 cards with 4-button rating bar, rubric checkpoints, and Feynman Voice Dojo grading explanations against rubrics with LLM feedback.
- **Commit Diff Autopsy:** Analyzes secret-scrubbed git diffs, synthesizes FSRS cards, attaches 3-bullet Session Warm-Up Briefs to deep work blocks, and generates 3-minute Emergency Floor micro-quizzes.

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
├── ai-service/                         # Python 3.12 FastAPI microservice (Port 8000)
│   ├── app/ (config, main, llm/*, models/*, routers/*, security/*, services/*, solver/*)
│   ├── tests/ (test_date_resolver, test_gpu_smoke, test_health, test_intent_parser, test_scrubber, test_solver, test_validator)
│   ├── Dockerfile, pyproject.toml, requirements.txt, uv.lock
├── backend/                            # Java 21 / Spring Boot 3.4 core backend (Port 8080)
│   ├── src/main/java/com/cadence/ (client/*, config/*, controller/*, dto/*, exception/*, model/*, repository/*, service/*)
│   ├── src/main/resources/ (application.yml, db/migration/V1__init_cadence_schema.sql, V2__add_proposed_blocks_to_proposals.sql)
│   ├── src/test/ (FsrsServiceTest, DurationCalibratorTest, BurnoutGuardTest, SystemControllerTest, ScheduleReplanIntegrationTest, CurriculumAndEndpointIntegrationTest)
│   ├── Dockerfile, mvnw, mvnw.cmd, pom.xml
└── android/                            # Kotlin + Jetpack Compose Mobile App (API 36)
    ├── app/
    │   ├── src/main/java/com/cadence/android/
    │   │   ├── CadenceApplication.kt   # Notification channel & periodic WorkManager sync
    │   │   ├── alarm/ (BootCompletedReceiver, CadenceAlarmManager, ExactAlarmPermissionHelper, NotificationReceiver)
    │   │   ├── data/local/ (CadenceDatabase, dao/*, entity/*)
    │   │   ├── data/remote/ (CadenceApiService, NetworkClient, RemoteDtos)
    │   │   ├── data/repository/ (CadenceRepository)
    │   │   ├── router/ (FastPathRouter, FastPathResult)
    │   │   ├── sync/ (SyncWorker)
    │   │   └── ui/ (MainActivity, screens/TodayScreen, screens/FlashcardsScreen, screens/VoiceReplanScreen, screens/SettingsScreen, theme/*)
    │   ├── src/main/res/ (mipmap/*, values/*, drawable/*)
    │   ├── src/test/ (AlarmTimeCalculationTest, FastPathRouterCommandParsingTest)
    │   └── build.gradle.kts
    ├── gradle/ (libs.versions.toml, wrapper/*)
    ├── build.gradle.kts, gradle.properties, gradlew, gradlew.bat, settings.gradle.kts
```

---

## Active API Endpoints
| Method | Endpoint | Service | Auth | Description |
|---|---|---|---|---|
| `GET` | `/health` | `ai-service:8000` | No | AI service readiness & trace probe |
| `GET` | `/api/v1/ai/gpu-smoke-test` | `ai-service:8000` | No | Ollama generation + `/api/ps` VRAM residency check |
| `POST` | `/api/v1/ai/parse-intent` | `ai-service:8000` | No | Natural language intent -> ConstraintMutationAST |
| `POST` | `/api/v1/solver/solve` | `ai-service:8000` | No | Direct OR-Tools CP-SAT schedule optimization |
| `POST` | `/api/v1/ai/replan` | `ai-service:8000` | No | End-to-end pipeline: intent -> date resolver -> CP-SAT |
| `GET` | `/api/v1/system/health` | `backend:8080` | No | Backend service readiness & timezone check |
| `GET` | `/api/v1/system/auth-check` | `backend:8080` | Yes | Validates API key / Bearer token |
| `POST` | `/api/v1/demo/seed` | `backend:8080` | Yes | Seeds 30-day curriculum & user goals |
| `GET` | `/api/v1/schedule/week` | `backend:8080` | Yes | Confirmed 7-day plan with warm-up briefs & floors |
| `POST` | `/api/v1/blocks/{id}/complete` | `backend:8080` | Yes | Completes block, updates EMA duration & banks flex |
| `GET` | `/api/v1/cards/due` | `backend:8080` | Yes | Fetches due FSRS cards for review |
| `POST` | `/api/v1/cards/{id}/review` | `backend:8080` | Yes | Applies FSRS-4.5 math to grade (1..4) |
| `POST` | `/api/v1/schedule/replan/propose` | `backend:8080` | Yes | Phase 1 replan: returns diff, tradeoff cards, PENDING proposal |
| `POST` | `/api/v1/schedule/replan/{id}/confirm`| `backend:8080` | Yes | Phase 2A replan: commits proposed changes to time_blocks atomically |
| `POST` | `/api/v1/schedule/replan/{id}/reject` | `backend:8080` | Yes | Phase 2B replan: discards proposed changes |
| `POST` | `/api/v1/ai/commit-autopsy` | `ai-service:8000` | No | Git diff autopsy -> flashcards, warmup briefs, micro-quizzes |
| `POST` | `/api/v1/ai/feynman-grade` | `ai-service:8000` | No | Spoken transcript -> FSRS grade (1..4) + rubric feedback |
| `POST` | `/api/v1/cards/{id}/feynman-grade` | `backend:8080` | Yes | Evaluates explanation with AI and updates card FSRS state |
| `POST` | `/api/v1/integrations/github/sync-latest` | `backend:8080` | Yes | Polls GitHub REST API for latest commit and analyzes diff |
| `POST` | `/api/v1/integrations/github/analyze-raw-diff` | `backend:8080` | Yes | Analyzes raw git diff string and persists cards/briefs |
