# Cadence AI & Solver Service

Stateless Python 3.12 FastAPI microservice handling LLM intent parsing, secret scrubbing, commit autopsy, and CP-SAT constraint scheduling.

## Local Execution
```bash
uv run uvicorn app.main:app --reload --port 8000
```

## Running Tests
```bash
uv run pytest -v
```
