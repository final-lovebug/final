# AI Worker Contract (Spring Boot ↔ FastAPI)

이 문서는 백엔드(Spring Boot)와 AI 워커(FastAPI)가 주고받는 것의 **유일한 원본**이다(`REQ-AI-001`). 워커 구현은 이 저장소 밖에 있으므로, 여기 적힌 것과 코드가 어긋나면 배포하고 나서야 드러난다.

근거 결정은 `docs/plan/CONFLICTS.md` 3-4절(`D-66`~`D-78`)이다.

> **콜백 엔드포인트의 HTTP 규격은 `docs/API.md`의 각 도메인 절에도 있다.** 그것은 우리가 여는 엔드포인트이기 때문이다. 이 문서는 그 절을 **참조하고 복제하지 않는다** — 두 문서가 어긋나는 것이 `CONFLICTS.md`가 존재하는 이유다.

---

## 1. 범위

| | 백엔드(이 저장소) | 워커(FastAPI) |
| --- | --- | --- |
| 작업 접수와 상태 저장 | ● | |
| 요청 메시지 발행 | ● | |
| 문서 본문·활성 용어 읽기 | | ● (DB 직접 조회) |
| 모델 호출과 프롬프트 | | ● |
| 결과 검증과 초안 생성 | ● | |
| 실패·타임아웃 회수 | ● | |

**모델 API 키는 워커 프로세스에만 있다**(`NFR-AI-003`). 백엔드는 갖지 않는다.

---

## 2. 흐름

```
POST /api/draft-dictionaries/extractions   → 202, 작업 PENDING
  │  (요청 트랜잭션 커밋)
  ├─ requestId(UUIDv4) 생성 → 작업 행에 저장, 상태 RUNNING
  └─ SQS 요청 큐로 발행 ──────────────────────────────┐
                                                      ▼
                                    워커: documentId 로 DB 조회 → 모델 호출
                                                      │
  ┌───────────────────────────────────────────────────┘
  ▼
POST /api/internal/llm/extractions/{jobId}/result   (requestId 동봉)
  │  requestId 대조 → 결과 검증 → 초안 생성 → 작업 SUCCEEDED
  ▼
GET /api/draft-dictionaries/extractions/{jobId}     ← 사용자는 폴링으로 확인
```

문서 대조(`/api/draft-documents/checks`)도 같은 모양이다.

**사용자에게 완료를 알리는 수단은 여전히 폴링이다**(`D-34`). 콜백은 백엔드가 결과를 받는 경로일 뿐 사용자 알림 경로가 아니다.

---

## 3. 계약 버전

- 모든 요청 메시지는 **`contractVersion`** 을 갖는다. 현재 **1**이다.
- **호환 변경**(필드 추가, 선택 필드의 의미 보존)은 버전을 올리지 않는다. 워커는 **모르는 필드를 무시**해야 한다.
- **비호환 변경**(필드 제거·의미 변경·타입 변경)은 버전을 올린다. 양쪽이 배포될 때까지 워커는 두 버전을 함께 처리한다.
- 이 문서 마지막 절의 변경 이력에 남긴다.

> HTTP 경로에는 버전 세그먼트를 두지 않는다 — `docs/API.md` «API 버저닝»의 결정을 그대로 따른다. 버전은 메시지 본문에만 있다.

**이 문서의 예시 JSON은 테스트의 기대 문자열과 같아야 한다** — `SqsLlmJobRequestSenderTest`가 큐로 나가는 본문을 그대로 출력한다.

---

## 4. 전송 계층

### 4-1. 요청 큐 (백엔드 → 워커)

| 항목 | 값 |
| --- | --- |
| 프로퍼티 | `app.messaging.sqs.llm-request-queue` |
| 기본 큐 이름 | `lovebug-llm-request` |
| 종류 | **표준 큐**(FIFO 아님) |
| 순서 | **보장되지 않는다** |
| 전달 | **at-least-once — 같은 메시지가 두 번 이상 온다** |
| 본문 | UTF-8 JSON 문자열 |

**도메인 이벤트 큐(`lovebug-domain-event`)와 다른 큐다**(`D-67`). 그쪽에는 백엔드 내부의 이벤트가 흐르며 워커가 볼 것이 없다.

