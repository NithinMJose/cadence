# Cadence AI Technical Map & Architecture Context

## Current Phase Status: PHASE 0 COMPLETE (Awaiting User Verification)
- **Active Phase:** Phase 0 (Monorepo Scaffold, Docker GPU + Auto-Pull Setup & RTX 5060 Smoke Test)
- **Next Phase:** Phase 1 (Spring Boot System of Record, Auth, Timezone Anchoring & FSRS-4.5 Engine)
- **FSRS-4.5 Spec:** 17 parameters ($w_0..w_{16}$), power-law curve $R(t, S) = (1 + F \cdot \frac{t}{S})^C$, Java backend only.

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
│   │   ├── main.py                     # FastAPI entrypoint, X-Trace-Id middleware, RFC 7807 handler
│   │   ├── models/
│   │   │   ├── gpu_test.py             # Schema for GPU smoke test & VRAM telemetry
│   │   │   ├── problem_detail.py       # RFC 7807 ProblemDetail & CadenceServiceException
│   │   │   └── trace.py                # Flight recorder _debug_trace schema
│   │   ├── routers/
│   │   │   ├── gpu.py                  # GET /api/v1/ai/gpu-smoke-test
│   │   │   └── health.py               # GET /health
│   │   └── services/
│   │       └── ollama_client.py        # Ollama HTTP client (generate + /api/ps)
│   ├── tests/
│   │   ├── conftest.py                 # Async test client fixture
│   │   ├── test_gpu_smoke.py           # Active GPU, CPU fallback, and 503 ProblemDetail tests
│   │   └── test_health.py              # Healthcheck and trace propagation tests
│   ├── Dockerfile
│   ├── pyproject.toml
│   ├── requirements.txt
│   └── uv.lock                         # Pinned Python lockfile
└── backend/                            # Java 21 / Spring Boot 3.4 core backend
    ├── src/
    │   ├── main/
    │   │   ├── java/com/cadence/
    │   │   │   ├── CadenceBackendApplication.java
    │   │   │   ├── config/
    │   │   │   │   ├── ApiKeyAuthFilter.java       # Stateless API key & Bearer token filter
    │   │   │   │   ├── SecurityConfig.java         # Stateless Spring Security config
    │   │   │   │   ├── TimezoneConfig.java         # Asia/Kolkata timezone bean
    │   │   │   │   └── TraceIdFilter.java          # X-Trace-Id MDC propagation filter
    │   │   │   ├── controller/
    │   │   │   │   └── SystemController.java       # Public /health and protected /auth-check
    │   │   │   ├── dto/
    │   │   │   │   ├── AuthCheckResponse.java
    │   │   │   │   ├── ProblemDetailResponse.java  # RFC 7807 response DTO
    │   │   │   │   └── SystemHealthResponse.java
    │   │   │   └── exception/
    │   │   │       ├── CadenceException.java       # Domain exception with ProblemDetail fields
    │   │   │       └── GlobalExceptionHandler.java # Uncaught exception mapper
    │   │   └── resources/
    │   │       └── application.yml
    │   └── test/
    │       └── java/com/cadence/controller/
    │           └── SystemControllerTest.java       # MockMvc tests for health and auth
    ├── Dockerfile
    ├── mvnw / mvnw.cmd                 # Maven wrapper
    └── pom.xml
```

---

## Active API Endpoints (Phase 0)
| Method | Endpoint | Service | Auth Required | Description |
|---|---|---|---|---|
| `GET` | `/health` | `ai-service:8000` | No | AI service readiness & trace probe |
| `GET` | `/api/v1/ai/gpu-smoke-test` | `ai-service:8000` | No | Ollama generation + `/api/ps` VRAM residency check |
| `GET` | `/api/v1/system/health` | `backend:8080` | No | Backend service readiness & timezone check |
| `GET` | `/api/v1/system/auth-check` | `backend:8080` | Yes (`X-Cadence-Api-Key` or `Bearer`) | Validates authentication credentials |

---

## RTX 5060 Blackwell (sm_120) Specifications
- **Architecture:** NVIDIA Blackwell GPU Architecture (Compute Capability `12.0` / `sm_120`).
- **Host Drivers:** NVIDIA Driver `>= 570.xx` on Windows/Linux host.
- **Docker Passthrough:** `deploy.resources.reservations.devices` with `capabilities: [gpu]` targeting Ollama container.
- **VRAM Telemetry:** Ollama `GET /api/ps` inspects `size_vram` vs `size` to confirm weights run on GPU tensor cores rather than falling back to CPU.
