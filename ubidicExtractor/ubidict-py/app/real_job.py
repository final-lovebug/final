"""REAL 대역 — 실제로 백엔드 DB를 읽고 모델을 불러 Spring 콜백 본문을 만든다.

판정 로직은 하나도 새로 쓰지 않는다. `app/service.py`의 `run_extract`/`run_contrast`를
그대로 부른다 — HTTP(`/extract`·`/jobs/extract`)로 들어오든 SQS로 들어오든 같은 함수를
쓴다는 원칙(`app/main.py` 모듈 docstring)을 REAL 대역에도 그대로 적용한 것이다.

이 모듈이 하는 일은 그 **앞뒤** 두 가지다.

1. **입력 조립** — SQS 메시지에는 식별자만 있다(`docs/AI_CONTRACT.md` 5절, `D-69`).
   추출은 `sourceDocumentIds`의 현재 발행 버전을, 대조는 `documentVersionNo`가 지정한
   버전을 읽는다. 대조의 기준 사전집은 메시지에 없다 — `dictionaryId`는
   `DOCUMENT_CHECK`에서 항상 `null`이라(`LlmJobRequest.documentCheck`) `workspaceId`의
   활성 사전집을 직접 찾는다.
2. **출력 검증** — 콜백 본문이 계약(6-1·6-2)을 어기면 백엔드는 그 한 건만 버리는 게
   아니라 **결과 전체**를 409로 거절하고 작업을 실패로 끝낸다
   (`ExtractionResultValidator`·`CheckSuggestionValidator`). 그래서 파이프라인 결과를
   그대로 싣지 않고, 규격을 못 채우는 항목은 여기서 **버리고** 나머지를 보낸다 — 한 건
   때문에 스물아홉 건을 잃지 않기 위해서다. 버린 건은 `requestId`와 함께 경고로 남긴다.

실패는 `RealJobError`로 올린다. 호출부(`app/queue_consumer.py`)가 그걸 실패
콜백(6-3)으로 바꾼다 — `reason`은 **사용자 화면에 그대로 보이는 문장**이므로 모델 원문도
스택도 담지 않는다.
"""

from __future__ import annotations

import logging

from app.anchor import anchor_for
from app.backend_db import (
    fetch_active_dictionary_entries,
    fetch_document_version,
    fetch_documents,
    fetch_existing_terms,
)
from app.queue_schema import LlmJobRequest
from app.schema import Candidate, ContrastRequest, ContrastSuggestion, ExtractRequest, HomographCandidate
from app.service import run_contrast, run_extract

logger = logging.getLogger("real_job")

# 백엔드 컬럼 상한(`V1__init_schema.sql`). 넘기면 콜백 자체는 통과하고 백엔드가 저장하다
# 500으로 죽는다 — 검증기보다 뒤에서 터지므로 여기서 미리 걸러야 한다.
_MAX_FORM_CHARS = 200  # candidate_term.form · candidate_term_variant_form.variant_form
_MAX_ENGLISH_CHARS = 200  # candidate_term.proposed_english_name
_MAX_SNIPPET_CHARS = 1000  # candidate_term_context_snippet.snippet
_MAX_SUGGESTION_TERM_CHARS = 255  # suggestion_term.origin_term · suggestion_term

# 메시지에 사전집 버전이 없다(5절). 판정에 쓰이지 않고 `ExtractResponse`/`ContrastResponse`에
# 그대로 실릴 뿐이며, 콜백 본문(6-1·6-2)에는 이 필드가 아예 없다 — 지어내지 않고 0을 둔다.
_UNKNOWN_DICTIONARY_VERSION = 0


class RealJobError(RuntimeError):
    """작업을 실패로 끝내야 하는 사유. `reason`은 사용자에게 그대로 보인다(6-3)."""

    def __init__(self, reason: str, code: str) -> None:
        super().__init__(reason)
        self.reason = reason
        self.code = code


