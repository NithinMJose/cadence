# Cadence Architecture & Engineering Specification (`ARCHITECTURE.md`)

Cadence is a phone-first adaptive AI learning coach and combinatorial constraint scheduler built for engineers preparing for high-intensity engineering interviews. Cadence enforces a strict boundary between probabilistic AI (natural language intent compilation and code comprehension) and deterministic mathematics (combinatorial optimization via Google OR-Tools CP-SAT and spaced repetition memory modeling via FSRS-4.5).

---

## 1. System Overview & Clean Separation Boundary

The architecture explicitly rejects end-to-end LLM hallucinations for scheduling, time-math, and memory tracking. The LLM translates ambiguous natural language requests into structured Constraint Mutation Abstract Syntax Trees (ASTs), which are validated and passed to deterministic solvers.

```mermaid
flowchart TD
    User["User (Phone / Web)"] -->|"Spoken / Text Utterance"| Gateway["API Gateway / Security Filter"]
    Gateway -->|"Scrubbed Utterance"| AIService["AI Service (FastAPI)"]
    
    subgraph AIServiceSub["AI Probabilistic Boundary (FastAPI)"]
        Scrubber["Secret Scrubber (Entropy + Regex)"]
        Router["LLM Router (Ollama / Gemini / Mock)"]
        Validator["Self-Healing Schema Validator"]
        DateResolver["Symbolic Date Resolver"]
        Scrubber --> Router --> Validator --> DateResolver
    end

    AIService -->|"ConstraintMutationAST (JSON)"| Backend["Spring Boot 3.4 Orchestrator"]
    
    subgraph DeterministicSub["Deterministic Mathematical Boundary"]
        CPSATSolver["Google OR-Tools CP-SAT Scheduler"]
        FSRSEngine["FSRS-4.5 Memory Engine"]
        CapacityEngine["Slack Variable Capacity Negotiator"]
        EMACalibrator["EMA Duration Multiplier Calibrator"]
    end

    Backend --> CPSATSolver
    Backend --> FSRSEngine
    CPSATSolver --> CapacityEngine
    Backend --> PostgreSQL[("PostgreSQL 16")]
    Backend -->|"Synced TimeBlocks & Cards"| AndroidRoom[("Android Room SQLite (Offline)")]
```

---

## 2. Autonomous Laptop-Off Android Sync Architecture

Cadence is designed for uninterrupted operation even when the user's laptop or workstation is completely powered off or disconnected.

```mermaid
flowchart TD
    subgraph LaptopHost["Laptop Workstation (When Online)"]
        HostServer["Spring Boot Backend (:8080)"]
        AIWorker["FastAPI AI & Solver (:8000)"]
        DBStore[("PostgreSQL 16")]
        HostServer --- DBStore
        HostServer --- AIWorker
    end

    subgraph PhoneDevice["Android Physical Device (Cadence Client)"]
        ExactAlarm["Android Exact Alarm Manager"]
        RoomDB[("Local Room SQLite DB")]
        SyncWorker["CadenceSyncWorker (WorkManager)"]
        TodayUI["Today's Schedule View"]
        CardsUI["Flashcards & Feynman Review"]
        PreBrief["5-Min Pre-Session Reminder Service"]
        EmergencyFloor["1-Tap Emergency Floor Engine"]
    end

    HostServer <-->|"Periodic Delta Sync (HTTP / ADB Reverse)"| SyncWorker
    SyncWorker -->|"Store Week Schedule & Cards"| RoomDB
    RoomDB --> TodayUI
    RoomDB --> CardsUI
    RoomDB --> PreBrief
    PreBrief --> ExactAlarm
    TodayUI -->|"Offline Action"| EmergencyFloor
    EmergencyFloor -->|"Immediate Local Commit (<10ms)"| RoomDB
```

### Key Principles of Offline Operation:
1. **Full Horizon Caching:** The 7-day schedule and all active FSRS cards are cached inside local Android Room SQLite.
2. **Exact Alarms (`SCHEDULE_EXACT_ALARM`):** Pre-session reminders trigger reliably 5 minutes before study sessions, even while the phone is in deep Doze mode.
3. **Emergency Floor:** If energy drops or the user is exhausted, a single tap drops a 60-minute deep practice block to a 3-minute emergency floor locally in `<10ms` without waiting for network connectivity.

---

## 3. GitHub Diff Autopsy & Feynman Spaced Repetition Pipeline

