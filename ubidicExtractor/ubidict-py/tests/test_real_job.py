"""REAL 대역 — 입력 조립(DB 조회)과 출력 검증(콜백 규격)만 본다.

판정 로직(`run_extract`/`run_contrast`)과 DB는 전부 모킹한다. 여기서 확인하는 것은
**백엔드가 결과 전체를 거절할 만한 항목을 워커가 먼저 걸러내는지**다 — 후보어 하나가
규격을 어기면 `ExtractionResultValidator`가 스물아홉 건까지 같이 버린다.
"""

from __future__ import annotations

from unittest.mock import patch

import pytest

from app.queue_schema import LlmJobRequest
from app.real_job import RealJobError, run_real_check, run_real_extraction
from app.schema import (
    ContrastResponse,
    ContrastSuggestion,
    DictionaryEntry,
    DocumentInput,
    ExtractResponse,
    GroupCandidate,
    HomographCandidate,
    Occurrence,
    Sense,
    Usage,
)

_BODY = "우리 서비스의 유저는 결제할 수 있다."


def _extraction_job(**overrides) -> LlmJobRequest:
    fields = {
        "contractVersion": 1,
        "requestId": "req-1",
        "jobType": "TERM_EXTRACTION",
        "jobId": 30,
        "workspaceId": 1,
        "dictionaryId": None,
        "sourceDocumentIds": [10, 20],
        "documentId": None,
        "documentVersionNo": None,
        "mode": "REAL",
        "requestedAt": "2026-09-16T10:00:00+09:00",
    }
    return LlmJobRequest(**{**fields, **overrides})


def _check_job(**overrides) -> LlmJobRequest:
    fields = {
        "contractVersion": 1,
        "requestId": "req-2",
        "jobType": "DOCUMENT_CHECK",
        "jobId": 40,
        "workspaceId": 1,
        "dictionaryId": None,
        "sourceDocumentIds": None,
        "documentId": 10,
        "documentVersionNo": 3,
        "mode": "REAL",
        "requestedAt": "2026-09-16T10:00:00+09:00",
    }
    return LlmJobRequest(**{**fields, **overrides})


def _document(document_id: int = 10, content: str = _BODY) -> DocumentInput:
    return DocumentInput(documentId=str(document_id), title="결제 API 명세", department="", content=content)


def _usage() -> Usage:
    return Usage(model="test", inputTokens=0, outputTokens=0, llmCalls=1, elapsedMs=1)


def _occurrence(document_id: int, form: str = "결제", snippet: str = "회원은 결제한다.") -> Occurrence:
    return Occurrence(
        documentId=str(document_id), department="", form=form, snippet=snippet, charStart=4, charEnd=4 + len(form)
    )


def _group(
    *,
    candidate_id: str = "c-1",
    preferred: str = "결제",
    forms: list[str] | None = None,
    occurrence_count: int = 3,
    occurrences: list[Occurrence] | None = None,
    english: str | None = "Payment",
) -> GroupCandidate:
    return GroupCandidate(
        candidateId=candidate_id,
        kind="VARIANT",
        forms=forms if forms is not None else [preferred, "페이먼트"],
        proposedPreferredForm=preferred,
        proposedEnglishName=english,
        proposedDefinition="재화나 용역의 대가를 지급하는 행위",
        confidence=1.0,
        reason="정규화 결과가 같다.",
        occurrenceCount=occurrence_count,
        documentCount=1,
        occurrences=occurrences if occurrences is not None else [_occurrence(10)],
    )


def _extract_response(candidates: list) -> ExtractResponse:
    return ExtractResponse(
        jobId="30", status="SUCCESS", dictionaryVersionNo=0, candidates=candidates, usage=_usage(), warnings=[]
    )


def _contrast_response(suggestions: list[ContrastSuggestion]) -> ContrastResponse:
    return ContrastResponse(
        jobId="40", status="SUCCESS", dictionaryVersionNo=0, suggestions=suggestions, usage=_usage(), warnings=[]
    )


