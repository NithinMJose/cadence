import os
import pytest
from httpx import ASGITransport, AsyncClient

# Enforce deterministic mock mode for offline CI tests
os.environ["LLM_PROVIDER_MODE"] = "MOCK"

from app.config import get_settings
get_settings.cache_clear()

from app.main import app


@pytest.fixture
async def async_client():
    """Async test client using httpx and ASGI transport."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client