`MessageGroupId`·`MessageDeduplicationId`는 붙지 않는다 — 표준 큐에 실어 보내면 SQS가 `InvalidParameterValue`로 거절한다.

**DLQ와 redrive policy는 인프라에서 건다**(`NFR-MSG-004`). `maxReceiveCount`와 가시성 타임아웃은 백엔드의 `app.ai.timeout.job`(기본 `PT15M`)보다 **작아야 한다** — 재배달 중인 작업을 백엔드 스위퍼가 먼저 실패시키면 워커의 응답이 버려진다(`D-77`).

### 4-2. 콜백 (워커 → 백엔드)

| 항목 | 값 |
| --- | --- |
| 경로 접두사 | `/api/internal/llm/` |
| 인증 헤더 | **없다** |
| 호출자 확인 | 본문의 `requestId` 대조(`D-70`) |
| Content-Type | `application/json` |

**`/api/internal/**`은 인증 필터를 통과한다.** 회원 principal이 없는 서버-투-서버 경로이기 때문이다. 그래서 **배포 시 보안 그룹·인그레스로 워커 출발지만 이 경로에 닿게 제한해야 하며, 그것이 없으면 `NFR-AI-002`는 미충족이다.**

---

## 5. 요청 스키마

### 5-1. 공통 필드

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `contractVersion` | int | 현재 `1` |
| `requestId` | string(36) | UUIDv4. **콜백에 그대로 실어 돌려줘야 한다** |
| `jobType` | enum | `TERM_EXTRACTION` \| `DOCUMENT_CHECK` |
| `jobId` | long | 콜백 경로의 `{jobId}` |
| `workspaceId` | long | |
| `mode` | enum | `STUB` \| `MOCK` \| `REAL` |
| `requestedAt` | ISO-8601 offset | 발행 시각 |

**작업 종류에 따라 채워지지 않는 필드는 `null`로 실린다.** 워커는 `jobType`으로 분기한다.

### 5-2. `mode`의 의미 (`D-71`·`D-86`)

- **`REAL`** — 모델을 호출해 실제 결과를 만든다.
- **`STUB`** — **모델을 호출하지 않는다.** 임의 시간(수 초 이내)을 기다린 뒤 빈 결과를 돌려준다. 기존 대역과의 호환을 위한 값이다.
- **`MOCK`** — **모델·워커 DB 조회를 호출하지 않는다.** 워커가 계약에 맞는 고정 목 결과를 HTTP 콜백으로 돌려준다. `dev`에서 Spring → LocalStack → FastAPI → Spring 왕복을 확인하기 위한 값이다.

백엔드는 이 값으로 아무 분기도 하지 않는다. `dev`는 `MOCK`, `prod`는 `REAL`이 기본이다.

### 5-3. 용어 추출 (`TERM_EXTRACTION`)

```json
{
  "contractVersion": 1,
  "requestId": "0d5c6f6e-0000-4000-8000-000000000001",
  "jobType": "TERM_EXTRACTION",
  "jobId": 30,
  "workspaceId": 1,
  "dictionaryId": null,
  "sourceDocumentIds": [10, 20],
  "documentId": null,
  "documentVersionNo": null,
  "mode": "REAL",
  "requestedAt": "2026-09-14T10:00:00+09:00"
}
```

| 필드 | 설명 |
| --- | --- |
| `dictionaryId` | 갱신 대상 사전집. **첫 회차 추출이면 `null`이다** — 아직 사전집이 없다 |
| `sourceDocumentIds` | 추출 대상 문서. 백엔드가 **이미 걸러서 보낸다**(활성 사전집 버전과 같고 직접 편집되지 않은 문서, `G-12`). 워커는 다시 거르지 않는다 |

각 문서의 본문은 **현재 발행 버전**(`document.current_version_no`)을 읽는다.

### 5-4. 문서 대조 (`DOCUMENT_CHECK`)

```json
{
  "contractVersion": 1,
  "requestId": "0d5c6f6e-0000-4000-8000-000000000001",
  "jobType": "DOCUMENT_CHECK",
  "jobId": 40,
  "workspaceId": 20,
  "dictionaryId": null,
  "sourceDocumentIds": null,
  "documentId": 10,
  "documentVersionNo": 3,
  "mode": "REAL",
  "requestedAt": "2026-09-14T10:00:00+09:00"
}
```