def _suggestion(*, origin: str, replacement: str, body: str = _BODY, term_id: str = "t-1") -> ContrastSuggestion:
    start = body.index(origin)
    return ContrastSuggestion(
        termId=term_id,
        preferredForm=replacement,
        foundForm=origin,
        documentId="10",
        department="",
        snippet=body,
        charStart=start,
        charEnd=start + len(origin),
        reason="맥락상 같은 개념이다.",
        method="llm",
    )


# ── 추출: 입력 조립 ───────────────────────────────────────────


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_existing_terms")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_reads_source_documents_and_delegates_to_the_pipeline(mock_documents, mock_terms, mock_extract):
    mock_extract.return_value = _extract_response([_group()])

    terms = run_real_extraction(_extraction_job())

    mock_documents.assert_called_once_with([10, 20])
    request = mock_extract.call_args.args[0]
    assert request.jobId == "30"
    assert request.workspaceId == "1"
    assert request.documents == [_document()]
    assert terms[0]["form"] == "결제"
    # 첫 회차 추출이면 dictionaryId 가 null 이라 조회할 사전집이 없다(5-3).
    mock_terms.assert_not_called()
    assert request.existingTerms == []


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_existing_terms", return_value=[])
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_reads_the_existing_dictionary_when_one_is_given(_mock_documents, mock_terms, mock_extract):
    mock_extract.return_value = _extract_response([])

    run_real_extraction(_extraction_job(dictionaryId=7))

    mock_terms.assert_called_once_with(7)


@patch("app.real_job.fetch_documents", return_value=[])
def test_extraction_fails_when_no_document_body_is_readable(_mock_documents):
    with pytest.raises(RealJobError) as error:
        run_real_extraction(_extraction_job())

    assert error.value.code == "DOCUMENT_NOT_FOUND"


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_continues_with_the_documents_it_could_read(_mock_documents, mock_extract):
    """삭제된 문서가 섞여 있어도 남은 문서로 추출한다 — 요청 전체를 실패시키지 않는다."""
    mock_extract.return_value = _extract_response([_group()])

    terms = run_real_extraction(_extraction_job())

    assert mock_extract.call_args.args[0].documents == [_document()]
    assert terms[0]["occurredDocumentIds"] == [10]


@patch("app.real_job.fetch_documents", side_effect=RuntimeError("connection refused"))
def test_extraction_fails_with_a_db_code_when_the_read_raises(_mock_documents):
    with pytest.raises(RealJobError) as error:
        run_real_extraction(_extraction_job())

    assert error.value.code == "BACKEND_DB_READ_FAILED"


@patch("app.real_job.run_extract", side_effect=RuntimeError("quota exhausted"))
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_fails_with_an_llm_code_when_the_pipeline_raises(_mock_documents, _mock_extract):
    with pytest.raises(RealJobError) as error:
        run_real_extraction(_extraction_job())

    assert error.value.code == "LLM_CALL_FAILED"
    assert "모델" in error.value.reason  # 사용자에게 그대로 보이는 문장이다(6-3)


