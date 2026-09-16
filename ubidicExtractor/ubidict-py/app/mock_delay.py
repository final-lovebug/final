"""Deterministic test seam for simulated external-model latency."""

from __future__ import annotations

import random
import time

MOCK_DELAY_MIN_SECONDS = 1.0
MOCK_DELAY_MAX_SECONDS = 3.0


def sleep_mock_delay() -> int:
    """Wait 1–3 seconds and return the selected delay in milliseconds."""
    seconds = random.uniform(MOCK_DELAY_MIN_SECONDS, MOCK_DELAY_MAX_SECONDS)
    time.sleep(seconds)
    return round(seconds * 1000)
