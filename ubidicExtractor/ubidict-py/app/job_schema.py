"""문서 ID 기반 "작업(job)" 요청 — `app/schema.py`(§3/§12, 문서를 인라인으로
통째로 받는 옛 계약)와는 다른, DB 조회 기반 새 계약이다.

백엔드가 문서 본문을 요청에 실어 보내지 않는다 — `documentIds`만 보내고,
FastAPI가 `app/backend_db.py`로 백엔드 DB(`document`/`document_version`/
`term` 테이블, `final/backend`의 Flyway 마이그레이션 기준)에서 직접 읽는다.
`dictionaryId`도 마찬가지로 ID만 받아 `term` 테이블에서 직접 조회한다
(existingTerms/dictionary를 인라인으로 안 보낸다).

`accessToken`은 **검증하지 않는다** — FastAPI는 이 값을 그대로 들고 있다가
결과를 돌려줄 때 그대로 같이 실어 보낸다(백엔드가 자기 쪽에서 필요하면
쓸 수 있게). 서명 검사·클레임 파싱은 여기서 하지 않는다.

`mode`: 백엔드의 `app.ai.mode`(`stub`/`mock`/`real`)와 같은 개념이며 값 셋은
SQS 계약(`app/queue_schema.py`의 대문자 `STUB`/`MOCK`/`REAL`)과 일대일로 맞춘다.

- `stub`: Gemini도 DB도 안 부르고 2~3초 뒤 **빈 결과**를 돌려준다. 백엔드가
  큐·폴링 배선만 확인하고 싶을 때 쓴다.
- `mock`: Gemini도 DB도 안 부르지만 **규격을 충족하는 임시 데이터**를 돌려준다.
  화면·저장까지 데이터가 흐르는지 비용 없이 확인할 때 쓴다 — `stub`은 결과가
  비어 있어 「성공했지만 아무것도 안 생김」과 실패를 구분할 수 없다.
- `real`: 실제로 DB를 읽고 Gemini를 부른다.

기본값은 백엔드 쪽 기본값과 맞춰 `stub`이다 — 명시하지 않으면 비용이 안 나간다.
"""

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ExtractJobRequest(BaseModel):
    jobId: str
    workspaceId: int
    dictionaryVersionNo: int
    dictionaryId: int
    documentIds: list[int] = Field(min_length=1)
    accessToken: str
    mode: Literal["stub", "mock", "real"] = "stub"


class ContrastJobRequest(BaseModel):
    jobId: str
    workspaceId: int
    dictionaryVersionNo: int
    dictionaryId: int
    documentIds: list[int] = Field(min_length=1)
    accessToken: str
    mode: Literal["stub", "mock", "real"] = "stub"