Cadence ingests GitHub commit diffs from the developer's repositories, redacts sensitive tokens, extracts architectural interview concepts, and feeds them into the FSRS-4.5 spaced repetition review queue.

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Git as GitHub Repository
    participant Poller as GitHubCommitPoller (Spring Boot)
    participant Autopsy as CommitAutopsyService (FastAPI)
    participant DB as PostgreSQL 16
    participant Android as Android Physical Device
    participant Feynman as FeynmanGraderService (FastAPI)
    participant FSRS as FsrsService (Java)

    Dev->>Git: git push origin main
    Poller->>Git: Poll Recent Commits via REST API
    Git-->>Poller: Raw Commit Diffs & Metadata
    Poller->>Autopsy: POST /api/v1/diff-autopsy (raw_diff)
    Note over Autopsy: 1. Scrub Secrets & Strip Injection<br/>2. Extract 4-Point Technical Rubric
    Autopsy-->>Poller: Extracted Topic Cards with Rubrics
    Poller->>DB: Upsert Topic Cards (Due Tomorrow)
    DB-->>Android: Sync Topic Cards into Room SQLite
    
    Dev->>Android: Spoken Concept Explanation (60s)
    Android->>Feynman: POST /api/v1/feynman-grade (transcript + rubric)
    Note over Feynman: Score Spoken Keywords against Rubric
    Feynman-->>Android: FSRS Grade (1..4) + Constructive Feedback
    Android->>FSRS: Apply Grade to FSRS Memory State
    Note over FSRS: Update Stability (S), Difficulty (D), Interval (I)
    FSRS->>DB: Persist Next Due Date
```

---

## 4. Google OR-Tools CP-SAT Soft Slack Variable Capacity Negotiation

When the user adds new hard commitments or blocks schedule windows, Cadence's CP-SAT solver optimizes time assignments while penalizing schedule disruptions and fatigue.

```mermaid
flowchart LR
    InputReq["Solver Request (Tasks, Blocked, Horizon)"] --> Solver["OR-Tools CP-SAT Optimizer"]
    
    subgraph HardConstraints["Hard Constraints (Must Satisfy)"]
        NoOverlap["No Task Overlap (IntervalVar)"]
        RespectBlocked["Honor Blocked Windows (Sleep, Work, Lunch)"]
        VenueMatch["Device Venue Match (Phone vs Laptop)"]
        DailyLimit["Daily Cognitive Study Limit (<= 120m)"]
    end
    
    subgraph SoftObjectives["Soft Objective Minimization"]
        SlackPen["Slack Variable Penalties ($1000 per lost flex min)"]
        ChurnPen["Schedule Churn Penalties ($50 per moved block)"]
        ScatterPen["Dispersed Fragment Penalties ($20 per break)"]
    end

    Solver --> HardConstraints
    Solver --> SoftObjectives
    SoftObjectives --> Solution{"Feasible Solution?"}
    Solution -->|"Optimal Feasible"| ResultOk["Optimal Schedule Generated"]
    Solution -->|"Capacity Overloaded"| Tradeoffs["Generate Trade-off Proposals"]
    Tradeoffs --> Proposal1["Option A: Drop Lowest Priority Block"]
    Tradeoffs --> Proposal2["Option B: Downgrade to Emergency Floor"]
    Tradeoffs --> Proposal3["Option C: Borrow from Sunday Flex Buffer"]
```

---

## 5. Subsystem Summary & Technology Stack

| Layer | Technology | Primary Responsibility | Determinism Model |
|---|---|---|---|
| **Mobile Client** | Android 16 (API 36), Kotlin, Jetpack Compose, Room SQLite | On-device schedule execution, exact pre-session alarms, 1-tap floor actions, Feynman recording | 100% Deterministic SQLite |
| **Desktop Web** | Next.js 15, React 19, TypeScript, Tailwind CSS, Lucide | 7-day visual schedule grid, natural language replan console with diffs, autopsy dojo, telemetry | Strict TypeScript Types |
| **Core Backend** | Java 21, Spring Boot 3.4, Spring Security, Hibernate | API orchestration, GitHub commit poller, FSRS-4.5 spaced repetition state machine, PostgreSQL sync | 100% Deterministic Math |
| **AI Service** | Python 3.12, FastAPI, Pydantic v2, OR-Tools 9.11 | LLM intent translation, secret scrubbing, CP-SAT combinatorial scheduling, Feynman rubric grading | Hybrid (AI Translation + CP-SAT Math) |
| **Local LLM** | Ollama (`qwen2.5:7b-instruct-q4_K_M`), NVIDIA RTX 5060 | Offline local prompt compilation and Feynman spoken transcript comprehension | Probabilistic Translation |

---

## 6. End-to-End Latency & Reliability Guarantees

1. **Deterministic Schedulers:** CP-SAT schedules 7-day horizons (672 time slots of 15 minutes) in `< 25ms`.
2. **FSRS Spaced Repetition Updates:** Next review intervals computed in `< 1ms` in Java with reference-verified vectors.
3. **Intent Compilation:** Natural language requests translated and grounded into exact ISO timestamps with `< 250ms` p50 latency.
4. **Flight Recorder (`_debug_trace`):** Every API response carries immutable provenance metadata including trace ID, model name, provider latency, validation retries, and scrub count.
