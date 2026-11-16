import pytest
from httpx import ASGITransport, AsyncClient
from app.main import app
from app.models.feynman import FeynmanGradeRequest
from app.services.feynman_grader_service import FeynmanGraderService


@pytest.fixture
def feynman_service():
    return FeynmanGraderService()


@pytest.mark.asyncio
async def test_feynman_grade_all_checkpoints_covered(feynman_service):
    req = FeynmanGradeRequest(
        card_title="Transformer Attention & KV Caching",
        topic_tag="ai_eng",
        concept_summary="Understanding Key-Value caching dynamics, memory overhead per token.",
        rubric_checkpoints=[
            "1. Store Key-Value states per attention layer",
            "2. Memory scales linearly with context length and batch size",
            "3. Eliminates redundant recomputation of historical tokens",
            "4. Precision format FP16 uses 2 bytes per tensor element",
        ],
        transcript=(
            "In autoregressive generation, we store the key and value states per attention layer in a KV cache. "
            "This eliminates redundant recomputation of past tokens during decode steps. "
            "The memory overhead scales linearly with context length and batch size, "
            "where FP16 precision requires 2 bytes per element."
        ),
    )
    res = await feynman_service.grade_explanation(req, trace_id="trace-grade-4")

    assert res.fsrs_grade == 4
    assert len(res.matched_checkpoints) == 4
    assert len(res.missing_checkpoints) == 0
    assert "Flawless" in res.feedback or "precision" in res.feedback.lower()
    assert res.debug_trace.trace_id == "trace-grade-4"


@pytest.mark.asyncio
async def test_feynman_grade_partial_coverage(feynman_service):
    req = FeynmanGradeRequest(
        card_title="OR-Tools CP-SAT Interval Scheduling",
        topic_tag="solver",
        concept_summary="Formulating non-overlapping windows with interval variables.",
        rubric_checkpoints=[
            "1. Define start integer variable",
            "2. Define duration or size variable",
            "3. Define end integer variable enforcing start plus size equals end",
            "4. Add AddNoOverlap constraint across intervals",
        ],
        transcript="You define a start integer variable and a duration size variable for the intervals.",
    )
    res = await feynman_service.grade_explanation(req, trace_id="trace-grade-2")

    # Only start and duration were explained, missing end and AddNoOverlap
    assert res.fsrs_grade in (1, 2)
    assert len(res.missing_checkpoints) >= 2
    assert "missed" in res.feedback.lower()


@pytest.mark.asyncio
async def test_feynman_grade_empty_or_idk(feynman_service):
    req = FeynmanGradeRequest(
        card_title="Monotonic Stack Pattern",
        topic_tag="dsa",
        concept_summary="Maintaining an invariant of decreasing elements in O(N).",
        rubric_checkpoints=[
            "1. Maintain decreasing elements invariant",
            "2. Push indices to stack while current is less than top",
            "3. Pop elements when incoming element is strictly greater",
            "4. Time complexity is O(N) as each element pushed/popped once",
        ],
        transcript="I honestly don't know this concept yet.",
    )
    res = await feynman_service.grade_explanation(req)

    assert res.fsrs_grade == 1
    assert len(res.matched_checkpoints) == 0
    assert len(res.missing_checkpoints) == 4


@pytest.mark.asyncio
async def test_feynman_endpoint_success():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/api/v1/ai/feynman-grade",
            json={
                "card_title": "Cache Invalidation",
                "topic_tag": "sys_design",
                "concept_summary": "Cache aside vs write through",
                "rubric_checkpoints": [
                    "Point A: Cache aside pattern",
                    "Point B: Write through synchronization",
                    "Point C: TTL expiration boundary",
                    "Point D: Stampede prevention with mutex",
                ],
                "transcript": "With cache aside pattern, we check cache first and use TTL expiration boundary.",
            },
            headers={"X-Trace-Id": "trace-feynman-api"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert 1 <= data["fsrs_grade"] <= 4
        assert "_debug_trace" in data
        assert data["_debug_trace"]["trace_id"] == "trace-feynman-api"
