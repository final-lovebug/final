"""배포에서 Parameter Store `/lovebug/llm/*`가 주는 **소문자** 환경변수를
키·모델 폴백 체인이 그대로 읽는지 확인한다.

`deploy/scripts/start_container.sh`는 파라미터 이름의 마지막 세그먼트를
그대로 `-e` 이름으로 넘긴다. 등록된 이름이 `gemini_api_key_1`이라
대문자로만 찾으면 컨테이너는 뜨지만 real 모드 호출이 전부
`GEMINI_API_KEY(_1)이 .env에 없다`로 죽는다 — 그 회귀를 막는 테스트다.
"""

from __future__ import annotations

import pytest

from app.pipeline.api_keys import getenv_ci, load_api_keys
from app.pipeline.model_chain import load_default_model_chain

_ALL = [f"{prefix}{suffix}" for prefix in ("GEMINI_API_KEY", "GEMINI_MODEL") for suffix in ("", "_1", "_2", "_3", "_4")]


@pytest.fixture(autouse=True)
def _clear_env(monkeypatch: pytest.MonkeyPatch) -> None:
    """대문자·소문자 양쪽 이름을 모두 지우고 시작한다 — 실행 환경의 `.env`가
    테스트 결과를 바꾸지 않도록."""
    for name in _ALL:
        monkeypatch.delenv(name, raising=False)
        monkeypatch.delenv(name.lower(), raising=False)


def test_소문자_키_이름을_읽는다(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("gemini_api_key_1", "k1")
    monkeypatch.setenv("gemini_api_key_2", "k2")

    assert load_api_keys() == ["k1", "k2"]


def test_소문자_모델_이름을_읽는다(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("gemini_model_1", "m1")
    monkeypatch.setenv("gemini_model_2", "m2")

    assert load_default_model_chain() == ["m1", "m2"]


def test_대문자_이름은_그대로_동작한다(monkeypatch: pytest.MonkeyPatch) -> None:
    """`.env`를 쓰는 로컬 실행 경로가 안 바뀌는지."""
    monkeypatch.setenv("GEMINI_API_KEY_1", "K1")
    monkeypatch.setenv("GEMINI_MODEL_1", "M1")

    assert load_api_keys() == ["K1"]
    assert load_default_model_chain() == ["M1"]


def test_단수_이름도_대소문자를_안_가린다(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("gemini_api_key", "single")
    monkeypatch.setenv("gemini_model", "single-model")

    assert load_api_keys() == ["single"]
    assert load_default_model_chain() == ["single-model"]


def test_정확히_일치하는_이름이_이긴다(monkeypatch: pytest.MonkeyPatch) -> None:
    """둘 다 있으면 호출부가 적은 이름 그대로가 우선한다."""
    monkeypatch.setenv("GEMINI_MODEL_1", "upper")
    monkeypatch.setenv("gemini_model_1", "lower")

    assert getenv_ci("GEMINI_MODEL_1") == "upper"


def test_키가_없으면_그대로_실패한다() -> None:
    with pytest.raises(RuntimeError):
        load_api_keys()

    assert load_default_model_chain() == []