def run_real_extraction(job: LlmJobRequest) -> list[dict]:
    """추출 콜백(6-1)의 `terms` 배열을 만든다."""
    source_ids = list(job.sourceDocumentIds or [])

    try:
        documents = fetch_documents(source_ids)
        # 첫 회차 추출이면 `dictionaryId`가 `null`이다(5-3) — 갱신할 사전집이 아직 없다.
        existing_terms = fetch_existing_terms(job.dictionaryId) if job.dictionaryId else []
    except Exception as error:
        logger.exception("requestId=%s 추출 입력을 읽지 못했다", job.requestId)
        raise RealJobError("문서와 사전집을 읽지 못했습니다.", "BACKEND_DB_READ_FAILED") from error

    if not documents:
        raise RealJobError("추출할 문서 본문을 찾지 못했습니다.", "DOCUMENT_NOT_FOUND")
    if len(documents) < len(source_ids):
        # 삭제됐거나 없는 문서는 `fetch_documents`가 조용히 뺀다. 남은 문서로 계속한다 —
        # 콜백의 `sourceDocumentIds`는 요청의 집합과 같아야 하므로(6-1) 거기엔 손대지 않고,
        # `occurredDocumentIds`가 실제로 읽은 문서만 가리키게 된다.
        logger.warning(
            "requestId=%s 문서 %d건 중 %d건만 읽었다 — 읽은 문서로만 추출한다",
            job.requestId,
            len(source_ids),
            len(documents),
        )

    request = ExtractRequest(
        jobId=str(job.jobId),
        workspaceId=str(job.workspaceId),
        dictionaryVersionNo=_UNKNOWN_DICTIONARY_VERSION,
        existingTerms=existing_terms,
        documents=documents,
    )

    try:
        response = run_extract(request)
    except Exception as error:
        logger.exception("requestId=%s 추출 파이프라인이 실패했다", job.requestId)
        raise RealJobError("용어 추출 중 모델 호출이 실패했습니다. 잠시 후 다시 시도해 주세요.", "LLM_CALL_FAILED") from error

    return _terms_from(response.candidates, set(source_ids), job.requestId)


def run_real_check(job: LlmJobRequest) -> list[dict]:
    """대조 콜백(6-2)의 `suggestions` 배열을 만든다."""
    if job.documentId is None:
        raise RealJobError("대조 요청에 documentId가 없습니다.", "INVALID_REQUEST")

    try:
        document = fetch_document_version(job.documentId, job.documentVersionNo)
        dictionary = fetch_active_dictionary_entries(job.workspaceId)
    except Exception as error:
        logger.exception("requestId=%s 대조 입력을 읽지 못했다", job.requestId)
        raise RealJobError("문서와 사전집을 읽지 못했습니다.", "BACKEND_DB_READ_FAILED") from error

    if document is None:
        raise RealJobError("대조할 문서 본문을 찾지 못했습니다.", "DOCUMENT_NOT_FOUND")
    if not dictionary:
        # 기준이 없으면 대조 자체가 성립하지 않는다. 빈 결과로 성공시키면 "검사했는데
        # 고칠 게 없다"로 읽혀 더 나쁘다 — 무엇이 없는지 사용자에게 말해준다.
        raise RealJobError("워크스페이스에 활성 사전집이 없어 대조할 기준이 없습니다.", "DICTIONARY_NOT_FOUND")

    request = ContrastRequest(
        jobId=str(job.jobId),
        workspaceId=str(job.workspaceId),
        dictionaryVersionNo=_UNKNOWN_DICTIONARY_VERSION,
        dictionary=dictionary,
        documents=[document],
    )

    try:
        response = run_contrast(request)
    except Exception as error:
        logger.exception("requestId=%s 대조 파이프라인이 실패했다", job.requestId)
        raise RealJobError("문서 대조 중 모델 호출이 실패했습니다. 잠시 후 다시 시도해 주세요.", "LLM_CALL_FAILED") from error

    return _suggestions_from(response.suggestions, document.content, job.requestId)


# ── 6-1 terms ────────────────────────────────────────────────


def _terms_from(candidates: list[Candidate], source_ids: set[int], request_id: str) -> list[dict]:
    """후보(§3)를 후보어(6-1)로 바꾼다. 표기가 겹치면 뒤에 온 것을 버린다.

    `candidate_term`의 `(draft_dictionary_id, form)`이 유니크이고 검증기도 배열 안의
    표기 중복을 거절한다 — VARIANT 그룹의 대표 표기와 SYNONYM 그룹의 대표 표기가 같은
    말로 수렴하면 실제로 겹칠 수 있다.
    """
    terms: list[dict] = []
    seen_forms: set[str] = set()
    for candidate in candidates:
        term = _term_from(candidate, source_ids, request_id)
        if term is None:
            continue
        if term["form"] in seen_forms:
            logger.warning("requestId=%s 표기가 겹치는 후보어를 버린다. form=%s", request_id, term["form"])
            continue
        seen_forms.add(term["form"])
        terms.append(term)
    return terms


