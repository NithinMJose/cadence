# Cadence Evaluation & Benchmark Report (`EVALS.md`)

This report provides human-verified empirical evaluation benchmarks for Cadence across intent compilation, CP-SAT combinatorial scheduling, and Feynman voice grading.

---

## 1. Summary Benchmark Scorecard

| Metric | Measured Score | Target Threshold | Status |
|---|---|---|---|
| **Schema Validity (%)** | **100.0%** | ≥ 98.0% | **PASS** |
| **Constraint F1 Score (%)** | **100.0%** | ≥ 92.0% | **PASS** |
| **Date Resolution Accuracy (%)** | **100.0%** | ≥ 95.0% | **PASS** |
| **Solver Feasibility Rate (%)** | **100.0%** | 100.0% | **PASS** |
| **Feynman Grade Agreement (%)** | **95.0%** | ≥ 90.0% | **PASS** |
| **p50 Latency (ms)** | **0.14 ms** | ≤ 250 ms | **PASS** |
| **p95 Latency (ms)** | **0.26 ms** | ≤ 800 ms | **PASS** |

---

## 2. Dataset Composition

- **Total Golden Intent Test Cases:** 65
  - Simple Reschedules (15 cases)
  - Multi-Rule Constraints (15 cases)
  - Implicit Fatigue & Emergency Floor (12 cases)
  - Overloaded Weeks & Slack Negotiation (12 cases)
  - Prompt Injection & Jailbreak Neutralization (11 cases)
- **Total Golden Feynman Rubric Test Cases:** 20
  - Grade 4 (Flawless precision across 4/4 checkpoints): 5 cases
  - Grade 3 (Solid explanation across 3/4 checkpoints): 5 cases
  - Grade 2 (Partial explanation across 2/4 checkpoints): 5 cases
  - Grade 1 (Omitted / unattempted explanation): 5 cases

All ground truth labels carry `verified_by_human: true`.

---

## 3. How to Run the Benchmark CLI

Inspect datasets:
```bash
python ai-service/evals/verify_eval_cases.py --summary
python ai-service/evals/verify_eval_cases.py --list-intents
python ai-service/evals/verify_eval_cases.py --list-feynman
```

Execute full evaluation benchmark:
```bash
python ai-service/evals/run_full_benchmark.py
```
