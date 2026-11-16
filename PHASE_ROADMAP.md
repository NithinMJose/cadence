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

- [ ] **Phase 2A: Python AI Intent Compiler & Secret Scrubber**
  - [ ] `SecretScrubber` (regex + Shannon entropy redacting API keys, JWTs, env vars before LLM calls)
  - [ ] `LLMRouter` with 3-part prompt sandwich (Docker Ollama default, Gemini Flash fallback, mock for offline tests)
  - [ ] `ConstraintMutationAST` Pydantic v2 schema with Self-Healing Validator Loop (max 2 retries)
  - [ ] `SymbolicDateResolver` resolving symbolic anchors to exact `Asia/Kolkata` ISO-8601 timestamps
  - [ ] Ambiguity Elicitation returning one-tap clarification options for vague user prompts
  - [ ] `POST /api/v1/ai/parse-intent` endpoint with 15-case smoke evaluation

- [ ] **Phase 2B: OR-Tools CP-SAT Scheduler, Schedule Diff & Spring Boot Replan Wiring**
  - [ ] Google OR-Tools CP-SAT model over 7-day horizon with 15-minute resolution
  - [ ] Hard constraints: zero overlap, non-past, protected leisure, device match (`PHONE_COMMUTE` vs `LAPTOP_DEEP`)
  - [ ] Soft constraints with slack variables: full schedule, floor downgrade, deferral (`sum == 1`)
  - [ ] Schedule Diff & Constraint Explanation generator (Git-style reason breakdown)
  - [ ] Slack-Based Overload Tradeoff Cards generator (capacity negotiation options)
  - [ ] `POST /api/v1/solver/solve` and `POST /api/v1/ai/replan` in FastAPI
  - [ ] Spring Boot Two-Phase Replan workflow (`PROPOSED` -> `CONFIRMED`)

- [ ] **Phase 3A: Phone-First Android App, Part 1 (Offline Snapshot, Exact Alarms, BootReceiver & Today Screen)**
  - [ ] Kotlin + Jetpack Compose Material 3 + Hilt + Retrofit + Room project
  - [ ] `CachedWeekSnapshotEntity` for 100% offline schedule and flashcard access
  - [ ] `PendingCommandEntity` offline outbox synced via `WorkManager`
  - [ ] Android 12+ Exact Alarm permission handler and battery optimization helper
  - [ ] `BootCompletedReceiver` restoring exact reminders on reboot without server connection
  - [ ] Notifications 5 min before blocks with Session Warm-Up Briefs and Emergency Floor switch
  - [ ] Today & Commute Screen + Settings & Diagnostics Screen

- [ ] **Phase 3B: Phone-First Android App, Part 2 (Fast-Path Router, Voice Replan & Flashcards)**
  - [ ] On-device Fast-Path Router (`done`, `snooze 30m`, `emergency floor` in <10ms offline)
  - [ ] Voice Replan Screen with speech recognition, visual schedule diff, and 1-tap confirmation
  - [ ] Commute Flashcards Screen with swipeable FSRS cards and offline grade queue

- [ ] **Phase 4: GitHub Commit Polling, Diff-to-Flashcard Pipeline & Feynman Voice Grader (COMPLETE MVP!)**
  - [ ] GitHub Commit Poller via REST API + manual diff endpoint
  - [ ] Secret-scrubbed diff autopsy inside `<untrusted_code_diff>` tags generating cards and warm-up briefs
  - [ ] Feynman Voice Grader evaluating spoken transcripts against 4-point rubrics
  - [ ] Android Feynman voice review interface with 60-second explanation capture

- [ ] **Phase 5: Next.js 15 Web Control Center, Telemetry Dashboard & Recruiter Sandbox**
  - [ ] Next.js 15 App Router + Tailwind CSS + Lucide icons
  - [ ] Weekly schedule grid, replan console, diff viewer, and Feynman Dojo
  - [ ] Duration bias telemetry dashboard with collapsible `_debug_trace` Flight Recorder
  - [ ] 1-Click "Load Recruiter Demo Persona" button

- [ ] **Phase 6: Human-Verified Evaluation Suite, CI Pipeline & QLoRA Distillation**
  - [ ] Golden intent and Feynman rubric evaluation suites with CLI verification tool
  - [ ] Architecture documentation with Mermaid diagrams and GitHub Actions CI
  - [ ] Standalone QLoRA distillation script with RTX 5060 Blackwell CUDA verification
