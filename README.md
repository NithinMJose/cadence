# Cadence: Adaptive AI Learning Coach & Constraint Scheduler

Cadence is a phone-first adaptive learning coach and constraint scheduler. It couples deterministic math (Google OR-Tools CP-SAT scheduling and FSRS-4.5 spaced repetition) with local LLM semantic translation (`qwen2.5:7b-instruct-q4_K_M` running on an RTX 5060 Blackwell GPU with Gemini 2.5 Flash cloud fallback).

---

## Live Status Dashboard

| Milestone | Status | Description |
|---|---|---|
| **Phase 0: Scaffold & GPU Smoke Test** | **VERIFIED COMPLETE** | Monorepo scaffold, Docker GPU passthrough, auto-pull container, `/ai-service` and `/backend` skeletons with passing automated tests. |
| **Phase 1: System of Record & FSRS-4.5** | **VERIFIED COMPLETE** | PostgreSQL schema, Flyway migrations, reference-verified Java FSRS-4.5 (17 weights), duration bias calibrator (EMA $\alpha=0.3$), burnout guard (flex credits banking), 30-day curriculum seeder, and phone-ready read/log endpoints. |
| **Phase 2A: Intent Compiler & Scrubber** | **VERIFIED COMPLETE** | `SecretScrubber` (Shannon entropy + regex credential redaction), `LLMRouter` (3-part prompt sandwich), `ConstraintMutationAST` with `SelfHealingValidator` (max 2 retries), `SymbolicDateResolver` (`Asia/Kolkata`), ambiguity elicitation, and 15-case smoke evaluation. |
| **Phase 2B: CP-SAT Solver & Replan** | **VERIFIED COMPLETE** | Google OR-Tools CP-SAT scheduler, Git-style Schedule Diff & Constraint Explanation, Slack-Based Overload Tradeoff Cards, and Spring Boot Two-Phase Replan confirmation (`PROPOSED` -> `CONFIRMED`/`REJECTED`). |
| **Phase 3A: Android App (Part 1)** | **VERIFIED COMPLETE** | Kotlin + Compose Material 3, Room SQLite `CachedWeekSnapshotEntity`, offline outbox, exact alarm manager, boot receiver, and APK installed on connected device. |
| **Phase 3B: Android App (Part 2)** | **VERIFIED COMPLETE** | On-device Fast-Path Router (<10ms offline actions), Commute Flashcards screen (FSRS rating bar & rubric), Natural Language Replan screen (CP-SAT schedule diff & 1-tap confirmation), verified on Android 16 hardware. |
| **Phase 4: GitHub Pipeline & Feynman Grader** | **VERIFIED COMPLETE** | GitHub REST API commit poller, secret-scrubbed diff autopsy inside `<untrusted_code_diff>` tags, FSRS flashcard synthesis, Session Warm-Up Brief attachment, 3-minute Emergency Floor micro-quiz, and Feynman Voice Dojo grading with FSRS update. |
| **Phase 5: Next.js Web Control Center** | **VERIFIED COMPLETE** | Next.js 15 App Router, Tailwind CSS, Lucide icons, 4 desktop views (Weekly Schedule Grid, Natural Language Replan Console, GitHub Diff Autopsy & Feynman Flashcard Dojo, Duration Bias & Telemetry Dashboard with Flight Recorder), and 1-click Recruiter Demo Persona. |
| **Phase 6: Evals, CI & QLoRA** | Next Up | 60+ golden eval dataset, CI workflow, and RTX 5060 Blackwell QLoRA distillation check. |

---

## Plain-English Feature Reference

