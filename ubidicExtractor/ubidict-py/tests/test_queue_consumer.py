"""Spring v1 SQS 요청을 HTTP 콜백으로 끝내는 워커 경로의 회귀 테스트."""

from __future__ import annotations

import json
from unittest.mock import MagicMock, patch

from app.queue_consumer import _handle_message

_QUEUE_URL = "https://sqs.example/lovebug-llm-request"


def _message(body: dict) -> dict:
    return {"Body": json.dumps(body), "ReceiptHandle": "rh-1"}


def _extraction_job(mode: str = "MOCK") -> dict:
    return {
        "contractVersion": 1,
        "requestId": "0d5c6f6e-0000-4000-8000-000000000001",
        "jobType": "TERM_EXTRACTION",
        "jobId": 30,
        "workspaceId": 1,
        "dictionaryId": None,
        "sourceDocumentIds": [10, 20],
        "documentId": None,
        "documentVersionNo": None,
        "mode": mode,
        "requestedAt": "2026-09-14T10:00:00+09:00",
    }


def _check_job(mode: str = "MOCK") -> dict:
    return {
        "contractVersion": 1,
        "requestId": "0d5c6f6e-0000-4000-8000-000000000002",
        "jobType": "DOCUMENT_CHECK",
        "jobId": 40,
        "workspaceId": 1,
        "dictionaryId": None,
        "sourceDocumentIds": None,
        "documentId": 10,
        "documentVersionNo": 3,
        "mode": mode,
        "requestedAt": "2026-09-14T10:00:00+09:00",
    }


def test_malformed_message_is_not_deleted():
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, {"Body": "not-json", "ReceiptHandle": "rh-1"})

    client.delete_message.assert_not_called()


@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_extraction_posts_fixed_result_and_deletes_message(mock_post_callback):
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_extraction_job()))

    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/extractions/30/result"
    assert body["requestId"] == "0d5c6f6e-0000-4000-8000-000000000001"
    assert body["sourceDocumentIds"] == [10, 20]
    assert body["terms"][0]["form"] == "결제"
    assert body["terms"][0]["occurredDocumentIds"] == [10]
    client.delete_message.assert_called_once_with(QueueUrl=_QUEUE_URL, ReceiptHandle="rh-1")


