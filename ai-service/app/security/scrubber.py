import math
import re
from collections import Counter
from dataclasses import dataclass
from typing import List, Pattern, Tuple


@dataclass(frozen=True)
class ScrubResult:
    """Outcome of secret scrubbing on untrusted input text or code diffs."""

    scrubbed_text: str
    redacted_count: int
    redacted_categories: List[str]


class SecretScrubber:
    """Deterministic regex and Shannon entropy scanner that redacts credentials."""

    REDACTION_TOKEN = "[REDACTED_SECRET]"

    # Common credential and API key signatures
    _PATTERNS: List[Tuple[str, Pattern]] = [
        ("PRIVATE_KEY", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----[\s\S]*?-----END (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")),
        ("JWT_TOKEN", re.compile(r"eyJ[A-Za-z0-9-_]{10,}\.eyJ[A-Za-z0-9-_]{10,}\.[A-Za-z0-9-_.+/=]{10,}")),
        ("GITHUB_TOKEN", re.compile(r"(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{36,255}")),
        ("AWS_KEY", re.compile(r"(?:AKIA|ABIA|ACCA|ASIA)[0-9A-Z]{16}")),
        ("GEMINI_KEY", re.compile(r"AIzaSy[A-Za-z0-9-_]{33}")),
        ("OPENAI_KEY", re.compile(r"sk-(?:proj-)?[A-Za-z0-9_-]{32,}")),
        ("CONNECTION_STRING", re.compile(r"(?:postgres|postgresql|mysql|mongodb|redis):\/\/[^\s:]+:[^\s@]+@[^\s\/]+")),
        ("BEARER_AUTH", re.compile(r"(?i)bearer\s+[A-Za-z0-9\-._~+/]+=*")),
        ("ENV_ASSIGNMENT", re.compile(r"(?im)^(?:export\s+)?([A-Z0-9_]*(?:KEY|SECRET|PASSWORD|TOKEN|AUTH|CREDENTIAL)[A-Z0-9_]*\s*=\s*)([\"']?[^\r\n\"']+[\"']?)")),
    ]

    @staticmethod
    def calculate_shannon_entropy(token: str) -> float:
        """Calculate Shannon entropy in bits per character for a given string."""
        if not token:
            return 0.0
        length = len(token)
        counts = Counter(token)
        return -sum((count / length) * math.log2(count / length) for count in counts.values())

    def scrub(self, text: str) -> ScrubResult:
        """Scan and redact all credential patterns and high-entropy secret tokens."""
        if not text:
            return ScrubResult(scrubbed_text="", redacted_count=0, redacted_categories=[])

        scrubbed = text
        redacted_count = 0
        categories: List[str] = []

        # 1. Regex-based pattern matching
        for category, pattern in self._PATTERNS:
            if category == "ENV_ASSIGNMENT":
                def env_replacer(match):
                    nonlocal redacted_count
                    redacted_count += 1
                    return f"{match.group(1)}{self.REDACTION_TOKEN}"
                new_text = pattern.sub(env_replacer, scrubbed)
            else:
                matches = pattern.findall(scrubbed)
                if matches:
                    redacted_count += len(matches)
                    categories.append(category)
                    new_text = pattern.sub(self.REDACTION_TOKEN, scrubbed)
                else:
                    new_text = scrubbed
            scrubbed = new_text

        # 2. Shannon Entropy check for random high-entropy strings
        # Split by whitespace, punctuation, and delimiters while preserving lines
        raw_tokens = re.split(r"[\s=,;()\"'<>\[\]{}]+", scrubbed)
        for token in set(raw_tokens):
            clean = token.strip()
            # Must be a long token without underscores (ignore snake_case variables like DEFAULT_USER_TIMEZONE)
            if (
                len(clean) >= 24
                and clean != self.REDACTION_TOKEN
                and "_" not in clean
                and not clean.startswith("http")
                and not clean.startswith("app.")
            ):
                entropy = self.calculate_shannon_entropy(clean)
                is_hex = bool(re.fullmatch(r"[0-9a-fA-F]+", clean))
                is_secret = (is_hex and entropy >= 3.65) or (not is_hex and entropy >= 4.0)

                if is_secret:
                    scrubbed = scrubbed.replace(clean, self.REDACTION_TOKEN)
                    redacted_count += 1
                    if "HIGH_ENTROPY_TOKEN" not in categories:
                        categories.append("HIGH_ENTROPY_TOKEN")

        return ScrubResult(
            scrubbed_text=scrubbed,
            redacted_count=redacted_count,
            redacted_categories=categories,
        )
