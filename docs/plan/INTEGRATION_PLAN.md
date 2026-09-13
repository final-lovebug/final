# 통합(Integration) 작업 계획

## Context

member(+auth)를 포함해 9개 백엔드 도메인(member~revisionlog)과 FastAPI 추출·대조 서비스
(`ubidicExtractor/ubidict-py`)는 각자 독립적으로는 이미 완성돼 있는데, 셋을 잇는 이음매
세 군데가 비어 있어 "따로는 되는데 전체로는 안 되는" 상태다.

1. **프론트-백엔드**: 프론트 7개 feature 중 5개(workspace·document·dictionary·review·
   notification)가 여전히 `fixtures.ts` + `delay()` 목업만 쓴다. 대응하는 백엔드 REST API는
   이미 실사용 가능한데 화면에 반영되지 않았다.
2. **백엔드-FastAPI(SQS)**: DD-5(문서 대조 작업)·DI-5(용어 추출 작업)는 접수·실행 파이프라인이
   끝났지만, 실제로 LLM을 부르는 `TermCheckerPort`/`TermExtractorPort`는 빈 리스트만 돌려주는
   스텁뿐이다. AWS에 이미 있는 SQS 큐(`lovebug-llm-request`/`-reply`)를 백엔드가 아직 읽지도
   쓰지도 않는다 — ubidict-py는 그 큐로 실 Gemini 왕복까지 이미 검증해 뒀다.
3. **프론트 화면 공백**: 위 두 이음매가 비어 있다 보니 추출·대조 결과를 보여줄 화면 자체가
   없다(`TermExtractionPage`의 "추출 실행" 버튼은 작업을 만들지 않고 목록으로 이동만 한다).

**①FE↔BE 연동을 먼저, ②SQS↔FastAPI 실연동을 나중에** 진행한다(두 트랙은 건드리는 파일이
겹치지 않아 실제 진행은 병렬도 가능하다 — 순서는 우선순위일 뿐 강제 선행 관계가 아니다).
SQS 메시지 봉투 스키마는 **ubidict-py의 기존 초안을 그대로 채택**해 이 문서에서 확정한다.

`docs/plan/EXECUTION_ORDER.md`의 관례를 따라 이 작업들은 **`T-INT-` 접두사**를 그대로 잇는다
(마지막 `T-INT-5`까지 사용됨 → `T-INT-6`부터). `docs/plan/CONFLICTS.md`도 함께 읽는다 — 이
문서가 인용하는 `D-*`·`G-*` 결정은 전부 거기 정의돼 있다.

---

## 0. 현재 상태 (2026-09-14 조사 결과)

### 백엔드

| 항목 | 상태 |
|---|---|
| 9개 도메인(member~revisionlog) | 전부 머지됨 |
| DD-5(문서 대조 작업 접수·실행) | **완료** — `CheckJob`·`CheckJobRepository`·`DraftDocumentCheckController`(`POST`/`GET /api/draft-documents/checks{,/{id}}`), `@Async`+`AFTER_COMMIT` 실행 |
| DI-5(용어 추출 작업 접수·실행) | **완료** — 동일 패턴, `ExtractionJob`·`DraftDictionaryExtractionController` |
| `TermExtractorPort`/`TermCheckerPort` | 인터페이스만 실재. 구현체는 `TermExtractorStub`/`TermCheckerStub`(빈 리스트 반환)뿐 — **real 어댑터 없음** |
| `EventPublisher`(도메인 이벤트) | `InMemoryEventPublisher`(기본)·`SqsEventPublisher`(`app.messaging.mode=sqs`) 둘 다 실동작. 단 이건 **도메인 이벤트 전용 단일 큐**(`lovebug-domain-event`)이고 지금은 Notification만 소비 중 |
| LLM 워커 큐(`lovebug-llm-request`/`-reply`) | AWS에 프로비저닝됨, **백엔드에 읽고 쓰는 코드가 0줄** — `application.yml` 주석이 이미 자각하고 있다 |
| T-INT-2(크로스 도메인 real 전환) | 완료 |
| T-INT-3(인증 주체·프로파일 분리) | 거의 완료 — `NotificationController` 4개 엔드포인트만 아직 `@RequestParam Long memberId` |

### 프론트엔드

| 영역 | 상태 |
|---|---|
| `auth` | 실연동 완료 |
| `member` | 부분 실연동(`fetchCurrentMember`·`withdrawMember`) — `fetchWorkspaceMembers`는 목업 |
| `workspace`·`document`·`dictionary`·`review`·`notification` | **전부 목업**(`fixtures.ts`+`delay()`). 대응 백엔드 API는 이미 실사용 가능 |
| 추출/대조 작업 진행상태·결과 화면 | **존재하지 않음**(목업조차 없음) |
| `httpClient`(`shared/api/httpClient.ts`) | 완성돼 있고 재사용 가능 — 인증 헤더, 401 시 리프레시+재시도, `credentials: include` |
| dev 프록시/env | 없음 — `VITE_API_BASE_URL` 미설정 시 `localhost:8080` 직행(CORS+credentials) |

