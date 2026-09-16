"""모델을 부르기 직전에 ``requestId``를 선점한다(``docs/AI_CONTRACT.md`` 7-2-2, `D-111`·`D-113`).

**왜 필요한가** — 요청 큐는 표준 큐이고 at-least-once 다. 같은 메시지가 두 번
배달되면 워커는 같은 작업으로 모델을 두 번 부른다. 백엔드의 콜백 멱등(`D-72`)은
**초안만** 하나로 접으므로 DB는 깨끗하고 요금만 두 배가 된다 — 어디에도 실패로
남지 않아 눈에 띄지 않는 종류의 낭비다.

**`mode=REAL`에만 건다.** ``STUB``·``MOCK``은 모델을 부르지 않으므로 선점할 것이
없고, 목 대역만 쓰는 로컬 개발이 Redis 없이 돌아야 한다.

**키를 지우지 않는다.** 작업이 끝나도 TTL 만료까지 남긴다 — 지우면 그 뒤에 도착한
재배달분이 다시 선점에 성공해 모델을 또 부른다. ``requestId``는 작업 하나에만
쓰이는 1회용 값이라(`D-70`) 키가 남아도 다음 작업을 막지 않는다.

**선점 여부를 모르면 부르지 않는다.** ``REDIS_URL``이 없거나 Redis 에 닿지 못하면
{class}`ClaimUnavailableError`를 올려 호출자가 그 작업을 실패로 끝내게 한다 —
조용히 건너뛰면 이 모듈이 있으나 마나다.
"""

from __future__ import annotations

import os

import redis

# 백엔드의 app.ai.timeout.job(기본 PT15M)과 맞춘다. 이보다 짧으면 백엔드의 타임아웃
# 스위퍼가 작업을 회수하기 전에 키가 풀려 중복 호출이 다시 열린다.
_TTL_SECONDS = 900
_KEY_PREFIX = "llm:request:"

# 선점한 쪽을 로그에서 가려내기 위한 값이다. 판정에는 쓰지 않는다 — 판정은 SET NX 의
# 성공 여부 하나로 끝난다.
_WORKER_ID = os.getenv("HOSTNAME") or f"pid-{os.getpid()}"

_client: redis.Redis | None = None


class ClaimUnavailableError(RuntimeError):
    """선점 여부를 확인하지 못했다. 이 경우 모델을 부르지 않는다."""


def claim_request(request_id: str) -> bool:
    """``request_id``를 선점한다. 이미 다른 배달분이 잡았으면 ``False``.

    :raises ClaimUnavailableError: ``REDIS_URL``이 없거나 Redis 에 닿지 못했다.
    """
    try:
        acquired = _redis().set(_KEY_PREFIX + request_id, _WORKER_ID, nx=True, ex=_TTL_SECONDS)
    except ClaimUnavailableError:
        raise
    except Exception as error:
        raise ClaimUnavailableError(f"선점 저장소에 접근하지 못했습니다: {error}") from error

    return bool(acquired)


def _redis() -> redis.Redis:
    """접속을 한 번만 만들어 재사용한다. 연결 자체는 redis-py 의 풀이 관리한다."""
    global _client

    if _client is None:
        url = os.getenv("REDIS_URL")
        if not url:
            raise ClaimUnavailableError("REDIS_URL 이 없어 중복 호출을 막을 수 없습니다.")
        _client = redis.Redis.from_url(url, socket_connect_timeout=3, socket_timeout=3)

    return _client


def reset_client_for_test() -> None:
    """테스트가 ``REDIS_URL``을 바꿔 끼울 수 있게 캐시된 접속을 버린다."""
    global _client

    _client = None
