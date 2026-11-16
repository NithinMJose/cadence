# Cadence AI Technical Map & Architecture Context

## Current Phase Status: PHASE 2B COMPLETE (Awaiting User Verification)
- **Active Phase:** Phase 2B (OR-Tools CP-SAT Scheduler, Schedule Diff & Spring Boot Replan Wiring)
- **Next Phase:** Phase 3A (Phone-First Android App, Part 1: Offline Snapshot, Exact Alarms, BootReceiver & Today Screen)
- **CP-SAT Specs:** 7-day horizon, 15-min buckets ($672$ slots), hard non-overlap, sleep protection (23:00-07:00), slack variables ($x_{\text{full}} + x_{\text{floor}} + x_{\text{deferred}} = 1$) preventing infeasibility crashes.
- **FSRS-4.5 Spec:** 17 parameters ($w_0..w_{16}$), default weights from `open-spaced-repetition/py-fsrs` v2.0.0, power-law forgetting curve $R(t, S) = (1 + F \cdot \frac{t}{S})^C$ with $C = -0.5, F \approx 0.234568$. Runs on Java backend only.

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
│   ├── app/
│   │   ├── config.py                   # Pydantic Settings configuration
│   │   ├── main.py                     # App factory, X-Trace-Id middleware, RFC 7807 handler
│   │   ├── llm/ (date_resolver, gemini_client, prompts, router, validator)
│   │   ├── models/ (ast, gpu_test, intent, problem_detail, replan, trace)
│   │   ├── routers/ (gpu, health, intent, replan, solver)
│   │   ├── security/ (scrubber.py)     # SecretScrubber (regex + Shannon entropy redacting keys/tokens)
│   │   ├── services/ (ollama_client.py)# Ollama HTTP client (generate + /api/ps)
│   │   └── solver/
│   │       ├── cpsat_scheduler.py      # CadenceCpSatSolver (OR-Tools CP-SAT discrete optimizer)
│   │       ├── diff_explainer.py       # DiffExplainer (Git-style causal reason explanations)
│   │       ├── models.py               # SolverTask, BlockedWindow, LeisureWindow, DiffItem, TradeoffCard
│   │       └── tradeoff_generator.py   # Slack-Based Overload Tradeoff Cards generator
│   ├── tests/ (test_date_resolver, test_gpu_smoke, test_health, test_intent_parser, test_scrubber, test_solver, test_validator)
│   ├── Dockerfile, pyproject.toml, requirements.txt, uv.lock
└── backend/                            # Java 21 / Spring Boot 3.4 core backend
    ├── src/main/java/com/cadence/
    │   ├── client/ (AiServiceClient)   # RestClient communicating with ai-service:8000
    │   ├── config/ (ApiKeyAuthFilter, SecurityConfig, TimezoneConfig, TraceIdFilter)
    │   ├── controller/ (CardController, DemoSeederController, ScheduleController, ScheduleReplanController, SystemController)
    │   ├── dto/ (BlockComplete, CardReview, ProblemDetail, WeeklySchedule, replan/*)
    │   ├── exception/ (CadenceException, GlobalExceptionHandler)
    │   ├── model/ (Users, Goals, TopicCards, Constraints, TimeBlocks, Proposals, Stats, Telemetry, Enums)
    │   ├── repository/ (8 JPA repositories for all domain entities)
    │   └── service/ (BurnoutGuardService, CurriculumSeederService, DurationCalibratorService, FsrsService, ScheduleReplanService)
    ├── src/main/resources/ (application.yml, db/migration/V1__init_cadence_schema.sql, V2__add_proposed_blocks_to_proposals.sql)
    ├── src/test/ (FsrsServiceTest, DurationCalibratorTest, BurnoutGuardTest, SystemControllerTest, ScheduleReplanIntegrationTest, CurriculumAndEndpointIntegrationTest)
    ├── Dockerfile, mvnw, mvnw.cmd, pom.xml
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
