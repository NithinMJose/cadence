# Cadence: Adaptive AI Learning Coach & Constraint Scheduler

Cadence is a phone-first adaptive learning coach and constraint scheduler. It couples deterministic math (Google OR-Tools CP-SAT scheduling and FSRS-4.5 spaced repetition) with local LLM semantic translation (`qwen2.5:7b-instruct-q4_K_M` running on an RTX 5060 Blackwell GPU with Gemini 2.5 Flash cloud fallback).

---

## Live Status Dashboard

| Milestone | Status | Description |
|---|---|---|
| **Phase 0: Scaffold & GPU Smoke Test** | **VERIFIED COMPLETE** | Monorepo scaffold, Docker GPU passthrough, auto-pull container, `/ai-service` and `/backend` skeletons with passing automated tests. |
| **Phase 1: System of Record & FSRS-4.5** | Pending User Signal | PostgreSQL schema, Flyway migrations, reference-verified Java FSRS-4.5, duration bias calibrator, burnout guard, and curriculum seeder. |
| **Phase 2A: Intent Compiler & Scrubber** | Queued | Pydantic v2 AST, secret scrubber, self-healing validator loop, symbolic date resolver (`Asia/Kolkata`), and ambiguity elicitation. |
| **Phase 2B: CP-SAT Solver & Replan** | Queued | OR-Tools CP-SAT solver, schedule diff explanations, overload tradeoff cards, and two-phase replan confirmation. |
| **Phase 3A: Android App (Part 1)** | Queued | Offline snapshot, exact alarm manager, boot receiver, and Today/Commute screen. |
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
| **Session Warm-Up Brief** | 3-bullet context refresher shown 5 minutes before a deep coding block. | Extracted from recent git diffs and notes to reload memory before work starts. |
| **Minimum Viable Session / Emergency Floor** | 1-tap downgrade from a 45-minute laptop block to a 3-minute phone quiz. | Guaranteed non-zero daily progress card scheduled to prevent streak breaks on busy days. |
| **Flight Recorder (`_debug_trace`)** | Telemetry block attached to every AI and solver response. | End-to-end auditability tracking LLM provider, latency, prompt tokens, validation retries, and solver wall time. |

---

## Phase 0 Quickstart & Verification

### 1. Requirements
- Docker & Docker Compose (v2.20+)
- Java 21+ (Java 24 verified)
- Python 3.12+ (managed with `uv`)
- NVIDIA Driver >= 570.xx with RTX 5060 (Blackwell architecture) for local GPU acceleration

### 2. Run Automated Unit Tests

#### Python AI Service (`/ai-service`):
```bash
cd ai-service
uv run pytest -v
```
*(Runs 5 unit tests validating health probe, active GPU VRAM residency calculation, CPU fallback diagnostics, RFC 7807 problem details, and trace header propagation).*

#### Spring Boot Backend (`/backend`):
```bash
# Windows:
cd backend
.\mvnw.cmd test

# Linux/macOS:
cd backend
./mvnw test
```
*(Runs 5 integration tests validating public `/health`, 401 unauthorized rejection, `X-Cadence-Api-Key` authentication, `Authorization: Bearer` authentication, and trace propagation).*

### 3. Docker Compose Stack
```bash
# Copy template environment file
cp .env.example .env

# Start all containers in detached mode
docker compose up -d

# Check running containers
docker compose ps

# Verify GPU smoke test endpoint
curl -s http://localhost:8000/api/v1/ai/gpu-smoke-test
```

### 4. RTX 5060 (Blackwell) Diagnostics
When testing against local Ollama, `/api/v1/ai/gpu-smoke-test` queries Ollama's `/api/ps` endpoint:
- **GPU Confirmed (`SUCCESS`):** Model reports `size_vram > 0` (100% offload indicates all layers reside in GPU memory).
- **CPU Fallback (`CPU_FALLBACK`):** If `size_vram == 0`, check host NVIDIA driver (>= 570.xx required for Blackwell sm_120) and verify `capabilities: [gpu]` in `docker-compose.yml`.