**`documentVersionNo`가 지정하는 버전을 읽어야 한다.** 그 사이 문서가 새 버전으로 바뀌면 제안어의 앵커 오프셋이 무효가 되므로, 백엔드는 콜백에 실린 버전이 현재 버전과 다르면 결과를 받지 않고 작업을 실패로 끝낸다.

### 5-5. 워커가 읽는 테이블

**백엔드 DB를 직접 조회한다**(`D-69`). 읽기 전용 계정을 쓴다.

| 테이블 | 읽는 것 | 주의 |
| --- | --- | --- |
| `document` | `id`, `workspace_id`, `title`, `current_version_no` | `deleted_at is null`인 행만 |
| `document_version` | `document_id`, `version_no`, `body` | 본문의 유일한 저장 위치. `(document_id, version_no)`가 유니크 |
| `dictionary` | `workspace_id`, `version_no`, `status` | 활성 사전집은 `status = 'ACTIVE'`이며 워크스페이스당 하나 |
| `term` | `dictionary_id`, `preferred_form`, `english_name`, `definition` | 활성 사전집에 속한 행이 그 워크스페이스의 표준어다 |

> **이 목록이 두 서비스의 결합 지점이다.** 위 컬럼을 바꾸는 마이그레이션은 워커를 함께 깨뜨리므로, 변경 시 이 문서와 워커 저장소를 함께 확인한다.

---

## 6. 콜백 스키마

### 6-1. 추출 성공

`POST /api/internal/llm/extractions/{jobId}/result` → **204**

```json
{
  "requestId": "0d5c6f6e-0000-4000-8000-000000000001",
  "sourceDocumentIds": [10, 20],
  "terms": [
    {
      "form": "결제",
      "proposedDefinition": "재화나 용역의 대가를 지급하는 행위",
      "proposedEnglishName": "Payment",
      "occurredDocumentIds": [10],
      "occurrenceCount": 3,
      "contextSnippets": ["회원은 결제할 수 있다."],
      "variantForms": ["결제", "페이먼트"]
    }
  ]
}
```

| 필드 | 제약 |
| --- | --- |
| `requestId` | 필수. 요청에 실려 온 값 그대로 |
| `sourceDocumentIds` | 비어 있지 않아야 하고 **요청의 집합과 완전히 같아야 한다** |
| `terms` | 배열(빈 배열 허용). `null` 금지 |
| `terms[].form` | 공백 아님. **배열 안에서 중복 금지** |
| `terms[].occurredDocumentIds` | `null` 금지. **`sourceDocumentIds` 밖의 id가 있으면 거절** |
| `terms[].occurrenceCount` | 1 이상 |
| `terms[].contextSnippets` | `null` 금지(빈 배열 허용) |
| `terms[].variantForms` | 추출기가 **한 개념으로 묶은 여러 표기**(`D-65`). 생략하거나 `null`이면 빈 목록으로 다룬다 |
| `terms[].proposedDefinition`·`proposedEnglishName` | `null` 허용 |

### 6-2. 대조 성공

`POST /api/internal/llm/checks/{jobId}/result` → **204**

```json
{
  "requestId": "0d5c6f6e-0000-4000-8000-000000000001",
  "documentVersionNo": 3,
  "suggestions": [
    {
      "anchor": { "startOffset": 10, "endOffset": 12 },
      "originTerm": "유저",
      "suggestionTerm": "이용자"
    }
  ]
}
```

| 필드 | 제약 |
| --- | --- |
| `documentVersionNo` | 워커가 실제로 읽은 버전. 현재 버전과 다르면 결과를 받지 않고 작업을 실패로 끝낸다 |
| `suggestions` | 배열(빈 배열 허용). `null` 금지 |
| `anchor` | `0 <= startOffset <= endOffset`, `endOffset <= 본문 길이` |
| `originTerm` | 공백 아님. **`body.substring(startOffset, endOffset)`와 정확히 같아야 한다** |
| `suggestionTerm` | 공백 아님 |