# ── 추출: 출력 검증 ───────────────────────────────────────────


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_maps_a_group_candidate_to_the_callback_contract(_mock_documents, mock_extract):
    mock_extract.return_value = _extract_response([_group()])

    terms = run_real_extraction(_extraction_job())

    assert terms == [
        {
            "form": "결제",
            "proposedDefinition": "재화나 용역의 대가를 지급하는 행위",
            "proposedEnglishName": "Payment",
            "occurredDocumentIds": [10],
            "occurrenceCount": 3,
            "contextSnippets": ["회원은 결제한다."],
            "variantForms": ["결제", "페이먼트"],
        }
    ]


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_drops_candidates_that_never_occur_in_the_documents(_mock_documents, mock_extract):
    """occurrenceCount 가 0 인 후보어 하나만으로 백엔드는 결과 **전체**를 거절한다."""
    mock_extract.return_value = _extract_response([_group(occurrence_count=0), _group(candidate_id="c-2", preferred="주문")])

    terms = run_real_extraction(_extraction_job())

    assert [term["form"] for term in terms] == ["주문"]


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_drops_the_later_of_two_candidates_with_the_same_form(_mock_documents, mock_extract):
    mock_extract.return_value = _extract_response([_group(), _group(candidate_id="c-2")])

    terms = run_real_extraction(_extraction_job())

    assert [term["form"] for term in terms] == ["결제"]


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_drops_document_ids_outside_the_requested_set(_mock_documents, mock_extract):
    """요청 집합 밖의 id 가 섞이면 결과 전체가 거절된다(6-1)."""
    mock_extract.return_value = _extract_response(
        [_group(occurrences=[_occurrence(10), _occurrence(99), _occurrence(20), _occurrence(10)])]
    )

    terms = run_real_extraction(_extraction_job())

    assert terms[0]["occurredDocumentIds"] == [10, 20]


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_drops_forms_too_long_for_the_backend_column(_mock_documents, mock_extract):
    """form 은 varchar(200) 이다 — 자르면 표기가 달라지므로 버린다."""
    mock_extract.return_value = _extract_response([_group(preferred="가" * 201)])

    assert run_real_extraction(_extraction_job()) == []


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_drops_an_english_name_too_long_for_the_backend_column(_mock_documents, mock_extract):
    """제안 영문명은 버려도 후보어 자체는 쓸 수 있다 — 표기와 달리 본질이 아니다."""
    mock_extract.return_value = _extract_response([_group(english="A" * 201)])

    assert run_real_extraction(_extraction_job())[0]["proposedEnglishName"] is None


@patch("app.real_job.run_extract")
@patch("app.real_job.fetch_documents", return_value=[_document()])
def test_extraction_sends_a_homograph_as_one_term_listing_its_senses(_mock_documents, mock_extract):
    """갈래형은 뜻이 여럿인데 후보어엔 정의가 한 칸뿐이다 — 나열해 한 건으로 보낸다."""
    homograph = HomographCandidate(
        candidateId="c-1",
        kind="HOMOGRAPH",
        forms=["정산"],
        confidence=0.9,
        reason="두 뜻으로 쓰인다.",
        occurrenceCount=4,
        documentCount=2,
        senses=[
            Sense(label="대금 정산", definition="판매 대금을 나눠 지급하는 일", occurrences=[_occurrence(10, "정산")]),
            Sense(label="회계 정산", definition="기간 손익을 확정하는 일", occurrences=[_occurrence(20, "정산")]),
        ],
    )
    mock_extract.return_value = _extract_response([homograph])

    terms = run_real_extraction(_extraction_job())

    assert len(terms) == 1
    assert terms[0]["form"] == "정산"
    assert terms[0]["proposedDefinition"] == "대금 정산: 판매 대금을 나눠 지급하는 일 / 회계 정산: 기간 손익을 확정하는 일"
    assert terms[0]["occurredDocumentIds"] == [10, 20]
    assert terms[0]["proposedEnglishName"] is None


# ── 대조: 입력 조립 ───────────────────────────────────────────


@patch("app.real_job.run_contrast")
@patch("app.real_job.fetch_active_dictionary_entries")
@patch("app.real_job.fetch_document_version", return_value=_document())
def test_check_reads_the_requested_version_and_the_active_dictionary(mock_document, mock_dictionary, mock_contrast):
    entries = [DictionaryEntry(termId="t-1", preferredForm="이용자", englishName=None, definition="서비스를 쓰는 사람")]
    mock_dictionary.return_value = entries
    mock_contrast.return_value = _contrast_response([])

    run_real_check(_check_job())

    # 백엔드는 콜백에 실린 버전이 현재 버전과 다르면 결과를 버린다(5-4) — 지정된 버전을 읽어야 한다.
    mock_document.assert_called_once_with(10, 3)
    mock_dictionary.assert_called_once_with(1)
    request = mock_contrast.call_args.args[0]
    assert request.dictionary == entries
    assert request.documents == [_document()]


