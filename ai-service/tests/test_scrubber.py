import secrets
from app.security.scrubber import SecretScrubber


def test_scrubber_redacts_api_keys():
    scrubber = SecretScrubber()
    text = (
        "Here is my OpenAI key sk-proj-1234567890abcdef1234567890abcdef12345678 "
        "and my GitHub token ghp_1234567890abcdef1234567890abcdef1234."
    )
    result = scrubber.scrub(text)
    assert "[REDACTED_SECRET]" in result.scrubbed_text
    assert "sk-proj" not in result.scrubbed_text
    assert "ghp_" not in result.scrubbed_text
    assert result.redacted_count >= 2


def test_scrubber_redacts_env_lines():
    scrubber = SecretScrubber()
    text = (
        "POSTGRES_PASSWORD=cadence_ultra_secret_123\n"
        "CADENCE_API_KEY=my-super-secret-key-prod\n"
        "DEFAULT_USER_TIMEZONE=Asia/Kolkata"
    )
    result = scrubber.scrub(text)
    assert "POSTGRES_PASSWORD=[REDACTED_SECRET]" in result.scrubbed_text
    assert "CADENCE_API_KEY=[REDACTED_SECRET]" in result.scrubbed_text
    assert "DEFAULT_USER_TIMEZONE=Asia/Kolkata" in result.scrubbed_text


def test_scrubber_redacts_bearer_token():
    scrubber = SecretScrubber()
    text = "Use Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThis"
    result = scrubber.scrub(text)
    assert "[REDACTED_SECRET]" in result.scrubbed_text
    assert "doNotLeakThis" not in result.scrubbed_text
    assert result.redacted_count >= 1


def test_scrubber_redacts_high_entropy_token():
    scrubber = SecretScrubber()
    # 32-character random hex token
    high_entropy = secrets.token_hex(20)
    text = f"Token is {high_entropy} for auth."
    result = scrubber.scrub(text)
    assert "[REDACTED_SECRET]" in result.scrubbed_text
    assert high_entropy not in result.scrubbed_text
    assert "HIGH_ENTROPY_TOKEN" in result.redacted_categories


def test_scrubber_preserves_clean_text():
    scrubber = SecretScrubber()
    clean_text = "I have a dentist appointment on Friday evening at 6pm."
    result = scrubber.scrub(clean_text)
    assert result.scrubbed_text == clean_text
    assert result.redacted_count == 0
    assert len(result.redacted_categories) == 0
