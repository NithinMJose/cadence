from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings for Cadence AI and Solver service."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "cadence-ai-service"
    app_version: str = "0.1.0"
    environment: str = "development"

    # Ollama LLM
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "qwen2.5:7b-instruct-q4_K_M"

    # Google Gemini Fallback
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"

    # LLM Router Settings
    llm_provider_mode: str = "AUTO"  # AUTO | LOCAL_ONLY | GEMINI_ONLY | MOCK
    allow_cloud_fallback: bool = False

    # Security & Timezone
    cadence_api_key: str = "cadence-dev-secret-key-change-me"
    default_user_timezone: str = "Asia/Kolkata"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Return cached application settings."""
    return Settings()
