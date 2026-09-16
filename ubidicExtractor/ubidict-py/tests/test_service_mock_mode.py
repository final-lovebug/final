"""`mode="mock"` 이 DB·Gemini 없이 규격을 충족하는 임시 데이터를 돌려주는지 본다.

`stub` 과 갈리는 지점이 핵심이다 — `stub` 은 빈 결과라 「성공했지만 아무것도 안
생김」과 실패를 구분할 수 없고, `mock` 은 화면·저장까지 데이터가 흐르는지 확인하려고
쓴다. 그래서 두 모드 모두 `fetch_*` 를 부르지 않는다는 것까지 함께 검증한다.
"""

from __future__ import annotations

from unittest.mock import patch

from app.job_schema import ContrastJobRequest, ExtractJobRequest
from app.service import run_contrast_job, run_extract_job


def _extract_job(mode: str) -> ExtractJobRequest:
    return ExtractJobRequest(
        jobId="job-1",
        workspaceId=1,
        dictionaryVersionNo=3,
        dictionaryId=7,
        documentIds=[10, 20],
        accessToken="token",
        mode=mode,
    )


def _contrast_job(mode: str) -> ContrastJobRequest:
    return ContrastJobRequest(
        jobId="job-2",
        workspaceId=1,
        dictionaryVersionNo=3,
        dictionaryId=7,
        documentIds=[10],
        accessToken="token",
        mode=mode,
    )


@patch("app.service.sleep_mock_delay", return_value=1500)
def test_extract_mock_returns_candidate_without_db_or_llm(mock_delay):
    with (
        patch("app.service.fetch_documents") as documents,
        patch("app.service.fetch_existing_terms") as terms,
    ):
        response = run_extract_job(_extract_job("mock"))

    documents.assert_not_called()
    terms.assert_not_called()

    assert response.status == "SUCCESS"
    assert response.jobId == "job-1"
    assert response.dictionaryVersionNo == 3
    assert len(response.candidates) == 1

    candidate = response.candidates[0]
    assert candidate.kind == "VARIANT"
    assert candidate.proposedPreferredForm == "결제"
    assert candidate.proposedDefinition
    # 유래 문서는 요청에 실려온 첫 문서다 — 지어낸 id 를 쓰지 않는다.
    assert candidate.occurrences[0].documentId == "10"
    assert response.usage.llmCalls == 0
    assert response.usage.elapsedMs == 1500
    mock_delay.assert_called_once_with()


@patch("app.service.sleep_mock_delay", return_value=1500)
def test_extract_mock_occurrence_offsets_match_its_own_snippet(_mock_delay):
    """anchor 를 쓰는 소비자가 있으므로 오프셋이 snippet 과 맞아야 한다."""
    response = run_extract_job(_extract_job("mock"))

    occurrence = response.candidates[0].occurrences[0]
    assert occurrence.snippet[occurrence.charStart : occurrence.charEnd] == occurrence.form


def test_extract_stub_still_returns_empty():
    """mock 을 더해도 stub 의 계약(빈 결과)은 그대로다."""
    response = run_extract_job(_extract_job("stub"))

    assert response.status == "SUCCESS"
    assert response.candidates == []


@patch("app.service.sleep_mock_delay", return_value=1500)
def test_contrast_mock_returns_suggestion_without_db_or_llm(mock_delay):
    with (
        patch("app.service.fetch_documents") as documents,
        patch("app.service.fetch_dictionary_entries") as dictionary,
    ):
        response = run_contrast_job(_contrast_job("mock"))

    documents.assert_not_called()
    dictionary.assert_not_called()

    assert response.status == "SUCCESS"
    assert len(response.suggestions) == 1

    suggestion = response.suggestions[0]
    assert suggestion.preferredForm == "결제"
    assert suggestion.foundForm == "페이먼트"
    assert suggestion.documentId == "10"
    assert suggestion.snippet[suggestion.charStart : suggestion.charEnd] == suggestion.foundForm
    assert response.usage.elapsedMs == 1500
    mock_delay.assert_called_once_with()


def test_contrast_stub_still_returns_empty():
    response = run_contrast_job(_contrast_job("stub"))

    assert response.suggestions == []