| Technical Feature | Plain-English Description | Engineering Law / Math Boundary |
|---|---|---|
| **Schedule Diff & Constraint Explanation** | Git-style before/after schedule view explaining why tasks moved. | Deterministic diff calculation comparing prior slot vs new slot with rule-based causal tagging. |
| **Slack-Based Overload Tradeoff Cards** | Capacity negotiation options when a week has more tasks than free hours. | Soft slack variables ($is\_downgraded\_to\_floor$, $is\_deferred$) inside CP-SAT solver to prevent raw `INFEASIBLE` crashes. |
| **Duration Bias Calibrator** | Multiplier automatically adjusting task estimates based on actual past speed. | Exponential Moving Average ($\alpha = 0.3$, clamped $[0.7, 2.5]$) per topic tag calculated in Java. |
| **Burnout Guard & Flex-Time Banking** | Auto-scales daily work caps and banks flex credits when finishing deep work early. | 7-day rolling completion rate; triggers recovery mode after 2+ consecutive skipped blocks. |
| **FSRS-4.5 Spaced Repetition Engine** | State-of-the-art DSR spaced repetition model running purely on the backend. | 17 parameters ($w_0..w_{16}$), power-law curve $R(t, S) = (1 + F \cdot \frac{t}{S})^C$. Tested against `py-fsrs` reference vectors. |
| **Secret Scrubber & Injection Defense** | Redacts credentials, tokens, and env vars before LLMs touch text or git diffs. | Deterministic regex + Shannon entropy (>4.0 bits/char) scanning. Untrusted text is strictly sandboxed. |
| **Self-Healing Validator Loop** | Corrects schema and formatting errors automatically if an LLM response fails validation. | Pydantic v2 validation feeding compiler error traces back to the model for up to 2 retries. |
| **Session Warm-Up Brief** | 3-bullet context refresher shown 5 minutes before a deep coding block. | Extracted from recent git diffs and notes to reload memory before work starts. |
| **Minimum Viable Session / Emergency Floor** | 1-tap downgrade from a 45-minute laptop block to a 3-minute phone quiz. | Guaranteed non-zero daily progress card scheduled to prevent streak breaks on busy days. |
| **Flight Recorder (`_debug_trace`)** | Telemetry block attached to every AI and solver response. | End-to-end auditability tracking LLM provider, latency, prompt tokens, validation retries, and solver wall time. |

---

## Running Automated Tests

### 1. Python AI Service (`/ai-service`):
```bash
cd ai-service
uv run pytest -v
```
*(Runs 23 tests validating health probe, GPU VRAM residency calculation, secret scrubber, symbolic date resolver, self-healing validator loop, 15-case intent eval, OR-Tools CP-SAT scheduler, Schedule Diff explainer, and Slack-Based Overload Tradeoff Cards).*

### 2. Spring Boot Backend (`/backend`):
```bash
cd backend
.\mvnw.cmd test    # Windows
./mvnw test        # Linux / macOS
```
*(Runs 18 tests validating FSRS-4.5 reference vectors, Duration Bias Calibrator EMA, Burnout Guard flex credits, public /health, API key authentication, and end-to-end curriculum seeding & Two-Phase Replan proposal and confirmation).*

### 3. Android Mobile Client (`/android`):
```bash
cd android
.\gradlew.bat testDebugUnitTest    # Windows
./gradlew testDebugUnitTest        # Linux / macOS
```
*(Runs unit tests validating 5-minute pre-session reminder calculation and Emergency Floor state transitions).*

#### Install APK & Reverse Port for Local Backend:
```bash
adb reverse tcp:8080 tcp:8080
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Two-Phase Replan Workflow (`/api/v1/schedule/replan`)

1. **Phase 1: Propose Replan**
```bash
curl -i -X POST http://localhost:8080/api/v1/schedule/replan/propose \
  -H "X-Cadence-Api-Key: cadence-dev-secret-key-change-me" \
  -H "Content-Type: application/json" \
  -d '{"user_message": "Block Friday evening from 7pm to 11pm for party"}'
```
Returns a `PENDING` proposal with Git-style `diff_items`, capacity negotiation `tradeoff_cards` (if overloaded), and flight recorder `_debug_trace`.

2. **Phase 2: Confirm or Reject Replan**
```bash
# Confirm and atomically commit proposed blocks to database:
curl -i -X POST http://localhost:8080/api/v1/schedule/replan/{proposalId}/confirm \
  -H "X-Cadence-Api-Key: cadence-dev-secret-key-change-me"

# Or discard the proposal:
curl -i -X POST http://localhost:8080/api/v1/schedule/replan/{proposalId}/reject \
  -H "X-Cadence-Api-Key: cadence-dev-secret-key-change-me"
```

---

## Intent Compiler API Usage (`POST /api/v1/ai/parse-intent`)

```bash
curl -i -X POST http://localhost:8000/api/v1/ai/parse-intent \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: demo-trace-intent" \
  -d '{
    "prompt": "Dentist on Friday evening, block it out",
    "reference_date": "2026-11-16T10:00:00+05:30",
    "timezone": "Asia/Kolkata"
  }'
```

Returns:
- Deterministic AST with symbolic anchors (`target_weekday: "FRIDAY"`, `window: "EVENING"`).
- Deterministically resolved ISO-8601 windows (`2026-11-20T17:00:00+05:30` to `2026-11-20T21:00:00+05:30`).
- `_debug_trace` flight recorder with `secrets_redacted_count`, `validation_retries_used`, and `llm_latency_ms`.