### FastAPI(`ubidicExtractor/ubidict-py`, 별도 레포)

- `/extract`·`/contrast` 실 Gemini 호출 검증 완료. 실 AWS SQS(표준 큐, DLQ 연결)로 스텁
  백엔드를 상대로 요청→처리→응답 왕복까지 완료(2026-09-13).
- **표준 큐(FIFO 아님) 기준으로 이미 구현돼 있다** — 백엔드의 `D-53`(표준 큐 채택) 결정과
  이미 일치, 충돌 없음.
- `app/queue_schema.py`의 봉투는 "초안" 상태로 백엔드 실제 스키마를 기다리는 중 — 이
  문서가 그 대기를 해소한다.

---

## 1. SQS 작업 큐 계약 (T-INT-7의 산출물로 확정)

### 1-1. 봉투(Envelope)

큐: `lovebug-llm-request`(요청)/`lovebug-llm-reply`(응답)/`lovebug-llm-dlq`(DLQ,
`maxReceiveCount=3` 이미 연결됨). **표준 큐**(FIFO 아님, `D-53`과 동일 이유).

요청 봉투(백엔드→ubidict-py, ubidict-py `QueueTaskEnvelope`와 동일):

```json
{ "jobId": "123", "type": "extract" | "contrast", "payload": { ... } }
```

- `replyQueueUrl`은 보내지 않는다 — ubidict-py가 `.env`의 기본 응답 큐를 쓴다.
- **`jobId`는 문자열이다.** 백엔드 `CheckJob`/`ExtractionJob`의 PK는 `Long`이므로 어댑터가
  `.toString()`으로 보내고 응답에서 `Long.parseLong()`으로 되돌린다.
- **`type` 값 주의** — 백엔드 내부 용어는 "대조"(check)지만 와이어 값은 ubidict-py 관례를
  따라 `"contrast"`를 쓴다(`"check"`가 아니다). 어댑터 경계에서만 이름이 바뀐다.

응답 봉투(ubidict-py→백엔드, `QueueResultEnvelope`):

```json
{ "jobId": "123", "type": "extract" | "contrast", "status": "SUCCESS" | "FAILED", "result": { ... } | null, "error": "..." | null }
```

- ubidict-py `ExtractResponse.status`가 가질 수 있는 `PARTIAL`은 백엔드에서 `SUCCESS`로
  취급한다 — `CheckJob`/`ExtractionJob`의 상태 축은 `PENDING/RUNNING/SUCCEEDED/FAILED`
  4종뿐이라 `PARTIAL`을 받을 자리가 없다. `warnings`는 로그로만 남기고 저장하지 않는다.

### 1-2. payload/result 본문 — ubidict-py `app/schema.py`(SPEC.md §3)를 그대로 채택

`payload`는 `type=extract`면 `ExtractRequest`, `type=contrast`면 `ContrastRequest` 형태의
JSON. `result`(SUCCESS)는 각각 `ExtractResponse`/`ContrastResponse`. **필드명·구조는
`ubidict-py/app/schema.py`가 정본이다** — 이미 실 Gemini로 검증된 계약이라 백엔드가 요구해서
바꾸지 않는다. 백엔드 어댑터가 자신의 단순한 포트 타입(`TermSnapshot`/`ExtractedTerm`/
`DocumentSnapshot`/`CheckSuggestion`)과 이 풍부한 스키마 사이 변환을 전담한다(포트/어댑터
패턴의 목적 그대로).

### 1-3. 변환 매핑 — 어댑터가 흡수할 것

| 백엔드 쪽 | → | ubidict-py 쪽 | 비고 |
|---|---|---|---|
| `sourceDocumentIds`+문서 본문 | → | `ExtractRequest.documents[]`(`DocumentInput`) | `department`는 백엔드에 대응 개념이 없다 — **빈 문자열로 채운다**(제안 — 프롬프트 라벨용일 뿐 구조에 영향 없어 별도 확정 불필요) |
| `List<TermSnapshot> activeTerms` | → | `ExtractRequest.existingTerms[]` | `synonyms`는 항상 `[]`(`D-28`로 동의어를 안 둔다) |
| `ExtractResponse.candidates[]`(`GroupCandidate`\|`HomographCandidate`) | → | `List<ExtractedTerm>` | `GroupCandidate` → `form=proposedPreferredForm`, `occurredDocumentIds`=occurrences documentId 중복제거, `contextSnippets`=occurrences[].snippet. **`HomographCandidate`는 대응 필드가 없다**(`ExtractedTerm`은 표기 하나=뜻 하나 전제) — 1차는 건너뛰고 경고 로그만 남긴다(제안) |
| `DocumentSnapshot`(단일 문서) | → | `ContrastRequest.documents=[그 문서 하나]` | 리스트지만 항상 원소 1개 |
| `List<TermSnapshot> activeTerms` | → | `ContrastRequest.dictionary[]`(`DictionaryEntry`) | `definition` 포함, `synonyms=[]` |
| `ContrastResponse.suggestions[]` | → | `List<CheckSuggestion>` | `anchor=TextRange(charStart,charEnd)`, `originTerm=foundForm`, `suggestionTerm=preferredForm`. `termId`·`documentId`·`department`·`reason`·`method`는 `CheckSuggestion`에 자리가 없어 버려진다(근거 표시가 필요해지면 후속 과제) |

