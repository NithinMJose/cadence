import pytest
from pydantic import ValidationError
from app.llm.validator import SelfHealingValidator
from app.models.ast import ConstraintMutationAST


@pytest.mark.asyncio
async def test_validator_succeeds_on_valid_json():
    validator = SelfHealingValidator()
    valid_payload = """
    {
      "is_ambiguous": false,
      "constraints": [
        {
          "action": "BLOCK",
          "day_offset": 0,
          "window": "EVENING",
          "reason": "Dentist",
          "is_hard_constraint": true
        }
      ],
      "summary": "Block today evening"
    }
    """
    async def dummy_retry(prompt: str) -> str:
        return "{}"

    ast, retries = await validator.validate_with_retry(valid_payload, dummy_retry)
    assert retries == 0
    assert ast.is_ambiguous is False
    assert len(ast.constraints) == 1
    assert ast.constraints[0].reason == "Dentist"


@pytest.mark.asyncio
async def test_validator_self_heals_on_malformed_json():
    validator = SelfHealingValidator()
    # First attempt: invalid trailing comma / broken json
    bad_payload = "{ broken json: true "

    valid_payload = """
    {
      "is_ambiguous": false,
      "constraints": [],
      "summary": "Recovered valid schema"
    }
    """

    async def mock_correction_retry(correction_prompt: str) -> str:
        assert "invalid JSON" in correction_prompt
        return valid_payload

    ast, retries = await validator.validate_with_retry(bad_payload, mock_correction_retry)
    assert retries == 1
    assert ast.summary == "Recovered valid schema"


@pytest.mark.asyncio
async def test_validator_fails_after_max_retries():
    validator = SelfHealingValidator()
    persistently_bad = "not json at all"

    async def fail_retry(prompt: str) -> str:
        return "still not json"

    with pytest.raises(Exception):
        await validator.validate_with_retry(persistently_bad, fail_retry)
