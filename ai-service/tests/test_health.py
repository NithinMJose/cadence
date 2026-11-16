import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_health_check_returns_up(async_client: AsyncClient):
    """Verify /health returns UP status and valid system metadata."""
    response = await async_client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "cadence-ai-service"
    assert "timestamp" in data
    assert "timezone" in data
    assert "trace_id" in data
    assert "X-Trace-Id" in response.headers
    assert "X-Response-Time-Ms" in response.headers


@pytest.mark.asyncio
async def test_trace_id_propagation(async_client: AsyncClient):
    """Verify incoming X-Trace-Id header is preserved across the pipeline."""
    custom_trace = "custom-trace-header-12345"
    response = await async_client.get(
        "/health", headers={"X-Trace-Id": custom_trace}
    )
    assert response.status_code == 200
    assert response.headers.get("X-Trace-Id") == custom_trace
    assert response.json()["trace_id"] == custom_trace
