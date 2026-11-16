# Cadence: Adaptive AI Learning Coach & Constraint Scheduler

Cadence is a phone-first adaptive learning coach and constraint scheduler. It couples deterministic math (Google OR-Tools CP-SAT scheduling and FSRS-4.5 spaced repetition) with local LLM semantic translation (`qwen2.5:7b-instruct-q4_K_M` running on an RTX 5060 Blackwell GPU with Gemini 2.5 Flash cloud fallback).

---

## Live Status Dashboard

| Milestone | Status | Description |
|---|---|---|
| **Phase 0: Scaffold & GPU Smoke Test** | **VERIFIED COMPLETE** | Monorepo scaffold, Docker GPU passthrough, auto-pull container, `/ai-service` and `/backend` skeletons with passing automated tests. |
| **Phase 1: System of Record & FSRS-4.5** | **VERIFIED COMPLETE** | PostgreSQL schema, Flyway migrations, reference-verified Java FSRS-4.5 (17 weights), duration bias calibrator (EMA $\alpha=0.3$), burnout guard (flex credits banking), 30-day curriculum seeder, and phone-ready read/log endpoints. |
| **Phase 2A: Intent Compiler & Scrubber** | Queued | Pydantic v2 AST, secret scrubber, self-healing validator loop, symbolic date resolver (`Asia/Kolkata`), and ambiguity elicitation. |
| **Phase 2B: CP-SAT Solver & Replan** | Queued | OR-Tools CP-SAT solver, schedule diff explanations, overload tradeoff cards, and two-phase replan confirmation. |
| **Phase 3A: Android App (Part 1)** | Queued | Offline snapshot, exact alarm manager, boot receiver, and Today/Commute screen. *(Can run directly now!)* |
| **Phase 3B: Android App (Part 2)** | Queued | On-device fast-path router, voice replan, and commute flashcards screen. |
| **Phase 4: GitHub Pipeline & Feynman Grader** | Queued | Commit diff autopsy, FSRS card generator, warm-up briefs, and Feynman voice grader. |
| **Phase 5: Next.js Web Control Center** | Queued | Desktop control center, side-by-side diff viewer, and telemetry dashboard. |
| **Phase 6: Evals, CI & QLoRA** | Queued | 60+ golden eval dataset, CI workflow, and RTX 5060 Blackwell QLoRA distillation check. |

---

## Plain-English Feature Reference

| Technical Feature | Plain-English Description | Engineering Law / Math Boundary |
|---|---|---|
| **Schedule Diff & Constraint Explanation** | Git-style before/after schedule view explaining why tasks moved. | Deterministic diff calculation comparing prior slot vs new slot with rule-based causal tagging. |
| **Slack-Based Overload Tradeoff Cards** | Capacity negotiation options when a week has more tasks than free hours. | Soft slack variables ($is\_downgraded\_to\_floor$, $is\_deferred$) inside CP-SAT solver to prevent raw `INFEASIBLE` crashes. |
| **Duration Bias Calibrator** | Multiplier automatically adjusting task estimates based on actual past speed. | Exponential Moving Average ($\alpha = 0.3$, clamped $[0.7, 2.5]$) per topic tag calculated in Java. |
| **Burnout Guard & Flex-Time Banking** | Auto-scales daily work caps and banks flex credits when finishing deep work early. | 7-day rolling completion rate; triggers recovery mode after 2+ consecutive skipped blocks. |
| **FSRS-4.5 Spaced Repetition Engine** | State-of-the-art DSR spaced repetition model running purely on the backend. | 17 parameters ($w_0..w_{16}$), power-law curve $R(t, S) = (1 + F \cdot \frac{t}{S})^C$. Tested against `py-fsrs` reference vectors. |
| **Session Warm-Up Brief** | 3-bullet context refresher shown 5 minutes before a deep coding block. | Extracted from recent git diffs and notes to reload memory before work starts. |
| **Minimum Viable Session / Emergency Floor** | 1-tap downgrade from a 45-minute laptop block to a 3-minute phone quiz. | Guaranteed non-zero daily progress card scheduled to prevent streak breaks on busy days. |
| **Flight Recorder (`_debug_trace`)** | Telemetry block attached to every AI and solver response. | End-to-end auditability tracking LLM provider, latency, prompt tokens, validation retries, and solver wall time. |

---

## Running Automated Tests

### 1. Spring Boot Backend (`/backend`):
```bash
cd backend
.\mvnw.cmd test    # Windows
./mvnw test        # Linux / macOS
```
*(Runs 16 tests validating FSRS-4.5 reference vectors, Duration Bias Calibrator EMA, Burnout Guard flex credits, public /health, API key authentication, and end-to-end curriculum seeding & retrieval).*

### 2. Python AI Service (`/ai-service`):
```bash
cd ai-service
uv run pytest -v
```
*(Runs 5 tests validating health probe, active GPU VRAM residency calculation, CPU fallback diagnostics, RFC 7807 problem details, and trace header propagation).*

---

## Phase 1 Phone-Ready API Endpoints

All protected endpoints accept `X-Cadence-Api-Key: cadence-dev-secret-key-change-me` or `Authorization: Bearer cadence-dev-secret-key-change-me`.

- **Seed 30-Day Curriculum:**
  `POST http://localhost:8080/api/v1/demo/seed`
- **Get Weekly Schedule (for Android Today screen):**
  `GET http://localhost:8080/api/v1/schedule/week`
- **Complete a Time Block:**
  `POST http://localhost:8080/api/v1/blocks/{blockId}/complete`
  `{"actual_duration_min": 45}`
- **Get Due Flashcards (for Commute screen):**
  `GET http://localhost:8080/api/v1/cards/due`
- **Submit Flashcard Review Grade:**
  `POST http://localhost:8080/api/v1/cards/{cardId}/review`
  `{"grade": 3}` *(1=Again, 2=Hard, 3=Good, 4=Easy)*
