"""서비스 레이어 — HTTP 라우터(`main.py`)와 SQS 컨슈머(`queue_consumer.py`)가
공유하는 단일 진입점.

`test/app/cli.py`의 `extract`/`contrast` 커맨드가 하던 일(파이프라인 호출 →
`ExtractResponse`/`ContrastResponse` 조립)과 완전히 같은 순서다 — 다른 점은
입력을 fixtures가 아니라 이미 파싱된 요청 객체(`ExtractRequest`/
`ContrastRequest`)로 받는다는 것뿐이다. 로직 자체(무엇을 호출하고 어떤
순서로 후보를 조립하는지)는 test/에서 검증된 것 그대로다.

`mode`가 `stub`·`mock`이면 DB·Gemini를 건너뛴다 — 앞의 것은 빈 결과,
뒤의 것은 규격을 충족하는 임시 데이터를 돌려준다(`app/job_schema.py` 참고).

실패하면 예외를 그대로 올려보낸다 — 여기서 삼키지 않는다. HTTP 라우터는
그걸 받아 502/500으로 응답하고, 큐 컨슈머는 그걸 받아 메시지를 삭제하지
않는다(SQS 재시도/DLQ에 맡긴다).

**`run_extract_job`/`run_contrast_job`(2026-09-14 추가)**는 문서 본문을
인라인으로 받지 않고 `documentIds`/`dictionaryId`만 받아 `app.backend_db`로
백엔드 DB에서 직접 읽어온 뒤, 위 `run_extract`/`run_contrast`에 그대로
위임한다 — 판정 로직 자체는 하나도 안 바뀐다. `accessToken`은 검증하지
않고 그대로 들고 있다가 호출부(큐 컨슈머·HTTP 라우터)가 응답에 그대로
실어 보낸다.
"""

from __future__ import annotations

import time

from app.backend_db import fetch_dictionary_entries, fetch_documents, fetch_existing_terms
from app.job_schema import ContrastJobRequest, ExtractJobRequest
from app.mock_delay import sleep_mock_delay
from app.pipeline.contrast_llm import find_llm_contrast_matches
from app.pipeline.llm import extract_synonyms_and_homographs
from app.pipeline.synonym import build_homograph_candidates, build_synonym_candidates
from app.pipeline.variant import find_variants
from app.schema import (
    Candidate,
    ContrastRequest,
    ContrastResponse,
    ContrastSuggestion,
    ExtractRequest,
    ExtractResponse,
    GroupCandidate,
    Occurrence,
    Usage,
)

# stub 모드가 실제로 뭔가 하는 척(비동기 처리 시간 흉내) 기다리는 시간. 백엔드가
# 큐/폴링 배선만 검증하고 싶을 때 쓰는 값이라 정밀할 필요 없다 — 2~3초 사이.
_STUB_DELAY_SECONDS = 2.5

# mock 모드는 실제 LLM 대신 랜덤 지연을 넣고 고정 응답을 반환한다. 화면·저장까지
# 데이터가 흐르는지 확인하면서 외부 모델의 처리시간도 모의한다.


def _mock_usage(elapsed_ms: int) -> Usage:
    return Usage(model="mock", inputTokens=0, outputTokens=0, llmCalls=0, elapsedMs=elapsed_ms)

_MOCK_WARNING = "mode=mock — 실제 LLM·DB 호출 없이 고정 임시 데이터를 반환함"


def _mock_extract_response(job: ExtractJobRequest) -> ExtractResponse:
    """DB·Gemini 없이 §3 규격을 충족하는 VARIANT 후보 하나를 만든다.

    SQS 경로(`app/queue_consumer.py`의 `_mock_term`)가 돌려주는 것과 같은 용어를
    쓴다 — 두 경로를 번갈아 확인해도 화면에 같은 것이 보이게 하기 위함이다.
    """
    elapsed_ms = sleep_mock_delay()
    document_id = str(job.documentIds[0])
    snippet = "회원은 결제수단을 선택해 주문을 결제한다."

    return ExtractResponse(
        jobId=job.jobId,
        status="SUCCESS",
        dictionaryVersionNo=job.dictionaryVersionNo,
        candidates=[
            GroupCandidate(
                candidateId="mock-1",
                kind="VARIANT",
                forms=["결제", "페이먼트"],
                proposedPreferredForm="결제",
                proposedEnglishName="Payment",
                proposedDefinition="재화나 용역의 대가를 지급하는 행위",
                confidence=0.9,
                reason="mock 모드가 돌려주는 고정 후보다. 판정 근거가 아니다.",
                occurrenceCount=1,
                documentCount=1,
                occurrences=[
                    Occurrence(
                        documentId=document_id,
                        department="mock",
                        form="결제",
                        snippet=snippet,
                        charStart=snippet.index("결제"),
                        charEnd=snippet.index("결제") + len("결제"),
                    )
                ],
            )
        ],
        usage=_mock_usage(elapsed_ms),
        warnings=[_MOCK_WARNING],
    )


