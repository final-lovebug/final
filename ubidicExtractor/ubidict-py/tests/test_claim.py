"""app.claim 단위 테스트 — 실제 Redis 없이 redis.Redis.from_url 을 모킹한다."""

from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from app import claim
from app.claim import ClaimUnavailableError, claim_request


@pytest.fixture(autouse=True)
def _reset_client(monkeypatch):
    monkeypatch.setenv("REDIS_URL", "redis://localhost:6379/0")
    claim.reset_client_for_test()
    yield
    claim.reset_client_for_test()


@patch("app.claim.redis.Redis.from_url")
def test_claim_succeeds_when_key_is_new(mock_from_url):
    mock_from_url.return_value.set.return_value = True

    assert claim_request("req-1") is True


@patch("app.claim.redis.Redis.from_url")
def test_claim_uses_nx_and_ttl_matching_backend_job_timeout(mock_from_url):
    """SET NX EX 3000 이 아니면 선점이 아니거나 스위퍼보다 먼저 풀린다."""
    client = MagicMock()
    mock_from_url.return_value = client
    client.set.return_value = True

    claim_request("req-2")

    key, _value = client.set.call_args.args
    assert key == "llm:request:req-2"
    assert client.set.call_args.kwargs["nx"] is True
    assert client.set.call_args.kwargs["ex"] == 3000


@patch("app.claim.redis.Redis.from_url")
def test_claim_fails_when_another_delivery_already_holds_the_key(mock_from_url):
    """redis-py 는 NX 가 걸리면 None 을 준다 — 그것이 「이미 잡혔다」는 뜻이다."""
    mock_from_url.return_value.set.return_value = None

    assert claim_request("req-3") is False


@patch("app.claim.redis.Redis.from_url", side_effect=OSError("connection refused"))
def test_claim_raises_when_redis_is_unreachable(_mock_from_url):
    """닿지 못하면 「선점했다」고 볼 수 없다 — 조용히 통과시키지 않는다."""
    with pytest.raises(ClaimUnavailableError):
        claim_request("req-4")


def test_claim_raises_when_redis_url_is_missing(monkeypatch):
    monkeypatch.delenv("REDIS_URL", raising=False)
    claim.reset_client_for_test()

    with pytest.raises(ClaimUnavailableError):
        claim_request("req-5")


@patch("app.claim.redis.Redis.from_url")
def test_client_is_built_once_and_reused(mock_from_url):
    mock_from_url.return_value.set.return_value = True

    claim_request("req-6")
    claim_request("req-7")

    mock_from_url.assert_called_once()