@patch("app.queue_consumer._post_callback", return_value=204)
def test_stub_extraction_posts_empty_terms_and_deletes_message(mock_post_callback):
    """STUB 은 MOCK 과 달리 빈 결과다 — 임시 데이터를 돌려주는 모드는 MOCK 하나뿐이다."""
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_extraction_job(mode="STUB")))

    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/extractions/30/result"
    assert body["terms"] == []
    client.delete_message.assert_called_once_with(QueueUrl=_QUEUE_URL, ReceiptHandle="rh-1")


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["이용자", "결제"])
@patch("app.queue_consumer.fetch_document_body", return_value="우리 서비스의 유저는 결제할 수 있다.")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_anchors_suggestions_on_the_real_body(mock_post_callback, mock_fetch_body, _mock_terms):
    """MOCK 대조는 본문을 읽어 실제로 있는 자리에만 앵커를 단다.

    백엔드의 CheckSuggestionValidator 가 body.substring(anchor) 와 originTerm 이 같은지
    보므로, 지어낸 오프셋은 예외 없이 거절된다.
    """
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    mock_fetch_body.assert_called_once_with(10, 3)
    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/checks/40/result"
    assert body["requestId"] == "0d5c6f6e-0000-4000-8000-000000000002"
    assert body["documentVersionNo"] == 3
    assert body["suggestions"] == [
        {
            "anchor": {"startOffset": 8, "endOffset": 10},
            "originTerm": "유저",
            "suggestionTerm": "이용자",
        }
    ]
    client.delete_message.assert_called_once_with(QueueUrl=_QUEUE_URL, ReceiptHandle="rh-1")


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["이용자"])
@patch("app.queue_consumer.fetch_document_body", return_value="우리 서비스의 유저는 결제할 수 있다.")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_anchor_matches_the_body_slice(_mock_post_callback, _mock_fetch_body, _mock_terms):
    """앵커가 가리키는 구간을 실제로 잘라 originTerm 과 같은지 본다 — 백엔드와 같은 판정이다."""
    body_text = "우리 서비스의 유저는 결제할 수 있다."
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    suggestion = _mock_post_callback.call_args.args[1]["suggestions"][0]
    anchor = suggestion["anchor"]
    assert body_text[anchor["startOffset"] : anchor["endOffset"]] == suggestion["originTerm"]


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["결제"])
@patch("app.queue_consumer.fetch_document_body", return_value="유저는 페이먼트를 진행한다.")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_skips_variants_whose_replacement_is_not_registered(mock_post_callback, _mock_body, _mock_terms):
    """사전집에 없는 대체 용어는 제안하지 않는다.

    백엔드는 적용 시점에 대체 용어가 활성 사전집의 표준어인지 본다
    (SuggestionTermProcessor.accept) — 지어내면 초안은 생기지만 「적용」이 막힌다.
    본문에 "유저"가 있어도 "이용자"가 등재돼 있지 않으면 건너뛰고, 등재된 "결제"만 제안한다.
    """
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    suggestions = mock_post_callback.call_args.args[1]["suggestions"]
    assert [s["suggestionTerm"] for s in suggestions] == ["결제"]
    assert suggestions[0]["originTerm"] == "페이먼트"


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=[])
@patch("app.queue_consumer.fetch_document_body", return_value="유저는 페이먼트를 진행한다.")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_returns_nothing_when_dictionary_has_no_terms(mock_post_callback, _mock_body, _mock_terms):
    """표준어가 하나도 없으면 적용 가능한 제안을 만들 수 없다 — 지어내지 않고 빈 결과로 끝낸다."""
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    assert mock_post_callback.call_args.args[1]["suggestions"] == []
    client.delete_message.assert_called_once_with(QueueUrl=_QUEUE_URL, ReceiptHandle="rh-1")


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["결제"])
@patch("app.queue_consumer.fetch_document_body", return_value="공백없는아주긴한덩어리본문입니다")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_fallback_token_is_capped(mock_post_callback, _mock_fetch_body, _mock_terms):
    """공백이 없는 본문이면 첫 낱말이 문단 전체다 — 상한까지만 집는다."""
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    suggestion = mock_post_callback.call_args.args[1]["suggestions"][0]
    assert suggestion["anchor"] == {"startOffset": 0, "endOffset": 10}
    assert suggestion["originTerm"] == "공백없는아주긴한덩어리본문입니다"[:10]


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["결제"])
@patch("app.queue_consumer.fetch_document_body", return_value="표준어만 담긴 본문")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_falls_back_to_the_first_token(mock_post_callback, _mock_fetch_body, _mock_terms):
    """고정 쌍이 하나도 안 걸려도 빈 결과를 내지 않는다 — 화면 확인이 목적이라서다."""
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    suggestion = mock_post_callback.call_args.args[1]["suggestions"][0]
    assert suggestion["anchor"] == {"startOffset": 0, "endOffset": 4}
    assert suggestion["originTerm"] == "표준어만"


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["결제"])
@patch("app.queue_consumer.fetch_document_body", return_value=None)
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_reports_failure_when_body_is_missing(mock_post_callback, _mock_fetch_body, _mock_terms):
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/checks/40/failure"
    assert body["code"] == "DOCUMENT_NOT_FOUND"
    client.delete_message.assert_called_once_with(QueueUrl=_QUEUE_URL, ReceiptHandle="rh-1")


@patch("app.queue_consumer.fetch_active_preferred_forms", return_value=["결제"])
@patch("app.queue_consumer.fetch_document_body", side_effect=RuntimeError("connection refused"))
@patch("app.queue_consumer._post_callback", return_value=204)
def test_mock_check_reports_failure_when_db_read_raises(mock_post_callback, _mock_fetch_body, _mock_terms):
    """DB 가 죽어 있어도 작업을 매달아 두지 않는다 — 실패 콜백으로 끝낸다."""
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job()))

    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/checks/40/failure"
    assert body["code"] == "MOCK_BODY_READ_FAILED"


@patch("app.queue_consumer.fetch_active_preferred_forms")
@patch("app.queue_consumer.fetch_document_body")
@patch("app.queue_consumer._post_callback", return_value=204)
def test_stub_check_posts_empty_suggestions_without_reading_the_body(mock_post_callback, mock_fetch_body, _mock_terms):
    """STUB 은 종전대로 빈 결과다 — 본문도 읽지 않는다."""
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_check_job(mode="STUB")))

    mock_fetch_body.assert_not_called()
    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/checks/40/result"
    assert body["suggestions"] == []


@patch("app.queue_consumer._post_callback", return_value=503)
def test_server_error_leaves_message_for_sqs_retry(_mock_post_callback):
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_extraction_job()))

    client.delete_message.assert_not_called()


@patch("app.queue_consumer._post_callback", return_value=403)
def test_client_error_deletes_message_without_retry(_mock_post_callback):
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_extraction_job()))

    client.delete_message.assert_called_once_with(QueueUrl=_QUEUE_URL, ReceiptHandle="rh-1")


@patch("app.queue_consumer._post_callback", return_value=204)
def test_real_mode_reports_failure_callback_until_real_worker_is_implemented(mock_post_callback):
    client = MagicMock()

    _handle_message(client, _QUEUE_URL, _message(_extraction_job(mode="REAL")))

    path, body = mock_post_callback.call_args.args
    assert path == "/api/internal/llm/extractions/30/failure"
    assert body["code"] == "REAL_MODE_NOT_IMPLEMENTED"
    client.delete_message.assert_called_once()