def _mock_contrast_response(job: ContrastJobRequest) -> ContrastResponse:
    """DB·Gemini 없이 §12 규격을 충족하는 대조 제안 하나를 만든다.

    **오프셋은 실제 문서 본문과 맞지 않는다.** 이 응답은 워커를 직접 부르는
    경로(HTTP)의 형태 확인용이고, 백엔드로 콜백을 보내는 SQS 경로에서는
    `CheckSuggestionValidator` 가 본문을 잘라 `originTerm` 과 대조하므로 본문을
    읽지 않고 만든 제안은 거절된다.
    """
    elapsed_ms = sleep_mock_delay()
    snippet = "회원은 페이먼트 수단을 선택한다."

    return ContrastResponse(
        jobId=job.jobId,
        status="SUCCESS",
        dictionaryVersionNo=job.dictionaryVersionNo,
        suggestions=[
            ContrastSuggestion(
                termId="mock-term-1",
                preferredForm="결제",
                foundForm="페이먼트",
                documentId=str(job.documentIds[0]),
                department="mock",
                snippet=snippet,
                charStart=snippet.index("페이먼트"),
                charEnd=snippet.index("페이먼트") + len("페이먼트"),
                reason="mock 모드가 돌려주는 고정 제안이다. 판정 근거가 아니다.",
                method="rule",
            )
        ],
        usage=_mock_usage(elapsed_ms),
        warnings=[_MOCK_WARNING],
    )


def run_extract(request: ExtractRequest, *, note: str = "") -> ExtractResponse:
    """§3 — VARIANT(규칙) + SYNONYM·HOMOGRAPH(LLM 1회 호출)를 모두 판정한다.

    `test/app/cli.py`의 `extract` 커맨드(90~144행)와 동일한 순서.
    """
    candidates: list[Candidate] = list(find_variants(request))

    llm_result, usage = extract_synonyms_and_homographs(request, no_cache=False, note=note)

    next_id = len(candidates) + 1
    synonym_candidates = build_synonym_candidates(request, llm_result, start_id=next_id)
    candidates.extend(synonym_candidates)
    next_id += len(synonym_candidates)
    candidates.extend(build_homograph_candidates(request, llm_result, start_id=next_id))

    return ExtractResponse(
        jobId=request.jobId,
        status="SUCCESS",
        dictionaryVersionNo=request.dictionaryVersionNo,
        candidates=candidates,
        usage=usage,
        warnings=[],
    )


def run_contrast(request: ContrastRequest, *, note: str = "") -> ContrastResponse:
    """§12 — 사전집 정의 기반 LLM 대조(§10 D-22, 규칙 단계 없음).

    `test/app/cli.py`의 `contrast` 커맨드(241~278행)와 동일한 순서.
    """
    suggestions, usage = find_llm_contrast_matches(request, no_cache=False, note=note)

    return ContrastResponse(
        jobId=request.jobId,
        status="SUCCESS",
        dictionaryVersionNo=request.dictionaryVersionNo,
        suggestions=suggestions,
        usage=usage,
        warnings=[],
    )


def run_extract_job(job: ExtractJobRequest) -> ExtractResponse:
    """`documentIds`/`dictionaryId`로 백엔드 DB에서 직접 읽어온 뒤 `run_extract`에 위임.

    `mode="stub"`이면 DB 조회·Gemini 호출 둘 다 안 하고 몇 초 뒤 빈 결과를
    돌려준다(백엔드의 `TermExtractorStub`과 같은 역할 — 큐 배선만 확인하고
    싶을 때 비용 없이 쓴다). `mode="mock"`이면 역시 DB·Gemini를 안 부르지만
    빈 결과가 아니라 **규격을 충족하는 임시 후보 하나**를 돌려준다.
    """
    if job.mode == "mock":
        return _mock_extract_response(job)

    if job.mode == "stub":
        time.sleep(_STUB_DELAY_SECONDS)
        return ExtractResponse(
            jobId=job.jobId,
            status="SUCCESS",
            dictionaryVersionNo=job.dictionaryVersionNo,
            candidates=[],
            usage=Usage(model="stub", inputTokens=0, outputTokens=0, llmCalls=0, elapsedMs=int(_STUB_DELAY_SECONDS * 1000)),
            warnings=["mode=stub — 실제 LLM 호출 없이 빈 결과를 반환함"],
        )

    documents = fetch_documents(job.documentIds)
    existing_terms = fetch_existing_terms(job.dictionaryId)
    request = ExtractRequest(
        jobId=job.jobId,
        workspaceId=str(job.workspaceId),
        dictionaryVersionNo=job.dictionaryVersionNo,
        existingTerms=existing_terms,
        documents=documents,
    )
    return run_extract(request)


def run_contrast_job(job: ContrastJobRequest) -> ContrastResponse:
    """`documentIds`/`dictionaryId`로 백엔드 DB에서 직접 읽어온 뒤 `run_contrast`에 위임.

    `mode="stub"`이면 `run_extract_job`과 동일하게 DB·Gemini 둘 다 건너뛰고,
    `mode="mock"`이면 임시 제안 하나를 돌려준다.
    """
    if job.mode == "mock":
        return _mock_contrast_response(job)

    if job.mode == "stub":
        time.sleep(_STUB_DELAY_SECONDS)
        return ContrastResponse(
            jobId=job.jobId,
            status="SUCCESS",
            dictionaryVersionNo=job.dictionaryVersionNo,
            suggestions=[],
            usage=Usage(model="stub", inputTokens=0, outputTokens=0, llmCalls=0, elapsedMs=int(_STUB_DELAY_SECONDS * 1000)),
            warnings=["mode=stub — 실제 LLM 호출 없이 빈 결과를 반환함"],
        )

    documents = fetch_documents(job.documentIds)
    dictionary = fetch_dictionary_entries(job.dictionaryId)
    request = ContrastRequest(
        jobId=job.jobId,
        workspaceId=str(job.workspaceId),
        dictionaryVersionNo=job.dictionaryVersionNo,
        dictionary=dictionary,
        documents=documents,
    )
    return run_contrast(request)