def test_check_fails_when_the_request_has_no_document_id():
    with pytest.raises(RealJobError) as error:
        run_real_check(_check_job(documentId=None))

    assert error.value.code == "INVALID_REQUEST"


@patch("app.real_job.fetch_active_dictionary_entries", return_value=[])
@patch("app.real_job.fetch_document_version", return_value=None)
def test_check_fails_when_the_body_is_missing(_mock_document, _mock_dictionary):
    with pytest.raises(RealJobError) as error:
        run_real_check(_check_job())

    assert error.value.code == "DOCUMENT_NOT_FOUND"


@patch("app.real_job.fetch_active_dictionary_entries", return_value=[])
@patch("app.real_job.fetch_document_version", return_value=_document())
def test_check_fails_when_the_workspace_has_no_active_dictionary(_mock_document, _mock_dictionary):
    """기준이 없으면 대조가 성립하지 않는다 — 빈 결과로 성공시키면 '고칠 게 없다'로 읽힌다."""
    with pytest.raises(RealJobError) as error:
        run_real_check(_check_job())

    assert error.value.code == "DICTIONARY_NOT_FOUND"


@patch("app.real_job.fetch_active_dictionary_entries", side_effect=RuntimeError("connection refused"))
@patch("app.real_job.fetch_document_version", return_value=_document())
def test_check_fails_with_a_db_code_when_the_read_raises(_mock_document, _mock_dictionary):
    with pytest.raises(RealJobError) as error:
        run_real_check(_check_job())

    assert error.value.code == "BACKEND_DB_READ_FAILED"


# ── 대조: 출력 검증 ───────────────────────────────────────────


def _run_check_with(suggestions: list[ContrastSuggestion], body: str = _BODY) -> list[dict]:
    entries = [DictionaryEntry(termId="t-1", preferredForm="이용자", englishName=None, definition="서비스를 쓰는 사람")]
    with (
        patch("app.real_job.fetch_document_version", return_value=_document(content=body)),
        patch("app.real_job.fetch_active_dictionary_entries", return_value=entries),
        patch("app.real_job.run_contrast", return_value=_contrast_response(suggestions)),
    ):
        return run_real_check(_check_job())


def test_check_maps_a_suggestion_to_a_utf16_anchor():
    result = _run_check_with([_suggestion(origin="유저", replacement="이용자")])

    assert result == [
        {"anchor": {"startOffset": 8, "endOffset": 10}, "originTerm": "유저", "suggestionTerm": "이용자"}
    ]


def test_check_anchor_counts_utf16_code_units_not_code_points():
    """이모지 한 자는 UTF-16 으로 2 유닛이다 — 코드포인트로 보내면 Java substring 이 밀린다."""
    body = "🙂 유저는 결제한다."

    result = _run_check_with([_suggestion(origin="유저", replacement="이용자", body=body)], body=body)

    anchor = result[0]["anchor"]
    assert body.encode("utf-16-le")[anchor["startOffset"] * 2 : anchor["endOffset"] * 2].decode("utf-16-le") == "유저"


def test_check_drops_a_suggestion_whose_anchor_does_not_match_the_body():
    """앵커가 한 건이라도 어긋나면 백엔드는 결과 전체를 거절한다(6-2)."""
    misplaced = _suggestion(origin="유저", replacement="이용자").model_copy(update={"charStart": 0, "charEnd": 2})

    assert _run_check_with([misplaced]) == []


def test_check_drops_a_second_suggestion_on_the_same_span():
    first = _suggestion(origin="유저", replacement="이용자")
    second = _suggestion(origin="유저", replacement="이용자", term_id="t-2")

    assert len(_run_check_with([first, second])) == 1


def test_check_drops_a_suggestion_term_too_long_for_the_backend_column():
    """origin_term·suggestion_term 은 varchar(255) 다."""
    body = "가" * 300
    long_match = _suggestion(origin=body, replacement="이용자", body=body)

    assert _run_check_with([long_match], body=body) == []
