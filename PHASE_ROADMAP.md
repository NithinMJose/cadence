# Cadence Project Phase Roadmap

- [x] **Phase 0: Monorepo Scaffold, Docker GPU + Auto-Pull Setup & RTX 5060 (Blackwell) Smoke Test**
  - [x] Configure `.gitignore` (keep lockfiles) and `.cursorignore` / `.aiignore` (exclude lockfiles and binaries from AI context)
  - [x] Create `.env.example` and local `.env` with Ollama, Postgres, and Gemini variables
  - [x] Write `docker-compose.yml` with Postgres 16, Ollama GPU passthrough, and `ollama-pull` auto-pull init service
  - [x] Scaffold `/ai-service` (FastAPI 3.12, `GET /health`, `GET /api/v1/ai/gpu-smoke-test` inspecting VRAM)
  - [x] Scaffold `/backend` (Java 21 Spring Boot 3.4, `GET /api/v1/system/health`, `GET /api/v1/system/auth-check` with `ApiKeyAuthFilter`)
  - [x] Establish documentation quartet: `CODING_GUIDELINES.md`, `AI_CONTEXT.md`, `PHASE_ROADMAP.md`, `README.md`
  - [x] Automated tests for `/ai-service` (5/5 passed) and `/backend` (5/5 passed)

- [x] **Phase 1: Spring Boot System of Record, Auth, Timezone Anchoring & FSRS-4.5 Engine**
  - [x] Flyway Migration `V1__init_cadence_schema.sql` (PostgreSQL 16 `TIMESTAMPTZ`, users, cards, constraints, blocks, proposals, stats)
  - [x] `ApiKeyAuthFilter` protecting `/api/v1/**` with `X-Cadence-Api-Key` / Bearer token
  - [x] Reference-verified Java FSRS-4.5 implementation (17 parameters $w_0..w_{16}$, power-law forgetting curve, test vectors from `open-spaced-repetition/py-fsrs` v2.0.0)
  - [x] Duration Bias Calibrator (`alpha = 0.3` EMA multiplier per topic tag clamped to `[0.7, 2.5]`)
  - [x] Burnout Guard (7-day rolling completion rate, recovery mode, flex-time credit banking)
  - [x] 30-Day Starter Curriculum Seeder (`POST /api/v1/demo/seed`)
  - [x] Phone-ready read/log endpoints (`/api/v1/schedule/week`, `/api/v1/cards/due`, `/api/v1/blocks/{id}/complete`, `/api/v1/cards/{id}/review`)
  - [x] Automated tests for `/backend` (16/16 passed)

- [x] **Phase 2A: Python AI Intent Compiler & Secret Scrubber**
  - [x] `SecretScrubber` (regex + Shannon entropy redacting API keys, JWTs, env vars before LLM calls)
  - [x] `LLMRouter` with 3-part prompt sandwich (Docker Ollama default, Gemini Flash fallback, mock for offline tests)
  - [x] `ConstraintMutationAST` Pydantic v2 schema with Self-Healing Validator Loop (max 2 retries)
  - [x] `SymbolicDateResolver` resolving symbolic anchors to exact `Asia/Kolkata` ISO-8601 timestamps
  - [x] Ambiguity Elicitation returning one-tap clarification options for vague user prompts
  - [x] `POST /api/v1/ai/parse-intent` endpoint with 15-case smoke evaluation (17/17 tests passed in `/ai-service`)

- [x] **Phase 2B: OR-Tools CP-SAT Scheduler, Schedule Diff & Spring Boot Replan Wiring**
  - [x] Google OR-Tools CP-SAT model over 7-day horizon with 15-minute resolution
  - [x] Hard constraints: zero overlap, non-past, protected leisure, restorative sleep protection (23:00-07:00)
  - [x] Soft constraints with slack variables: full schedule, floor downgrade, deferral (`sum == 1`)
  - [x] Schedule Diff & Constraint Explanation generator (Git-style reason breakdown)
  - [x] Slack-Based Overload Tradeoff Cards generator (capacity negotiation options)
  - [x] `POST /api/v1/solver/solve` and `POST /api/v1/ai/replan` in FastAPI (23/23 tests passed in `/ai-service`)
  - [x] Spring Boot Two-Phase Replan workflow (`PROPOSED` -> `CONFIRMED`/`REJECTED`) (18/18 tests passed in `/backend`)

- [x] **Phase 3A: Phone-First Android App, Part 1 (Offline Snapshot, Exact Alarms, BootReceiver & Today Screen)**
  - [x] Kotlin + Jetpack Compose Material 3 + Retrofit + Room project targeting Android 16 (API 36)
  - [x] `CachedWeekSnapshotEntity` for 100% offline schedule and flashcard access
  - [x] `PendingCommandEntity` offline outbox synced via `WorkManager`
  - [x] Android 12+ Exact Alarm permission handler and battery optimization helper
  - [x] `BootCompletedReceiver` restoring exact reminders on reboot without server connection
  - [x] Notifications 5 min before blocks with Session Warm-Up Briefs and Emergency Floor switch
  - [x] Today & Commute Screen + Settings & Diagnostics Screen
  - [x] Debug APK built and installed via ADB directly onto connected Android phone (`00091349R001536`)

- [x] **Phase 3B: Phone-First Android App, Part 2 (Fast-Path Router, Voice Replan & Flashcards)**
  - [x] On-device Fast-Path Router (`done`, `snooze 15m/30m`, `emergency floor` in <10ms offline with visual latency telemetry)
  - [x] Voice & Natural Language Replan Screen with speech recognition, visual schedule diff, and 1-tap confirmation
  - [x] Commute Flashcards Screen with swipeable/flippable FSRS-4.5 cards and offline grade queue
  - [x] Verified on physical Android 16 hardware (`00091349R001536`) with all 4 screens and end-to-end sync

- [x] **Phase 4: GitHub Commit Polling, Diff-to-Flashcard Pipeline & Feynman Voice Grader (COMPLETE MVP!)**
  - [x] GitHub Commit Poller via REST API + manual diff endpoint
  - [x] Secret-scrubbed diff autopsy inside `<untrusted_code_diff>` tags generating cards and warm-up briefs
  - [x] Feynman Voice Grader evaluating spoken transcripts against 4-point rubrics
  - [x] Android Feynman voice review interface with 60-second explanation capture

- [x] **Phase 5: Next.js 15 Web Control Center, Telemetry Dashboard & Recruiter Sandbox**
  - [x] Next.js 15 App Router + Tailwind CSS + Lucide icons
  - [x] Weekly schedule grid, replan console, diff viewer, and Feynman Dojo
  - [x] Duration bias telemetry dashboard with collapsible `_debug_trace` Flight Recorder
  - [x] 1-Click "Load Recruiter Demo Persona" button

- [x] **Phase 6: Human-Verified Evaluation Suite, CI Pipeline & QLoRA Distillation (ALL PHASES 100% COMPLETE!)**
  - [x] Golden intent (65 cases) and Feynman rubric (20 cases) evaluation suites with CLI verification tool (`verify_eval_cases.py`)
  - [x] Full evaluation benchmark runner (`run_full_benchmark.py`) generating human-verified scorecard report in `EVALS.md`
  - [x] Architecture documentation with 4 Mermaid diagrams (`ARCHITECTURE.md`)
  - [x] GitHub Actions automated CI verification pipeline (`.github/workflows/ci.yml`)
  - [x] Standalone QLoRA distillation script (`ai-service/notebooks/train_qlora.py`) with RTX 5060 Blackwell CUDA verification
