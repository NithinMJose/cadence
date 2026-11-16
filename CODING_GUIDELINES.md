# Cadence Engineering Guidelines & Architectural Laws

## 1. Core Architectural Separation: Deterministic Math vs. LLM
Cadence is built on a strict boundary between probabilistic LLMs and deterministic code:
- **LLM Responsibilities (Semantic Translation Only):**
  - Parsing messy voice/text commands into symbolic Pydantic v2 schemas (`ConstraintMutationAST`).
  - Analyzing secret-scrubbed `git diff` code snippets to generate FSRS flashcards, 3-bullet Session Warm-Up Briefs, and 3-minute Emergency Floor micro-quizzes.
  - Grading 60-second Feynman voice explanations against 4-point rubrics (grades 1..4).
  - The LLM is NEVER permitted to perform calendar arithmetic, compute spaced repetition intervals, or directly produce schedule timetables.
- **Deterministic Code Responsibilities:**
  - Date & Timezone resolution: converting symbolic anchors (`day_offset`, `EVENING`) to ISO-8601 timestamps in `Asia/Kolkata`.
  - Combinatorial scheduling: Google OR-Tools CP-SAT assigning slots, enforcing hard constraints, and negotiating capacity using soft slack variables.
  - Spaced repetition math: FSRS-4.5 engine running purely in Java 21 Spring Boot.
  - Duration bias calibration: Exponential Moving Average (EMA, $\alpha=0.3$) scaling estimated task durations.
  - Burnout Guard & Flex-Time banking: 7-day rolling completion tracking and flex credit allocation.

## 2. Anti-Slop & Production Code Quality Laws
1. **Zero Stubs & Zero Placeholders:**
   - Never write `// TODO`, `# TODO`, `pass`, or mock return values in production code.
   - Every method must be fully implemented, handled, and verified.
2. **Zero Silent Failures (RFC 7807 ProblemDetail):**
   - Never swallow exceptions or return ambiguous HTTP 500 without details.
   - All errors must be serialized as RFC 7807 `ProblemDetail` with `error_code`, `message`, `trace_id`, and `remediation_hint`.
3. **Strict Modularity & File Size Limit:**
   - Keep source files strictly under **250 lines of code**.
   - Decouple controllers, service logic, repositories, models, scrubbers, and validators into focused single-responsibility files.
4. **Plain-English Naming Alongside Technical Features:**
   - Always pair technical architecture terms with plain-English descriptions in comments, UI, and documentation:
     - *Schedule Diff & Constraint Explanation* (Git-style before/after schedule view explaining why tasks moved)
     - *Slack-Based Overload Tradeoff Cards* (Capacity negotiation options using CP-SAT soft slack variables when a week is overbooked)
     - *Duration Bias Calibrator* (EMA multiplier adjusting task estimates based on actual completion time)
     - *Session Warm-Up Brief* (3-bullet context refresher shown 5 minutes before a coding block)
     - *Minimum Viable Session / Emergency Floor* (One-tap downgrade from a 45-min laptop block to a 3-min phone micro-quiz)

## 3. End-to-End Flight Recorder & Debuggability
- Every HTTP request must accept or generate an `X-Trace-Id` header passed across clients and services.
- Every AI and Solver response MUST include a `_debug_trace` dictionary/object containing:
  - `trace_id`
  - `llm_provider_used` (`ollama_local` | `gemini_fallback` | `deterministic_mock`)
  - `llm_model`
  - `llm_latency_ms`
  - `secrets_redacted_count`
  - `validation_retries_used`
  - `extracted_ast_summary`
  - `solver_status` (`OPTIMAL` | `FEASIBLE` | `OVERLOADED_SOLVED_WITH_SLACK`)
  - `solver_wall_time_ms`
  - `active_constraints_triggered`

## 4. Git Hygiene & Lockfile Policy
- **Never ignore lockfiles in `.gitignore`:** `package-lock.json`, `uv.lock`, and `gradle.lockfile` are committed for reproducible builds.
- Use `.cursorignore` and `.aiignore` to prevent AI agents from wasting token context on lockfiles, builds, and binary assets.
- Follow Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.
