"""SQS 요청 큐를 롱폴링으로 소비하고 Spring에 HTTP 콜백한다.

Spring 계약은 응답 큐를 쓰지 않는다. 워커는 요청 메시지의 ``requestId``를
그대로 본문에 넣어 ``/api/internal/llm/**``으로 콜백하고, 2xx·4xx면 메시지를
삭제하며 5xx·네트워크 오류만 SQS 재시도에 맡긴다.

`SQS_REQUEST_QUEUE_URL`이 `.env`에 없으면 컨슈머를 아예 시작하지 않는다
— 로컬에서 HTTP 엔드포인트(`/extract`·`/contrast`)만으로 개발·테스트할 때
AWS 자격증명이 없어도 앱이 뜨게 하기 위해서다.

`SQS_ENDPOINT_URL`을 `.env`에 채우면 실제 AWS 대신 그 주소로 접속한다 —
`docker-compose.local.yml`의 LocalStack(`http://localhost:4566`)을 가리키면
실제 AWS·백엔드 없이 로컬에서 전체 흐름을 확인할 수 있다(`scripts/`
참고). 비워두면 평소처럼 실제 AWS SQS에 접속한다.
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
import re
from urllib.error import HTTPError
from urllib.request import Request, urlopen

import boto3

from app.backend_db import fetch_active_preferred_forms, fetch_document_body
from app.queue_schema import LlmJobRequest

logger = logging.getLogger("queue_consumer")

_POLL_WAIT_SECONDS = 20  # SQS 롱폴링 최대치
_running = False

# 대조 MOCK 이 본문에서 찾아 볼 비표준 표기. 왼쪽이 본문에 있고 오른쪽이 **활성 사전집의
# 표준어일 때만** 제안한다 — 사전집에 없는 말을 제안하면 초안은 만들어져도 「적용」이
# DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM 으로 막힌다(SuggestionTermProcessor.accept).
_MOCK_VARIANTS: tuple[tuple[str, str], ...] = (
    ("유저", "이용자"),
    ("고객", "회원"),
    ("페이먼트", "결제"),
    ("어드민", "관리자"),
    ("오더", "주문"),
)
_MOCK_SUGGESTION_LIMIT = 3
# 폴백이 집는 낱말의 상한. 공백 없는 본문이면 첫 낱말이 문단 전체가 돼 하이라이트가
# 본문을 통째로 덮는다. 잘라도 본문의 연속 구간이라 앵커는 그대로 유효하다.
_MOCK_FALLBACK_MAX_CHARS = 10


def build_sqs_client():
    """`SQS_ENDPOINT_URL`이 있으면 그쪽으로(LocalStack 등), 없으면 실제 AWS로."""
    kwargs: dict = {"region_name": os.getenv("AWS_REGION")}
    endpoint_url = os.getenv("SQS_ENDPOINT_URL")
    if endpoint_url:
        kwargs["endpoint_url"] = endpoint_url
    return boto3.client("sqs", **kwargs)


async def start_consumer_loop() -> None:
    """FastAPI `lifespan`에서 백그라운드 태스크로 띄운다(`main.py`)."""
    global _running

    queue_url = os.getenv("SQS_REQUEST_QUEUE_URL")
    if not queue_url:
        logger.warning("SQS_REQUEST_QUEUE_URL이 없어 큐 컨슈머를 시작하지 않는다(로컬 개발 모드로 간주).")
        return

    _running = True
    client = build_sqs_client()
    loop = asyncio.get_event_loop()

    logger.info(f"SQS 컨슈머 시작: {queue_url}")
    while _running:
        try:
            response = await loop.run_in_executor(
                None,
                lambda: client.receive_message(
                    QueueUrl=queue_url,
                    MaxNumberOfMessages=1,
                    WaitTimeSeconds=_POLL_WAIT_SECONDS,
                ),
            )
        except Exception:
            logger.exception("SQS receive_message 실패 — 5초 후 재시도")
            await asyncio.sleep(5)
            continue

        for message in response.get("Messages", []):
            # 블로킹 처리(Gemini 호출·MySQL 접속 포함)라 스레드 풀에서 돌린다
            # — FastAPI 이벤트 루프를 막지 않기 위해서다.
            await loop.run_in_executor(None, _handle_message, client, queue_url, message)


def stop_consumer_loop() -> None:
    global _running
    _running = False


def _handle_message(client, queue_url: str, message: dict) -> None:
    receipt_handle = message["ReceiptHandle"]

    try:
        job = LlmJobRequest.model_validate_json(message["Body"])
    except Exception:
        logger.exception("메시지 파싱 실패 — 형식이 잘못됐다. 삭제하지 않고 DLQ 정책에 맡긴다.")
        return

    try:
        callback_path, body = _callback_for(job)
        status = _post_callback(callback_path, body)
    except Exception:
        logger.exception("jobId=%s 콜백 전송 실패 — 메시지를 삭제하지 않는다", job.jobId)
        return

    if status < 500:
        client.delete_message(QueueUrl=queue_url, ReceiptHandle=receipt_handle)
        logger.info("jobId=%s callback completed. status=%s", job.jobId, status)
    else:
        logger.warning("jobId=%s callback returned %s — SQS 재시도를 위해 메시지를 남긴다", job.jobId, status)


def _callback_for(job: LlmJobRequest) -> tuple[str, dict]:
    """요청 모드와 작업 종류에 맞는 Spring 콜백 경로·본문을 만든다."""
    if job.mode == "REAL":
        return _failure_callback(job, "REAL 모드는 아직 이 워커에 구현되지 않았습니다.", "REAL_MODE_NOT_IMPLEMENTED")

    if job.jobType == "TERM_EXTRACTION":
        if not job.sourceDocumentIds:
            return _failure_callback(job, "용어 추출 요청에 sourceDocumentIds가 없습니다.", "INVALID_REQUEST")
        # REAL 은 위에서 실패로 끊었으므로 남은 값은 STUB·MOCK 뿐이다. 그래도 else 로
        # 뭉치지 않는다 — 모드가 하나 늘면 조용히 mock 데이터를 돌려주게 된다.
        terms = [_mock_term(job.sourceDocumentIds[0])] if job.mode == "MOCK" else []
        return (
            f"/api/internal/llm/extractions/{job.jobId}/result",
            {"requestId": job.requestId, "sourceDocumentIds": job.sourceDocumentIds, "terms": terms},
        )

    # 문서 대조의 MOCK 은 **본문을 읽어야** 목 데이터를 만들 수 있다. 백엔드의
    # CheckSuggestionValidator 가 body.substring(anchor) 와 originTerm 이 같은지 보므로
    # (docs/AI_CONTRACT.md 6-2), 본문을 모르고 지어낸 anchor 는 예외 없이
    # DRAFT_DOCUMENT_CHECK_INVALID_RESULT 로 거절된다. 그래서 추출 MOCK 과 달리 읽기
    # 전용 DB 조회 한 번을 한다 — 모델은 여전히 부르지 않는다.
    if job.mode == "MOCK":
        try:
            body = fetch_document_body(job.documentId, job.documentVersionNo)
        except Exception:
            logger.exception("jobId=%s 목 대조 결과를 만들 본문을 읽지 못했다", job.jobId)
            return _failure_callback(job, "목 대조 결과를 만들 문서 본문을 읽지 못했습니다.", "MOCK_BODY_READ_FAILED")
        if body is None:
            return _failure_callback(job, "대조할 문서 본문을 찾지 못했습니다.", "DOCUMENT_NOT_FOUND")
        try:
            preferred_forms = fetch_active_preferred_forms(job.workspaceId)
        except Exception:
            logger.exception("jobId=%s 활성 사전집의 표준어를 읽지 못했다", job.jobId)
            return _failure_callback(job, "목 대조 결과를 만들 사전집을 읽지 못했습니다.", "MOCK_TERMS_READ_FAILED")
        suggestions = _mock_suggestions(body, preferred_forms)
    else:
        suggestions = []

    return (
        f"/api/internal/llm/checks/{job.jobId}/result",
        {"requestId": job.requestId, "documentVersionNo": job.documentVersionNo, "suggestions": suggestions},
    )


def _mock_suggestions(body: str, preferred_forms: list[str]) -> list[dict]:
    """본문에 실제로 있는 자리에만 앵커를 달고, 대체 용어는 **활성 사전집의 표준어**만 쓴다.

    두 조건 모두 백엔드가 강제한다 — 앵커는 대조 콜백에서(``CheckSuggestionValidator``),
    대체 용어는 「적용」에서(``SuggestionTermProcessor.accept``) 본다. 한쪽만 맞추면 초안은
    만들어지지만 사용자가 적용을 누르는 순간 막힌다.
    """
    if not preferred_forms:
        # 표준어가 없으면 적용 가능한 제안을 만들 수 없다. 지어내지 않고 빈 결과로 끝낸다.
        return []

    available = set(preferred_forms)
    suggestions: list[dict] = []
    for origin, replacement in _MOCK_VARIANTS:
        if replacement not in available:
            continue
        index = body.find(origin)
        if index == -1:
            continue
        suggestions.append(_suggestion(body, index, origin, replacement))
        if len(suggestions) == _MOCK_SUGGESTION_LIMIT:
            return suggestions
    if suggestions:
        return suggestions

    # 알고 있는 비표준 표기가 본문에 하나도 없으면, 첫 낱말을 아무 표준어로 바꿔 보게 둔다.
    # 화면이 하이라이트와 적용을 실제로 도는지 보려는 것이 목적이라 빈 결과보다 낫다.
    replacement = sorted(available)[0]
    for match in re.finditer(r"\S+", body):
        token = match.group()[:_MOCK_FALLBACK_MAX_CHARS]
        if token != replacement:
            return [_suggestion(body, match.start(), token, replacement)]
    return []


def _suggestion(body: str, index: int, origin: str, replacement: str) -> dict:
    start = _utf16_length(body[:index])
    return {
        "anchor": {"startOffset": start, "endOffset": start + _utf16_length(origin)},
        "originTerm": origin,
        "suggestionTerm": replacement,
    }


def _utf16_length(text: str) -> int:
    """Java String.substring 이 세는 UTF-16 코드 유닛 길이.

    파이썬 문자열 인덱스는 코드 포인트라 한글·영문에서는 같은 값이지만, 이모지 같은
    BMP 밖 문자가 본문에 있으면 한 글자가 2 유닛이라 어긋난다 — 앵커가 한 칸씩 밀려
    검증에서 떨어지므로 여기서 맞춰 센다.
    """
    return len(text.encode("utf-16-le")) // 2


def _mock_term(document_id: int) -> dict:
    """개발 환경에서만 쓰는, Spring 검증 규격을 충족하는 고정 추출 결과다."""
    return {
        "form": "결제",
        "proposedDefinition": "재화나 용역의 대가를 지급하는 행위",
        "proposedEnglishName": "Payment",
        "occurredDocumentIds": [document_id],
        "occurrenceCount": 1,
        "contextSnippets": ["mock 용어 추출 결과입니다."],
        "variantForms": ["결제", "페이먼트"],
    }


def _failure_callback(job: LlmJobRequest, reason: str, code: str) -> tuple[str, dict]:
    resource = "extractions" if job.jobType == "TERM_EXTRACTION" else "checks"
    return (
        f"/api/internal/llm/{resource}/{job.jobId}/failure",
        {"requestId": job.requestId, "reason": reason, "code": code},
    )


def _post_callback(path: str, body: dict) -> int:
    """동기 HTTP 콜백의 상태 코드를 돌려준다. HTTP 오류도 재시도 정책 판단에 쓴다."""
    base_url = os.getenv("BACKEND_CALLBACK_BASE_URL", "http://localhost:8080").rstrip("/")
    request = Request(
        f"{base_url}{path}",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=10) as response:
            return response.status
    except HTTPError as error:
        return error.code