def _term_from(candidate: Candidate, source_ids: set[int], request_id: str) -> dict | None:
    if isinstance(candidate, HomographCandidate):
        form = candidate.forms[0]
        definition = _homograph_definition(candidate)
        english_name = None  # 갈래형엔 제안 영문명이 없다(§3.4)
        occurrences = [occurrence for sense in candidate.senses for occurrence in sense.occurrences]
    else:
        form = candidate.proposedPreferredForm
        definition = candidate.proposedDefinition
        english_name = candidate.proposedEnglishName
        occurrences = candidate.occurrences

    form = form.strip()
    if not form or len(form) > _MAX_FORM_CHARS:
        logger.warning(
            "requestId=%s 표기가 비었거나 너무 길어 후보어를 버린다. candidateId=%s", request_id, candidate.candidateId
        )
        return None
    if candidate.occurrenceCount < 1:
        # 출현 횟수는 문서를 리터럴로 훑어 센다(`normalize.count_all`). 0이 나왔다는 건
        # 모델이 문서에 없는 표기를 지어냈다는 뜻이다 — 후보어가 아니고, 그대로 보내면
        # 검증기가 결과 전체를 거절한다.
        logger.warning("requestId=%s 문서에서 한 번도 찾지 못한 후보어를 버린다. form=%s", request_id, form)
        return None

    return {
        "form": form,
        "proposedDefinition": definition,
        "proposedEnglishName": english_name if english_name and len(english_name) <= _MAX_ENGLISH_CHARS else None,
        "occurredDocumentIds": _occurred_document_ids(occurrences, source_ids),
        "occurrenceCount": candidate.occurrenceCount,
        "contextSnippets": [occurrence.snippet[:_MAX_SNIPPET_CHARS] for occurrence in occurrences],
        "variantForms": [f for f in candidate.forms if f and len(f) <= _MAX_FORM_CHARS],
    }


def _homograph_definition(candidate: HomographCandidate) -> str:
    """갈래형은 뜻이 여럿인데 후보어 하나엔 정의가 한 칸뿐이다(6-1).

    같은 표기로 후보어를 여러 건 보내면 표기 중복으로 결과 전체가 거절되므로, 갈래를
    **정의 한 칸에 나열해** 한 건으로 보낸다 — 어느 뜻으로 등재할지는 사람이 교정 화면에서
    정한다. `proposed_definition`은 `text` 컬럼이라 길이 상한이 사실상 없다.
    """
    return " / ".join(f"{sense.label}: {sense.definition}" for sense in candidate.senses)


def _occurred_document_ids(occurrences: list, source_ids: set[int]) -> list[int]:
    """출현 위치의 문서 id — 요청 집합 밖의 id가 하나라도 있으면 결과 전체가 거절된다(6-1).

    `§3`의 `documentId`는 문자열이고 계약은 정수라 변환이 필요하다. 변환에 실패하는 값은
    애초에 우리가 DB에서 읽은 id가 아니므로 버린다.
    """
    document_ids: list[int] = []
    for occurrence in occurrences:
        try:
            document_id = int(occurrence.documentId)
        except (TypeError, ValueError):
            continue
        if document_id in source_ids and document_id not in document_ids:
            document_ids.append(document_id)
    return document_ids


# ── 6-2 suggestions ──────────────────────────────────────────


def _suggestions_from(suggestions: list[ContrastSuggestion], body: str, request_id: str) -> list[dict]:
    """대조 제안(§12)을 콜백 제안(6-2)으로 바꾸고, 백엔드와 **같은 판정**을 미리 한 번 한다.

    파이프라인이 오프셋을 본문에서 직접 찾아 계산하므로 정상 경로에서는 어긋나지 않는다.
    그래도 확인하는 이유는 어긋날 때의 대가가 비대칭이기 때문이다 — 한 건이 틀리면
    `CheckSuggestionValidator`가 결과 전체를 거절하고 작업이 실패로 끝난다.

    같은 구간에 두 번 제안하는 것도 막는다. 모델이 같은 표기를 두 매치로 돌려주면 한
    자리에 제안어가 둘 생겨 사용자가 어느 쪽을 적용해도 나머지가 남는다.
    """
    results: list[dict] = []
    seen_spans: set[tuple[int, int]] = set()
    for suggestion in suggestions:
        origin_term = suggestion.foundForm
        suggestion_term = suggestion.preferredForm
        span = (suggestion.charStart, suggestion.charEnd)

        if not origin_term.strip() or not suggestion_term.strip():
            logger.warning("requestId=%s 표기가 빈 제안을 버린다. termId=%s", request_id, suggestion.termId)
            continue
        if len(origin_term) > _MAX_SUGGESTION_TERM_CHARS or len(suggestion_term) > _MAX_SUGGESTION_TERM_CHARS:
            logger.warning("requestId=%s 표기가 너무 긴 제안을 버린다. termId=%s", request_id, suggestion.termId)
            continue
        if body[suggestion.charStart : suggestion.charEnd] != origin_term:
            logger.warning(
                "requestId=%s 앵커가 가리키는 구간이 originTerm과 달라 제안을 버린다. termId=%s",
                request_id,
                suggestion.termId,
            )
            continue
        if span in seen_spans:
            logger.warning("requestId=%s 같은 구간에 겹치는 제안을 버린다. termId=%s", request_id, suggestion.termId)
            continue

        seen_spans.add(span)
        results.append(
            {
                "anchor": anchor_for(body, suggestion.charStart, suggestion.charEnd),
                "originTerm": origin_term,
                "suggestionTerm": suggestion_term,
            }
        )
    return results