> **앵커 오프셋 단위는 UTF-16 code unit이다** — Java `String`의 인덱스와 같다. Python의 문자열 인덱스는 **코드포인트** 기준이라, 이모지나 일부 한자(BMP 밖 문자)가 본문에 있으면 그 뒤의 오프셋이 어긋난다. 워커는 본문을 UTF-16으로 다루거나 오프셋을 변환해서 보내야 한다.

### 6-3. 실패

`POST /api/internal/llm/{extractions|checks}/{jobId}/failure` → **204**

```json
{
  "requestId": "0d5c6f6e-0000-4000-8000-000000000001",
  "reason": "모델 응답이 JSON 스키마를 만족하지 않습니다.",
  "code": "LLM_SCHEMA_VIOLATION"
}
```

| 필드 | 설명 |
| --- | --- |
| `reason` | **사용자에게 그대로 보인다.** 최대 1000자. 모델 원문이나 내부 스택을 넣지 않는다 |
| `code` | 워커 내부 분류. **저장하지 않고 백엔드 로그에만 남는다** |

워커가 자체 재시도를 모두 소진한 뒤에 보낸다 — 이 콜백은 작업을 종료시킨다.

---

## 7. 상태·오류 규약

### 7-1. 응답 코드와 재시도 (`D-73`)

| 응답 | 뜻 | 워커 동작 |
| --- | --- | --- |
| **204** | 반영됐거나, 이미 끝난 작업이라 무시됐다 | **ack. 메시지를 지운다** |
| **400** | 본문이 계약을 어겼다 | **재시도 금지.** 메시지를 지우고 알람 |
| **403** | `requestId`가 작업의 것과 다르다 | **재시도 금지.** 메시지를 지우고 알람 |
| **404** | 그런 작업이 없다(롤백된 유령 메시지) | **재시도 금지.** 메시지를 지운다 |
| **409** | 결과가 검증에서 거절됐다 | **재시도 금지.** 메시지를 지우고 알람. 백엔드는 작업을 실패로 끝낸다 |
| **5xx**·타임아웃 | 백엔드 문제 | 지수 백오프로 재시도 → 끝내 실패하면 DLQ |

**4xx를 재시도하면 안 된다.** 같은 본문으로 다시 보내도 같은 응답이 오고, 메시지는 DLQ까지 간다.

### 7-2. 멱등 (`D-72`)

같은 메시지를 두 번 받아 콜백을 두 번 보내도 **초안은 하나만 생긴다.** 이미 끝난 작업에 도착한 콜백은 아무것도 바꾸지 않고 **204**로 답한다 — 성공한 작업에 늦게 도착한 실패 콜백도 성공을 뒤집지 않는다.

그러므로 **워커는 확신이 없으면 다시 보내도 된다.** 다만 4xx는 재시도하지 않는다.

### 7-3. 콜백을 보내지 못한 경우

백엔드가 응답하지 않아 결과를 끝내 전달하지 못하면, 백엔드의 타임아웃 스위퍼가 그 작업을 **실패로 회수한다**(`D-77`). 사용자는 폴링에서 실패 사유를 보고 다시 요청할 수 있다.

---

## 8. 관측

- **`requestId`가 correlation id를 겸한다**(`D-70`). 워커는 모든 로그에 이 값을 남긴다 — 백엔드 로그의 같은 값과 맞춰 한 작업의 전 구간을 따라갈 수 있다(`NFR-INF-008`).
- 워커는 모델 호출마다 **시작·성공·실패·타임아웃·재시도·최종 실패**를 남긴다. 필드는 `docs/LOG.md` «외부 연동 로그»를 따른다 — 외부 시스템 이름, 내부 식별자(`jobId`), 외부 요청 식별자, `elapsedMs`.
- **본문과 모델 응답 원문을 로그에 남기지 않는다.**

---

## 9. 변경 이력

| 버전 | 날짜 | 변경 | 호환성 |
| --- | --- | --- | --- |
| 1 | 2026-09-14 | 최초 정의(`T-INT-6`, `D-66`~`D-78`) | — |
| 1 | 2026-09-14 | `MOCK` 모드 추가(`D-86`). `dev`에서 고정 목 결과로 실제 SQS·HTTP 콜백 왕복을 검증한다 | 호환 — 워커는 알 수 없는 필드를 무시하는 대신 새 enum 값을 처리해야 한다 |