> **`HOMOGRAPH` 처리는 이번 범위의 알려진 한계다.** `REQ-EXT-005`가 이미 "MVP1은 축소
> 범위"라 적어 뒀으므로 1차 구현에서 건너뛰는 것이 과한 제약은 아니다. 실제로 그 정보가
> 필요해지면 `CandidateTerm`에 "표기 하나·뜻 여러 개" 표현을 추가하는 별도 결정이 필요하다
> — 그 전까지는 `제안`으로만 남긴다(루트 `CLAUDE.md`의 "미확정 항목은 임의로 확정하지
> 않는다"에 따름).

---

## 2. 태스크 목록

### Track A — 프론트엔드 목업 제거 (선행, 도메인별 병렬 가능)

| 태스크 | 범위 | 파일 |
|---|---|---|
| **T-INT-9** workspace 실연동 | `api/*` 3개 파일을 `httpClient`로, `model/types.ts`를 실제 응답 shape에 맞춤 | `frontend/src/features/workspace/**` |
| **T-INT-10** document 실연동 | **부분 완료**(2026-09-14, 5/10) — 목록·상세·버전이력·생성·라벨은 실연동. 대조 제안 클러스터(초안 목록·제안·이력·처리 4개)는 보류 — `DocumentReviewPage.tsx`의 본문 렌더링이 특정 문서에만 하드코딩돼 있어 API만 바꿔선 의미가 없다. `docs/task/T-INT-10-document.md` 참고 | `frontend/src/features/document/**` |
| **T-INT-11** dictionary 실연동 | 6개 api 파일(목록·검색·초안·후보어) | `frontend/src/features/dictionary/**` |
| **T-INT-12** review 실연동 | **보류**(2026-09-14) — 단순 API 연동이 아니라 프론트 화면 모델과 백엔드 도메인 모델 사이의 구조적 차이 3건(목록 N+1, 개정 diff 불가, 코멘트가 검토 제출 후에만 가능)이 나와 별도 세션에서 재설계 필요. `docs/task/T-INT-12-review.md` 참고 | `frontend/src/features/review/**` |
| **T-INT-13** notification 실연동 | **보류**(2026-09-14) — 대응하는 알림 "설정" API가 백엔드에 없다(`D-54`로 MVP1에서 채널 개념 자체를 제거). 목록/읽음 처리 API는 있지만 프론트 연동 코드가 아예 없어 원래 범위보다 커서 이번 트랙 A에서는 건너뛴다 | `frontend/src/features/notification/**` |
| **T-INT-14** member 잔여 실연동 | **보류**(2026-09-14) — `GET .../participants` 응답에 email/displayName이 없고 다른 회원을 id로 조회할 API도 없다. **T-INT-18**(백엔드 보강)이 선행돼야 한다 | `frontend/src/features/member/**` |
| **T-INT-15** 인증 가드 점검 | 라우터 주석의 "목업 가드" 서술이 낡았는지 확인(`authStore`는 이미 실 로그인과 연결된 것으로 보임) — 코드보다 주석 정정 가능성 높음 | `frontend/src/app/RequireAuth.tsx`, `router.tsx` |
| **T-INT-16** 환경설정 정리 | `.env.local.example` 추가, `VITE_API_BASE_URL` 로컬 설정 가이드 | `frontend/.env.local.example` |
| **T-INT-18** 회원 배치/단건 조회 API(백엔드) | **설계 확정**(2026-09-14) — `GET /api/members/{id}`(단건) + `GET /api/members?ids=`(배치) 신설. workspace·document·dictionary·draftdictionary·reviewrequest 5개 도메인이 공유하는 "행위자 id만 있고 이름 없음" 문제의 공용 해법. **T-INT-14의 선행**, T-INT-10 일부 보강. `docs/task/T-INT-18-*.md` 참고 | `backend/.../member/**` |

**패턴**: 대부분 "픽스처 반환 → `httpClient.get/post/patch/delete` 호출"로 함수 본문만
바꾸는 기계적 작업이다(`httpClient`가 인증·리프레시를 이미 처리). 응답 DTO가 목업 타입과
어긋나면 `model/types.ts`를 API 응답에 맞춰 고치고, 화면 컴포넌트는 최대한 안 건드린다.
`docs/API.md`의 해당 도메인 절이 실제 응답 shape의 정본이다. **실행 중 대응 백엔드
API가 아예 없는 항목이 나오면(T-INT-13·T-INT-14가 그 사례) 억지로 만들지 않고 보류
처리 후 `docs/task/`에 사유를 기록한다 — 자세한 경위는 각 `docs/task/T-INT-*.md` 참고.

### Track B — SQS↔FastAPI 실연동 (Track A 이후, 백엔드/독립 파일이라 병렬도 가능)

| 태스크 | 범위 |
|---|---|
| **T-INT-7** 큐 계약 문서화 | 위 1절을 `docs/plan/CONFLICTS.md`에 `D-62`로 등재. `application.yml`/`application-prod.yml`(+양쪽 test yml)의 주석 처리된 `llm-request-queue`/`llm-reply-queue` 키를 활성화 |
| **T-INT-8a** 추출 real 어댑터 | `SqsTermExtractorAdapter`(요청 발행) + `ExtractionReplyListener`(`@SqsListener`, 응답 수신 → `DraftDictionaryExtractionExecutionService.complete/fail` 직접 호출). **포트를 "발행만" 하는 형태로 바꾼다** — 응답이 비동기로 딴 곳에서 오므로 `TermExtractorPort.extract(...)`의 동기 반환값을 없앤다 |
| **T-INT-8b** 대조 real 어댑터 | 동일 패턴, `SqsTermCheckerAdapter` + `CheckReplyListener` |
| **T-INT-8c** 프로퍼티 전환 | `app.ai.extractor.mode`/`app.ai.checker.mode`에 `real` 옵션 추가(`@ConditionalOnProperty(havingValue="real")`), 기본값은 계속 `stub` |
| **T-INT-6** (잡일, 아무 때나) | `NotificationController` 4개 엔드포인트 `@RequestParam Long memberId` → `@AuthenticationPrincipal`(T-INT-3 잔여 마무리) |
| **T-INT-17** 추출/대조 결과 화면 | `TermExtractionPage`가 실제로 `POST .../extractions`를 호출하고 `jobId`로 폴링(`GET .../extractions/{jobId}`)하는 화면으로 교체. `DocumentReviewPage`의 대조 결과 표시도 동일 패턴. **T-INT-8a/8b가 먼저 끝나야 의미 있는 데이터가 나온다** — 그 전엔 붙여도 항상 빈 결과 |

---

## 3. 의존 그래프

```
T-INT-9~16 (Track A)              서로 독립, 병렬 가능
T-INT-6                           독립, 아무 때나
T-INT-7 → T-INT-8a·8b → T-INT-8c → T-INT-17
```

Track A와 Track B는 건드리는 파일이 겹치지 않아(프론트 vs 백엔드) 동시 진행 가능하지만,
우선순위상 Track A를 먼저 마친다.

---

## 4. 브랜치·커밋

- 프론트: `feat/WLSH-{티켓}-fe-{도메인}-real-api`(예: `feat/WLSH-xxx-fe-workspace-real-api`).
  태스크 하나 = PR 하나, 커밋은 함수 단위로 쪼개도 된다.
- 백엔드: `chore/WLSH-{티켓}-t-int-7`(문서), `feat/WLSH-{티켓}-t-int-8`(SQS 어댑터),
  `chore/WLSH-{티켓}-t-int-6`(NotificationController 잔여).
- ubidict-py: 별도 레포 — `task.md`에 "백엔드 실 스키마 도착, 재검토 완료" 기록만 하면
  된다. payload 구조 자체를 그대로 채택하므로 `queue_schema.py`의 "초안" 주석 제거 외에
  코드 변경은 거의 없을 것으로 예상.

---

## 5. 검증

- 백엔드: `./gradlew spotlessApply && ./gradlew check`. SQS 왕복은 기존
  `RealSqsRoundTripITest` 패턴을 참고해 수동/선택 통합 테스트로 둔다(실 AWS 비용·시간
  소요) — 기본 `check`에는 stub 경로만 포함.
- 프론트: 기존 테스트 방식 확인 후 필요 시 유닛 추가, 수동 QA(로그인 → 각 화면에서 실제
  백엔드 데이터 표시 확인).
- 3리포 E2E(Track B 완료 후): 워크스페이스 생성 → 문서 업로드 → "용어 추출 실행" → SQS →
  ubidict-py(실 Gemini) → 응답 → `ExtractionJob SUCCEEDED` → 사전 초안에 후보어 반영까지
  수동 확인. `backend/task/`(개인 추적 문서) 또는 이 문서에 체크리스트로 기록.
