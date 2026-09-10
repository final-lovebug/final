# DraftDocument 도메인 구현 계획

문서 초안(DraftDocument) 도메인의 구현 계획이다. `docs/DOMAIN.md`·`docs/ARCHITECTURE.md`·`docs/TEST.md`·`docs/API.md`·`docs/LOG.md`·`docs/EXCEPTION.md`를 먼저 읽고, 이 문서는 그 규칙을 이 도메인에 적용한 결과로 읽는다.

세 초안·리뷰 도메인 계획 문서는 같은 목차를 쓴다 — `DRAFT_DICTIONARY_PLAN.md`, `REVIEW_REQUEST_PLAN.md`.

**여러 세션으로 나눠 구현한다면 `EXECUTION_ORDER.md`를 먼저 읽는다.** 도메인을 가로지르는 태스크 순서, 공유 파일의 주인, 커밋 단위, 동시 실행 제약이 거기에 있다.

---

## 1. 범위와 목표

**담당 애그리게이트**: `DraftDocument`(루트), `SuggestionTerm`

**목표**: 사전집과 대조된 문서의 비표준 표현을 사람이 수용·거절해 교정본을 만들고, 교정이 끝나면 리뷰 요청으로 넘긴다.

**관련 요구사항**: `REQ-CHK-004`(대조 결과 화면 — 본문 하이라이트 + 우측 제안 목록), `REQ-UPD-001`(제안 수락 → 수정본 생성), `REQ-UPD-002`(제안 거절 + 사유 기록), `REQ-UPD-005`(문서별 제안 처리 이력 조회)

### MVP1 범위 밖

`docs/DOMAIN.md`에서 취소선 처리된 것은 **구현하지 않는다.**

| 대상 | 사유 | 대체 |
| --- | --- | --- |
| `DictionaryContrast`(사전집 대조) | 문서에 "1차 mvp에서는 없는 기능이라 제외"로 명시 | 초안을 **수동 생성 API**로 만든다 |
| `Examine`(교정 이력) | 폐기 | 처리 이력은 `SuggestionTerm.handledBy` + `updatedAt`으로 추적한다. **초안 단계에 회차(round) 개념을 두지 않는다**(`Reexamine.round`는 ReviewRequest의 별개 개념) |
| `DraftDocument.suggestions` 컬렉션 필드 | 표에서 취소선 | 컬렉션 매핑 없이 `SuggestionTerm`이 `draftDocumentId`로 단방향 참조 |

`docs/DOMAIN.md` 관계 표는 `Document (+Dictionary) --DictionaryContrast--> DraftDocument`로 쓰여 있으나, **MVP1의 실제 경로는 `Member --수동 생성--> DraftDocument`** 다. 관계 표에 "1차 MVP는 수동 생성" 각주를 반영했다(2026-09-10).

AI 대조가 나중에 붙어도 **초안 생성 API는 그대로 두고 제안어 공급자만 바뀐다.** 제안어를 만들어 넣는 지점은 `POST /api/draft-documents/{id}/suggestion-terms` 하나이므로, 대조 엔진은 이 유스케이스를 배치로 호출하면 된다.

---

## 2. 결정 대기표

### 2-1. 확정된 결정

| ID | 확정 내용 | DOMAIN.md 수정 제안 |
| --- | --- | --- |
| **D-9** | **교정 중에는 `draftBody`를 바꾸지 않는다.** 제안어 수용·거절은 `SuggestionTerm.status`만 전이시키고, **교정완료 시점에 수용된 제안어만 모아 본문을 뒤에서 앞으로 한 번에 치환**해 `draftBody`를 만든다 | `draftBody` 설명 "교정 반영이 **누적**되는 본문" → "교정완료 시 생성되는 본문" |
| **D-18** | 도메인 레벨 패키지 구조(`presentation`/`service`/`implement`/`infra`/`domain`/`exception`)만 지키고 하위 디렉터리는 담당자 판단. 기본값은 `presentation/dto/`·`service/model/`(Member 선례), **ErrorCode는 `{domain}/exception/`**(ARCHITECTURE.md 규정) | — |
| **D-8** | **초안 상태 축을 4상태로 두 도메인 통일** — `EXAMINING / EXAMINED / REVIEW_REQUESTED / REVISED`. 3상태를 택하면 `ReviewRequestRevisedEvent`를 받아도 반영할 상태가 없어 초안이 영구히 `리뷰요청됨`으로 남고, D-10의 "진행 중" 판정을 상태로 할 수 없다. 우회책인 "반영 시 소프트 삭제"는 이 저장소의 소프트 삭제가 메서드명 규약(`...AndDeletedAtIsNull`)이라 초안이 조회 경로에서 사라져 `REQ-UPD-005`(이력 조회)를 깨뜨린다 | `status` 값 목록을 4상태로 |
| **D-11** | **교정중이면 제안어 판정을 자유롭게 변경하고, 교정완료 이후에는 잠근다.** 감사 기록은 최종 상태 + `handledBy` + `updatedAt`으로 남기고 **변경 이력 엔티티를 두지 않는다.** D-9(교정 중에는 본문을 건드리지 않음)와 맞아떨어진다 | 정책 절에 잠금 규칙 추가 |
| **D-10** | **애플리케이션 검증만 한다.** "진행 중"은 `status != REVISED` AND `deleted_at is null`이다(D-8 4상태 덕분에 상태 하나로 판정된다). `UNIQUE(document_id)`를 걸지 않는다 — 소프트 삭제를 쓰는데 MySQL 8.4에 부분 유니크 인덱스가 없고, `deleted_at`을 `NOT NULL`로 바꾸는 안은 `common/domain/BaseEntity` 변경을 수반해 전 도메인에 영향을 준다 | 정책 절에 "애플리케이션 검증" 명시 |
| **D-17** | **`docs/API.md`에 페이징·정렬 규격과 버저닝 방침을 신설한다**(10절 규약과 동일). 버저닝은 `/api` 유지 | — |
| **D-16** | **`docs/ARCHITECTURE.md`를 수정하지 않는다.** 나열된 애노테이션은 예시이고 핵심은 "JPA 매핑 애노테이션은 허용한다 / Spring·Web 의존은 두지 않는다"다 | — |
| **포트 소유** | **각 소비 도메인이 포트를 정의하고 자기 어댑터까지 구현한다.** 어댑터는 `{domain}/infra/adapter/`에 두고 제공 도메인의 `infra`(Repository)만 참조한다 — 같은 레이어끼리라 방향 위반이 아니다. 제공 도메인의 `implement`를 참조하면 `infra -> implement` 역방향이 된다 | — |

**D-9이 이 도메인의 가장 중요한 결정이다.** 수용 즉시 본문을 치환하면 뒤에 있는 모든 `SuggestionTerm.anchor` 오프셋이 길이 차만큼 밀려, shift 계산과 겹침 무효화가 필요해진다. 교정완료 시 일괄 생성으로 정하면 **anchor는 끝까지 원본 기준으로 유지**되고 그 로직 전체가 사라진다.

### 2-2. 남은 결정 대기

**없다. 2026-09-10에 전건 확정했다.** 모든 Phase에 착수할 수 있다.

`docs/DOMAIN.md` 미확정 블록 중 이 도메인에 걸리는 것은 없다. `Document`의 라벨·outdated 속성은 Document 도메인 몫이다.

---

## 3. 도메인 모델

### DraftDocument (애그리게이트 루트)

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | `@GeneratedValue(IDENTITY)` |
| `documentId` | `Long` | `document_id` | X | 논리 FK — 크로스 도메인이라 DB 제약 없음 |
| `baseVersionNo` | `int` | `base_version_no` | X | 어느 문서 버전에서 갈라졌는지 |
| `draftBody` | `String` | `draft_body` | X | `TEXT`. **교정완료 시 생성**(D-9) |
| `status` | `DraftDocumentStatus` | `status` | X | `@Enumerated(STRING)` |
| `requestedBy` | `Long` | `requested_by` | O | 교정할 사람. DOMAIN.md에서 선택 |
| `createdBy` | `Long` | `created_by` | X | `updatable = false`. 정적 팩토리 인자로 받는다 |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | `BaseEntity` 상속 |

`suggestions` 컬렉션 필드는 두지 않는다(1절).

### SuggestionTerm

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `draftDocumentId` | `Long` | `draft_document_id` | X | **같은 도메인이라 DB FK를 건다** |
| `anchor` | `TextRange` | `start_offset`, `end_offset` | X | `@Embedded`. `common/domain/TextRange`(10절) |
| `originTerm` | `String` | `origin_term` | X | 문서에 실제로 쓰인 비표준 표현 |
| `suggestionTerm` | `String` | `suggestion_term` | X | 사전집의 대표어를 **참조해 채운 값**. 사전집 표기를 그대로 복사해 저장하지 않는다는 DOMAIN.md 주의는 MVP1에서 Dictionary 도메인이 없어 검증할 수 없다 → 13절 |
| `status` | `SuggestionTermStatus` | `status` | X | |
| `handledBy` | `Long` | `handled_by` | O | 처리자 |
| `rejectReason` | `String` | `reject_reason` | O | `TEXT`. 거절 시 필수(4절) |
| `createdBy` | `Long` | `created_by` | X | |

### 애그리게이트 경계

`DraftDocument`와 `SuggestionTerm`은 **같은 애그리게이트**지만 JPA 컬렉션으로 묶지 않는다. 제안어 개수가 문서 길이에 비례해 늘어나므로 루트에서 전부 로딩하면 목록·페이징이 불가능해진다. 대신

- 조회는 `SuggestionTermRepository`로 직접 한다(`implement`가 두 Reader를 함께 쓴다).
- 불변식(초안이 교정완료면 제안어를 바꿀 수 없다)은 `SuggestionTermProcessor`(implement)가 두 도메인 객체를 함께 받아 검증한다.
- 제안어 단독 삭제 API는 두지 않는다. 루트를 삭제하면 제안어는 DB FK `ON DELETE CASCADE` 없이 **애플리케이션에서 함께 지운다**(소프트 삭제는 루트만, 10절 규약).

### enum

```
DraftDocumentStatus { EXAMINING, EXAMINED, REVIEW_REQUESTED, REVISED }  // 교정중 / 교정완료 / 리뷰요청됨 / 반영완료
SuggestionTermStatus { PENDING, KEPT_ORIGIN, APPLIED_SUGGESTION }  // 처리 전 / 기존 용어 / 제안 용어
```

`KEPT_ORIGIN`(기존 용어 유지 = 거절)과 `APPLIED_SUGGESTION`(제안 용어 채택 = 수용)은 DOMAIN.md의 한국어 값을 그대로 옮긴 것이다. `REJECTED`/`ACCEPTED`로 바꾸지 않는다 — 유비쿼터스 언어를 코드 이름으로 유지한다.

---

## 4. 상태 전이와 도메인 메서드

### DraftDocumentStatus

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (신규) | `EXAMINING` | `DraftDocument.create(...)` | — |
| `EXAMINING` | `EXAMINED` | `markExamined(composedBody)` | **미처리(`PENDING`) 제안어가 0건**. 위반 시 `DRAFT_DOCUMENT_SUGGESTION_TERM_UNHANDLED_EXISTS`(409) |
| `EXAMINED` | `REVIEW_REQUESTED` | `markReviewRequested()` | `ReviewRequestCreatedEvent` 수신(Phase 4). 그 전까지는 도달하지 않는다 |
| `REVIEW_REQUESTED` | `EXAMINED` | `reopen()` | `ReviewRequestCanceledEvent` 수신. 교정완료 상태를 유지하므로 **다시 리뷰를 요청할 수 있다** |
| `REVIEW_REQUESTED` | `REVISED` | `markRevised()` | `ReviewRequestRevisedEvent` 수신. **종단** |
| `REVISED` | — | — | 종단. 이력으로만 남는다 |

**`REVIEW_REQUESTED → EXAMINING`은 이 도메인에 없다.** 재교정 본문은 리뷰 요청 쪽이 `PerformReexamineRequest{proposedBody}`로 직접 받으므로 초안을 다시 열지 않는다. `DraftDictionary`는 후보어 판정을 고쳐야 하므로 그 전이를 갖는다 — 상태 축은 통일하되 전이는 도메인별로 다르다.

Phase 3까지는 `EXAMINING`과 `EXAMINED` 둘만 실제로 쓰인다. 뒤의 두 상태는 Phase 4 이벤트 수신으로 도달한다.

### SuggestionTermStatus

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (신규) | `PENDING` | 생성 | — |
| `PENDING` | `APPLIED_SUGGESTION` | `accept(handlerId)` | 초안이 `EXAMINING` |
| `PENDING` | `KEPT_ORIGIN` | `reject(handlerId, rejectReason)` | 초안이 `EXAMINING`, **사유 필수** |
| 처리됨 | 다른 상태 | `accept` / `reject` 재호출 | **초안이 `EXAMINING`이면 허용한다**(D-11). `EXAMINED` 이후에는 `DRAFT_DOCUMENT_ALREADY_EXAMINED`(409) |

> DOMAIN.md는 `rejectReason`을 선택(X)으로 표기하지만 `REQ-UPD-002`가 "거절 사유를 기록"을 요구하고 흐름 설명도 사유를 전제한다. **거절 시에는 필수**로 검증하고, 필드 자체는 수용된 제안어에서 비므로 컬럼은 nullable로 둔다. 이 해석은 `docs/DOMAIN.md`에 반영했다(2026-09-10).

### 도메인 메서드 시그니처

```java
// DraftDocument
static DraftDocument create(Long documentId, int baseVersionNo, String draftBody, Long requestedBy, Long createdBy);
void markExamined(String composedBody);   // 상태 전이 + draftBody 확정
void markReviewRequested();               // P4 — 이벤트 수신
void reopen();                            // P4 — 리뷰 취소 시 EXAMINED로
void markRevised();                       // P4 — 종단
boolean isExamining();
boolean isExamined();
boolean isRevised();

// SuggestionTerm
static SuggestionTerm create(Long draftDocumentId, TextRange anchor, String originTerm, String suggestionTerm, Long createdBy);
void accept(Long handlerId);
void reject(Long handlerId, String rejectReason);
void changeAnchor(TextRange anchor);
boolean isPending();
boolean isApplied();
```

`markExamined`가 본문을 **인자로 받는다.** 조립은 `DraftBodyComposer`(implement)가 하고 도메인 객체는 결과만 받는다 — 도메인이 제안어 컬렉션을 들고 있지 않으므로 스스로 조립할 수 없다.

---

## 5. 스키마와 Flyway

**대역: 400–499** (`backend/CLAUDE.md`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 | Phase |
| --- | --- | --- |
| `V400__create_draft_document.sql` | `draft_document`, `suggestion_term` 두 테이블 | 1 |
| `V410__add_draft_document_document_id_index.sql` | 필요 시 인덱스 보강 | 2 |

```sql
create table draft_document (
    id              bigint      not null auto_increment,
    document_id     bigint      not null,
    base_version_no int         not null,
    draft_body      text        not null,
    status          varchar(20) not null,
    requested_by    bigint,
    created_by      bigint      not null,
    created_at      datetime(6) not null,
    updated_at      datetime(6) not null,
    deleted_at      datetime(6),
    primary key (id)
);

create index idx_draft_document_document on draft_document (document_id);

create table suggestion_term (
    id                bigint       not null auto_increment,
    draft_document_id bigint       not null,
    start_offset      int          not null,
    end_offset        int          not null,
    origin_term       varchar(255) not null,
    suggestion_term   varchar(255) not null,
    status            varchar(20)  not null,
    handled_by        bigint,
    reject_reason     text,
    created_by        bigint       not null,
    created_at        datetime(6)  not null,
    updated_at        datetime(6)  not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_suggestion_term_draft_document
        foreign key (draft_document_id) references draft_document (id)
);

create index idx_suggestion_term_draft_document_status
    on suggestion_term (draft_document_id, status);
```

- `document_id`에는 **FK 제약을 걸지 않는다**(크로스 도메인 논리 FK). `draft_document_id`는 같은 도메인이라 건다 — `fk_participant_workspace` 선례.
- **`UNIQUE(document_id)`를 걸지 않는다.** 소프트 삭제와 충돌하기 때문이다. "진행 중 초안 1개"는 `DraftDocumentCreationPolicyValidator`가 `status != REVISED` AND `deleted_at is null`로 검증한다(D-10).
- 마이그레이션은 **Phase 1 필수 산출물**이다. `support/RepositoryTestSupport`가 Flyway로 스키마를 만들므로 마이그레이션 없이는 Repository 테스트가 테이블을 찾지 못한다. `ddl-auto=create-drop` + `flyway.enabled=false` 우회는 쓰지 않는다.

---

## 6. 패키지와 파일

```
com.ubidict.backend.draftdocument
├── presentation
│   ├── DraftDocumentController.java          P1
│   ├── SuggestionTermController.java         P2
│   └── dto
│       ├── CreateDraftDocumentRequest.java   P1
│       ├── UpdateDraftBodyRequest.java       P1
│       ├── DraftDocumentResponse.java        P1
│       ├── AddSuggestionTermRequest.java     P2
│       ├── EditSuggestionTermRequest.java    P2
│       ├── TextRangeRequest.java             P2
│       ├── SuggestionTermResponse.java       P2
│       ├── RejectSuggestionTermRequest.java  P3
│       └── ExamineProgressResponse.java      P3
├── service
│   ├── DraftDocumentService.java             P1
│   ├── SuggestionTermService.java            P2
│   └── model
│       ├── CreateDraftDocumentCommand.java   P1
│       ├── UpdateDraftBodyCommand.java       P1
│       ├── DraftDocumentResult.java          P1
│       ├── AddSuggestionTermCommand.java     P2
│       ├── EditSuggestionTermCommand.java    P2
│       ├── SuggestionTermSearchQuery.java    P2
│       ├── SuggestionTermResult.java         P2
│       ├── DraftDocumentSearchQuery.java     P2
│       ├── AcceptSuggestionTermCommand.java  P3
│       ├── RejectSuggestionTermCommand.java  P3
│       ├── CompleteExamineCommand.java       P3
│       └── ExamineProgressResult.java        P3
├── implement
│   ├── DraftDocumentReader.java              P1
│   ├── DraftDocumentWriter.java              P1
│   ├── DraftDocumentRemover.java             P1
│   ├── SuggestionTermReader.java             P2
│   ├── SuggestionTermWriter.java             P2
│   ├── SuggestionTermOwnershipValidator.java P2
│   ├── SuggestionTermProcessor.java          P3
│   ├── DraftBodyComposer.java                P3
│   ├── DraftDocumentCreationPolicyValidator.java  P4
│   └── DraftDocumentEventPublisher.java      P4
├── infra
│   ├── DraftDocumentRepository.java          P1
│   ├── SuggestionTermRepository.java         P2
│   └── port
│       ├── ReviewRequestQueryPort.java       P4
│       ├── DocumentQueryPort.java            P4
│       ├── WorkspacePolicyPort.java          P4
│       ├── DocumentSnapshot.java             P4
│       └── stub
│           ├── StubReviewRequestQueryPort.java  P4
│           ├── StubDocumentQueryPort.java       P4
│           └── StubWorkspacePolicyPort.java     P4
├── domain
│   ├── DraftDocument.java                    P1
│   ├── DraftDocumentStatus.java              P1
│   ├── SuggestionTerm.java                   P2
│   ├── SuggestionTermStatus.java             P2
│   └── event
│       ├── DraftDocumentCreatedEvent.java    P4
│       └── DraftDocumentExaminedEvent.java   P4
└── exception
    └── DraftDocumentErrorCode.java           P1
```

`MemberWriter`가 Creator·Updater를 합친 선례를 따라 `DraftDocumentWriter` 하나로 생성·수정을 담는다. Reader/Writer/Remover 세 개면 이 도메인 규모에 충분하다.

---

## 7. API 명세

접두사 `/api`(버저닝 없음). 컨트롤러는 **`ResponseEntity<T>`를 반환**하고 `@ResponseStatus`를 쓰지 않는다. **Swagger 애노테이션은 쓰지 않는다**(명세는 `docs/API.md`가 담당).

요청자 식별은 **`@RequestParam Long memberId`** 다. 인증 계층(`NFR-USR-001`)이 없어 생긴 임시 방식이므로 컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 **"인증 도입 전까지 운영 배포 대상이 아니다"**를 적는다 — `WorkspaceController` 선례.

| Phase | Method | Path | 요청 | 응답 | 성공 | 에러 | 태그 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/api/draft-documents` | `CreateDraftDocumentRequest{documentId, baseVersionNo, draftBody}` | `DraftDocumentResponse` | 201 | 400 `COMMON_INVALID_REQUEST` | **SHRINK** |
| 1 | GET | `/api/draft-documents/{draftDocumentId}` | — | `DraftDocumentResponse` | 200 | 404 `DRAFT_DOCUMENT_NOT_FOUND` | KEEP |
| 1 | PATCH | `/api/draft-documents/{draftDocumentId}` | `UpdateDraftBodyRequest{draftBody}` | `DraftDocumentResponse` | 200 | 404 | KEEP |
| 1 | DELETE | `/api/draft-documents/{draftDocumentId}` | — | — | 204 | 404 | KEEP |
| 2 | GET | `/api/draft-documents` | `documentId?`, `status?`, `page`, `size`, `sort` | `PageResponse<DraftDocumentResponse>` | 200 | 400 | KEEP |
| 2 | POST | `/api/draft-documents/{id}/suggestion-terms` | `AddSuggestionTermRequest{anchor{startOffset,endOffset}, originTerm, suggestionTerm}` | `SuggestionTermResponse` | 201 | 404, 400 `DRAFT_DOCUMENT_INVALID_ANCHOR` | KEEP |
| 2 | GET | `/api/draft-documents/{id}/suggestion-terms` | `status?`, `page`, `size`, `sort` | `PageResponse<SuggestionTermResponse>` | 200 | 404 | KEEP |
| 2 | PATCH | `/api/suggestion-terms/{suggestionTermId}` | `EditSuggestionTermRequest{anchor?, originTerm?, suggestionTerm?}` | `SuggestionTermResponse` | 200 | 404 `DRAFT_DOCUMENT_SUGGESTION_TERM_NOT_FOUND` | KEEP |
| 2 | DELETE | `/api/suggestion-terms/{suggestionTermId}` | — | — | 204 | 404 | KEEP |
| 3 | POST | `/api/suggestion-terms/{id}/acceptance` | — | `SuggestionTermResponse` | 200 | 409 `DRAFT_DOCUMENT_ALREADY_EXAMINED` | KEEP |
| 3 | POST | `/api/suggestion-terms/{id}/rejection` | `RejectSuggestionTermRequest{rejectReason}` | `SuggestionTermResponse` | 200 | 409, 400 `DRAFT_DOCUMENT_REJECT_REASON_REQUIRED` | KEEP |
| 3 | POST | `/api/draft-documents/{id}/examine-completion` | — | `DraftDocumentResponse` | 200 | 409 `..._UNHANDLED_EXISTS`, 409 `..._ALREADY_EXAMINED` | KEEP |
| 3 | GET | `/api/draft-documents/{id}/examine-progress` | — | `ExamineProgressResponse{total, pending, keptOrigin, appliedSuggestion, previewBody}` | 200 | 404 | KEEP |

**임시 API 태그** — `POST /api/draft-documents`는 **SHRINK**다. MVP1에는 대조가 없어 사람이 초안을 시작하는 유일한 경로지만, `draftBody`는 원래 `Document.content` 복사여야 한다. Phase 4에 `DocumentQueryPort`가 붙으면 **`draftBody` 필드를 요청에서 제거하고 서버가 `documentId` + `baseVersionNo`로 파생**한다. 그때까지 이 필드에 `@Schema(description = "Phase 4에 서버 파생으로 전환 예정", deprecated = true)`를 달아 프론트가 의존하기 전에 신호를 준다.

경로에 `/manual`·`/temp` 같은 세그먼트를 붙이지 않는다. 임시성은 경로가 아니라 `deprecated` 플래그와 이 문서로 표현한다 — KEEP 대상의 프론트 경로를 나중에 다시 갈지 않기 위해서다.

`GET .../examine-progress`의 `previewBody`는 D-9 확정의 결과다. 교정 중에는 `draftBody`가 원본이므로, 지금까지 수용한 제안어를 반영한 본문을 **저장하지 않고 조립해서** 응답한다.

### 권한

초안은 문서에 딸린 리소스이므로 **워크스페이스 스코프 리소스**다. 이 도메인은 `workspaceId`를 직접 갖지 않고 `documentId`를 통해 간접적으로 속하므로, Phase 4에 `DocumentQueryPort`로 `workspaceId`를 얻어 `WorkspacePolicyPort`로 검사한다.

| 대상 | 규칙 |
| --- | --- |
| 조회·제안어 처리·교정완료 | 워크스페이스 참여자. **참여자가 아니면 403이 아니라 404** — 403은 리소스 존재를 드러낸다(`WorkspaceAccessValidator` 선례) |
| 초안 생성·삭제 | 참여자면 가능하다. `docs/DOMAIN.md`는 "Regular는 문서 작성·교정·리뷰까지 가능"이라 하므로 교정은 권한 제한이 없다. **문서 삭제**는 Admin 이상이지만 그건 Document 도메인 몫이고 초안 삭제와 다르다 |

**Phase 1~3은 권한 검사를 하지 않는다.** `memberId`가 요청 파라미터라 어차피 신뢰할 수 없고(인증 전까지 운영 배포 대상이 아니다), Phase 4에 포트가 붙을 때 한 번에 넣는다.

---

## 8. ErrorCode

`draftdocument/exception/DraftDocumentErrorCode.java`. `CommonErrorCode`와 같은 형태(`@RequiredArgsConstructor enum ... implements ErrorCode`, 상수가 `HttpStatus`를 보유)로 만든다.

| 상수 | status | message | Phase |
| --- | --- | --- | --- |
| `DRAFT_DOCUMENT_NOT_FOUND` | 404 | 문서 초안을 찾을 수 없습니다. | 1 |
| `DRAFT_DOCUMENT_INVALID_BODY` | 400 | 초안 본문은 비어 있을 수 없습니다. | 1 |
| `DRAFT_DOCUMENT_INVALID_BASE_VERSION` | 400 | 기준 문서 버전이 올바르지 않습니다. | 1 |
| `DRAFT_DOCUMENT_SUGGESTION_TERM_NOT_FOUND` | 404 | 제안어를 찾을 수 없습니다. | 2 |
| `DRAFT_DOCUMENT_INVALID_ANCHOR` | 400 | 제안어 위치가 올바르지 않습니다. | 2 |
| `DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY` | 400 | 제안어 위치가 본문 범위를 벗어났습니다. | 2 |
| `DRAFT_DOCUMENT_SUGGESTION_TERM_MISMATCHED` | 400 | 해당 초안의 제안어가 아닙니다. | 2 |
| `DRAFT_DOCUMENT_REJECT_REASON_REQUIRED` | 400 | 거절 사유는 필수입니다. | 3 |
| `DRAFT_DOCUMENT_SUGGESTION_TERM_UNHANDLED_EXISTS` | 409 | 처리하지 않은 제안어가 남아 있습니다. | 3 |
| `DRAFT_DOCUMENT_ALREADY_EXAMINED` | 409 | 이미 교정을 완료한 초안입니다. | 3 |
| `DRAFT_DOCUMENT_ALREADY_EXISTS` | 409 | 해당 문서에 이미 초안이 있습니다. | 4 |
| `DRAFT_DOCUMENT_UNDER_REVIEW` | 409 | 리뷰가 진행 중인 문서에는 초안을 만들 수 없습니다. | 4 |
| `DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND` | 404 | 대상 문서를 찾을 수 없습니다. | 4 |

`ANCHOR_STALE` 계열 코드는 **두지 않는다** — D-9 확정으로 본문이 교정 중에 바뀌지 않으므로 앵커가 무효화될 일이 없다.

`SUGGESTION_TERM_ALREADY_HANDLED`도 **두지 않는다** — D-11 확정으로 교정중에는 판정을 자유롭게 바꿀 수 있으므로 "이미 처리했다"가 거절 사유가 되지 않는다. 유일한 관문은 초안 상태이고 그건 `ALREADY_EXAMINED`가 담당한다.

---

## 9. 크로스 도메인 계약

### 소비 포트 (이 도메인이 정의하고 스텁까지 제공)

| 포트 | 시그니처 | 제공 도메인 | 스텁 동작 | Phase |
| --- | --- | --- | --- | --- |
| `ReviewRequestQueryPort` | `boolean hasOngoingDocumentReview(Long documentId)` | ReviewRequest | `false` | 4 |
| `DocumentQueryPort` | `Optional<DocumentSnapshot> read(Long documentId)` | Document(미착수) | 고정 스냅샷 | 4 |
| `WorkspacePolicyPort` | `boolean isParticipant(Long workspaceId, Long memberId)` | Workspace(**머지됨**) | `true` | 4 |
| `DocumentQueryPort` | `boolean isOutdated(Long documentId)` | Document(미착수) | `false`(D-15) | 4 |

`DocumentSnapshot`은 `record DocumentSnapshot(Long workspaceId, String title, String content, int currentVersionNo)`. **엔티티를 포트 시그니처에 노출하지 않는다.**

### 어댑터 배치 (2026-09-10 확정)

**포트는 우리가 정의하고 어댑터도 우리가 구현한다.** 어댑터는 `draftdocument/infra/adapter/`에 두고 **제공 도메인의 `infra`(Repository)만 참조한다** — 같은 레이어끼리라 방향 위반이 아니다. 제공 도메인의 `implement`(`ParticipantReader` 등)를 참조하면 `infra -> implement`가 되어 `docs/ARCHITECTURE.md`의 역방향 참조 금지를 어긴다. 이 규약 덕분에 **`workspace` 패키지의 파일을 한 줄도 고치지 않는다.**

`WorkspacePolicyPort`의 실제 어댑터는 `workspace/infra/ParticipantRepository`를 참조한다. 이 도메인은 `workspaceId`를 직접 갖지 않으므로 **`DocumentQueryPort`로 얻은 `workspaceId`를 넘겨** 호출한다.

어댑터 선택은 프로퍼티로 한다 — `app.crossdomain.{name}.mode=stub|real`(기본 `stub`, `matchIfMissing = true`). `InMemoryEventPublisher`의 `@ConditionalOnProperty` 패턴을 그대로 따르고, 컴포넌트 스캔 순서에 좌우되는 `@ConditionalOnMissingBean`은 쓰지 않는다.

**스텁 해제 조건**: ReviewRequest Phase 1이 develop에 머지되고 `reviewrequest`가 어댑터를 제공하면 `mode=real`로 전환한다(통합 태스크 `T-INT-2`).

### 정책의 이중 방어

"리뷰 요청 중이면 초안을 만들 수 없다"는 정책의 **1차 방어선은 이 도메인의 자기 상태**(`REVIEW_REQUESTED`)이고, 포트 질의는 2차 방어선이다. 이 이중화 덕분에 스텁 상태에서도 대부분의 시나리오가 동작한다.

D-8을 4상태로 확정한 덕에 **"한 문서당 진행 중 초안 1개"(D-10)도 상태 하나로 판정된다** — `status != REVISED` AND `deleted_at is null`. 3상태였다면 반영이 끝난 초안이 `REVIEW_REQUESTED`에 머물러 포트를 다시 조회해야 했다.

### 발행 이벤트

| 이벤트 | 페이로드 | 수신자 |
| --- | --- | --- |
| `DraftDocumentCreatedEvent` | `(Long draftDocumentId, Long documentId, int baseVersionNo, Long requestedBy, OffsetDateTime occurredAt)` | 없음(Notification 미정) |
| `DraftDocumentExaminedEvent` | `(Long draftDocumentId, Long documentId, Long examinedBy, OffsetDateTime occurredAt)` | 없음(Notification 미정) |

**불변 record, 식별자·원시값·시각만 담는다.** 도메인 모델이 곧 JPA 엔티티이므로 엔티티·지연 로딩 프록시·연관 컬렉션을 담으면 안 된다. 인메모리 어댑터는 참조를 그대로 넘겨 로컬에서 위반이 드러나지 않으므로(`docs/ARCHITECTURE.md`) **리뷰에서 확인한다.**

`implement`는 `common/infra/event/EventPublisher` 포트만 주입받는다. `ApplicationEventPublisher`를 직접 주입하지 않는다.

### 수신 이벤트

| 이벤트 | 발행자 | 처리 | Phase |
| --- | --- | --- | --- |
| `ReviewRequestCreatedEvent` | ReviewRequest | `markReviewRequested()` — `EXAMINED → REVIEW_REQUESTED` | 4 |
| `ReviewRequestRevisedEvent` | ReviewRequest | `markRevised()` — `REVIEW_REQUESTED → REVISED`(종단) | 4 |
| `ReviewRequestCanceledEvent` | ReviewRequest | `reopen()` — `REVIEW_REQUESTED → EXAMINED`. 다시 요청할 수 있다 | 4 |

**`ReviewRequestChangesRequestedEvent`는 이 도메인이 받지 않는다.** 재교정 본문은 리뷰 요청 쪽이 직접 받으므로 초안을 다시 열 필요가 없다(4절).

핸들러는 **멱등**하게 만든다(at-least-once 전제). 같은 이벤트를 두 번 받아도 상태가 한 번만 바뀌어야 한다. 리스너 어댑터는 수신 애노테이션만 담당하고 처리 로직은 공용 핸들러에 위임한다.

---

## 10. 공유 파일 규약

### 공유 자산과 주인

| 자산 | 최초 필요 | 규약 |
| --- | --- | --- |
| `common/domain/TextRange` | **이 도메인 Phase 2** — 우리가 만든다 | `@Embeddable` **record**(`@Entity`만 record 불가, `RuleSet` 선례). 필드 `startOffset`/`endOffset`, 컬럼 `start_offset`/`end_offset`, 생성자에서 `startOffset <= endOffset`·음수 아님 검증. **ReviewRequest의 `Comment.anchor`가 재사용**하므로 만든 즉시 develop에 올린다 |
| `common/presentation/PageResponse`, `common/service/PageResult` | **DraftDocument (DD-2)** — 선착순이 아니다(`EXECUTION_ORDER.md` 3절) | 요청 `page`(0-base) / `size`(기본 20, 최대 100) / `sort=필드,방향`(화이트리스트 외 400). 응답 JSON 키 `content`/`page`/`size`/`totalElements`/`totalPages`. **service는 Spring `Page`를 반환하지 않는다** — infra 타입이 상위 레이어로 새는 것 |

### 수정 금지 파일

`common/**`(위 2건 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`. 필요하면 통합 태스크로 넘기고 PR에 이유를 적는다.

### git worktree 운영

작업은 `../final-draftdocument` worktree에서 한다. 워킹 디렉터리는 분리되지만 `.git`·Docker·호스트 포트는 공유된다.

| 항목 | 규칙 | 이유 |
| --- | --- | --- |
| 브랜치명 | `feat/WLSH-{티켓}-draftdocument` | `.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다. 패턴이 없으면 티켓 추적이 끊긴다 |
| **`bootRun` 동시 실행 금지** | 한 번에 한 worktree만 | `spring-boot-docker-compose`가 worktree마다 별도 Compose 프로젝트(디렉터리명 기준)를 띄운다. MySQL·MongoDB·Redis·LGTM이 3배로 뜨고 호스트 포트 8080·3000이 겹쳐 `port is already allocated`로 죽는다 |
| **`./gradlew test` 동시 실행 주의** | 세 worktree에서 동시에 돌리지 않는다 | `RepositoryTestSupport`가 MySQL 컨테이너를, `IntegrationTestSupport`가 `TestcontainersConfiguration`(MySQL+Mongo+Redis+LGTM)을 띄운다. worktree 3개면 Gradle 데몬도 3개다. 루트 `CLAUDE.md`는 LGTM과 빌드 JVM이 겹치면 Docker Desktop 기본 메모리에서 OOM으로 죽는다고 기록한다 |
| develop 동기화 | 공유 자산이 올라온 직후, 다른 도메인 Phase 1이 머지된 직후, `refacotr/WLSH-86-base-entity`(BaseEntity 감사 애노테이션 교체)가 머지된 직후 **즉시 머지** | 규약이 어긋난 채 오래 진행하면 나중에 세 브랜치를 동시에 고쳐야 한다 |
| 파일 소유권 | `draftdocument/**` + Flyway 400–499 + 위 공유 자산 2건 | worktree는 편집 충돌을 숨기고 머지 시점에 되살린다 |

---

## 11. 테스트 계획

| 계층 | 방식 |
| --- | --- |
| Unit | 도메인·implement를 `new`로 생성. 저장 전이라 식별자가 `null`인 점에 유의 |
| Repository | `support/RepositoryTestSupport` 상속. **Flyway가 스키마를 만든다.** `TestEntityManager`(`em`) 사용 |
| Service | `support/IntegrationTestSupport` 상속. **개별 테스트에 `@Transactional`을 붙이지 않는다**(아래 경고) |
| Controller | `@AutoConfigureMockMvc(addFilters = false)` + `@WebMvcTest(XxxController.class)` + `@MockitoBean` + `RestAssuredMockMvc`. `MockMvc.perform()` 금지 |

> **경고 — 개별 테스트에 `@Transactional`을 붙이지 않는다.** `IntegrationTestSupport`는 롤백 대신 `DbCleaner`로 테이블을 비운다. 테스트 트랜잭션이 서비스가 선언한 트랜잭션 경계를 덮어써 경계 자체를 검증할 수 없기 때문이다. 이 방식 덕분에 Phase 4의 `@TransactionalEventListener(AFTER_COMMIT)` 검증도 그대로 동작한다 — `@Transactional`을 붙이면 리스너가 아예 실행되지 않는다.

> **테스트 전용 엔티티를 만들지 않는다.** `DbCleaner`는 `information_schema`에서 실제 테이블을 읽으므로, 마이그레이션에 없는 테이블이 생기면 정리 대상이 어긋난다.

AssertJ를 쓴다(`assertThat`, `assertThatThrownBy`). JUnit `assertEquals`·`assertThrows`는 쓰지 않는다. Mockito는 BDD 스타일(`given(...).willReturn(...)`). `@DisplayName`은 한글 `~다.` 어투로 사용자 행위·비즈니스 규칙을 쓰고, 메서드명은 영어로, 실패 케이스는 `{메서드명}_{실패이유}` 형식이다. given/when/then 주석으로 구역을 나누고 when에는 검증 대상 행위 하나만 둔다.

### 테스트 클래스와 필수 케이스

| 클래스 | 계층 | 필수 케이스 | Phase |
| --- | --- | --- | --- |
| `domain/DraftDocumentTest` | Unit | `create`("초안을 생성하면 교정중 상태로 시작한다.") / `create_bodyIsBlank` / `markExamined_alreadyExamined` / **`reopen_returnsToExamined`**("리뷰가 취소되면 교정완료로 돌아간다.") / **`markRevised_isTerminal`**("반영완료된 초안은 상태를 바꿀 수 없다.") | 1·4 |
| `infra/DraftDocumentRepositoryTest` | Repository | `findByDocumentId` / `save_auditingFieldsAreSet` / `findById_deleted`("삭제된 초안은 조회되지 않는다.") | 1 |
| `implement/DraftDocumentReaderTest` | Unit | `read_notFound` | 1 |
| `service/DraftDocumentServiceTest` | Service | `create` / `updateBody` / `delete` / `updateBody_draftDocumentNotFound` | 1 |
| `presentation/DraftDocumentControllerTest` | Controller | `create`(201) / `create_documentIdIsNull`(400) / `read_notFound`(404 + 코드 확인) | 1 |
| `common/domain/TextRangeTest` | Unit | `create_endBeforeStart` / `create_negativeOffset` | 2 |
| `domain/SuggestionTermTest` | Unit | `create`("제안어를 생성하면 처리 전 상태로 시작한다.") / `accept` / `reject_reasonIsBlank` / **`accept_afterReject`**("교정 중이면 거절한 제안어를 다시 수용할 수 있다." — D-11) | 2·3 |
| `infra/SuggestionTermRepositoryTest` | Repository | `findByDraftDocumentIdAndStatus`("상태로 제안어를 필터링해 조회한다.") / `countPendingByDraftDocumentId` / `save_persistsAnchor`(`@Embedded` 매핑) / 정렬 검증 | 2 |
| `implement/SuggestionTermOwnershipValidatorTest` | Unit | `validate_mismatched`("다른 초안의 제안어를 지정하면 예외가 발생한다.") | 2 |
| **`implement/DraftBodyComposerTest`** | Unit | **`compose_appliesFromTailToHead`**("여러 제안어를 반영해도 앞쪽 위치가 정확히 치환된다.") / **`compose_overlappingAnchors`** / `compose_noAcceptedSuggestion`("수용한 제안어가 없으면 본문이 그대로다.") | 3 |
| `service/SuggestionTermDecisionServiceTest` | Service | `accept` / `reject`("거절하면 본문이 유지되고 사유가 기록된다.") / `accept_afterExamined`(409) | 3 |
| `service/DraftDocumentExamineServiceTest` | Service | `completeExamine`("교정을 완료하면 수용한 제안어가 본문에 반영된다.") / `completeExamine_pendingExists`(409) | 3 |
| `presentation/SuggestionTermControllerTest` | Controller | `add`(201) / `add_originTermIsBlank`(400) / `search_returnsPageFormat` / `reject_reasonIsBlank`(400) | 2·3 |
| `implement/DraftDocumentCreationPolicyValidatorTest` | Unit | `validate_draftAlreadyExists` / `validate_documentIsUnderReview`(포트 mock 두 분기) | 4 |
| `presentation/DraftDocumentAuthorizationTest` | Controller | `read_notParticipant`(**404** — 존재를 드러내지 않는다) / `create_documentNotFound`(404) | 4 |
| `service/DraftDocumentEventServiceTest` | Service | `completeExamine_publishesEvent` / 같은 이벤트 2회 수신 시 멱등 | 4 |

**`DraftBodyComposerTest`가 이 도메인의 핵심 테스트다.** 치환을 **뒤에서 앞으로** 해야 앞쪽 오프셋이 유효하게 남는다. 앞에서 뒤로 치환하면 첫 치환 직후 나머지 오프셋이 전부 어긋난다.

### Fixture

`draftdocument/fixture/`에 둔다(src/test 전용). 정적 팩토리 + 내부 `static class XxxBuilder`, 기본값 보유, 체이닝, `build()`는 항상 유효한 객체를 반환한다. 식별자·값 객체는 `ReflectionTestUtils.setField`로 주입한다 — `WorkspaceFixture` 선례.

- `DraftDocumentFixture.draftDocument()` — `id`, `documentId`, `status`, `draftBody` 노출
- `SuggestionTermFixture.suggestionTerm()` — `id`, `draftDocumentId`, `anchor`, `status` 노출
- `TextRangeFixture.textRange()` — `startOffset`, `endOffset` 노출

모든 필드를 받는 거대 fixture나 `create(id, name, ...)` 형태는 만들지 않는다. 값이 자주 바뀌는 필드만 builder 메서드로 노출한다.

---

## 12. Phase와 태스크

| Phase | 목표 | 명시적 제외 |
| --- | --- | --- |
| 1 | 초안 한 건을 만들고 읽고 고치고 지운다 | 제안어, 목록 조회, 상태 전이, 크로스 도메인 |
| 2 | 제안어를 등록·수정·삭제하고 필터·페이징으로 조회한다 | 상태 전이, 본문 반영 |
| 3 | 제안어를 수용·거절하고 교정완료 시 본문을 만든다 | 크로스 도메인, 이벤트 |
| 4 | 초안 생성 정책을 지키고 교정완료를 이벤트로 알린다 | Notification 소비자, AI 대조 |

| ID | 태스크 | 산출물 | 일수 | 의존 |
| --- | --- | --- | --- | --- |
| **DD-1** | 루트 CRUD | 도메인 2 + `V400__` + Repository + implement 3 + Service + Command/Result 3 + Controller + DTO 3 + ErrorCode + 테스트 5 | 2 | — |
| **DD-2** | 제안어 CRUD + 목록 | `common/domain/TextRange` **최초 생성** + 도메인 2 + Repository + implement 3 + Service + 모델 4 + Controller + DTO 4 + 테스트 5 | 2 | DD-1 |
| **DD-3** | 교정 흐름 | `SuggestionTermProcessor` + `DraftBodyComposer` + 전이 메서드 + 모델 4 + DTO 2 + 테스트 4 | 1.5 | DD-2 |
| **DD-4** | 정책 + 이벤트 | 포트 3 + 스텁 3 + `CreationPolicyValidator` + `EventPublisher` + 이벤트 record 2 + 수신 핸들러 3 + 테스트 3 | 1.5 | DD-3, **RR-1** |

**DD-3이 D-9 확정으로 2일에서 1.5일로 줄었다.** `AnchorShiftCalculator`, `shiftAnchor(delta)`, 앵커 무효화 처리, `ANCHOR_STALE` 에러코드가 전부 사라졌다.

### Phase별 DoD

**공통 (모든 Phase)**
- [ ] `DraftDocumentService`가 **Repository를 직접 주입받지 않는다** — implement만 주입한다
- [ ] `domain` 패키지에 `@Component`·`@Transactional`·`ResponseEntity`·`HttpStatus`가 없다
- [ ] 컨트롤러가 도메인 모델을 직접 직렬화하지 않는다(Result → Response 변환만)
- [ ] Service의 상태 변경 지점에 `[DraftDocumentService.메서드명] ` prefix INFO 로그가 있다. 호출 스택을 읽어 자동 생성하지 않는다
- [ ] `./gradlew spotlessApply && ./gradlew check` 초록

**DD-1**
- [ ] `V400__create_draft_document.sql`이 PR에 포함되고 Repository 테스트가 Flyway 스키마 위에서 통과한다
- [ ] `DraftDocumentErrorCode` 상수 3개가 각각 최소 1개 테스트로 도달한다
- [ ] `docs/API.md` **끝에 자기 도메인 절만** 추가해 엔드포인트 4건을 적는다. 공통 규칙·페이징·버저닝 절은 건드리지 않는다(`EXECUTION_ORDER.md` 3절)

**DD-2**
- [ ] `common/domain/TextRange`가 규약(필드명·컬럼명·검증)대로 만들어져 develop에 올라갔고, ReviewRequest 담당에게 공유됐다
- [ ] `PageResponse` JSON 키가 공유 규약과 일치한다
- [ ] `sort` 파라미터가 화이트리스트 밖 필드를 받으면 400이다
- [ ] 목록 조회에 N+1이 없음을 Repository 테스트로 확인한다
- [ ] 목록 조회 서비스 메서드에 `@Transactional(readOnly = true)`가 있다

**DD-3**
- [ ] `compose_appliesFromTailToHead`가 통과한다
- [ ] 상태 전이 판단이 전부 도메인 메서드 안에 있고 service에 `if (status == ...)`가 없다
- [ ] `DRAFT_DOCUMENT_ALREADY_EXAMINED`가 수용·거절·교정완료 세 경로에서 모두 발생한다
- [ ] **교정중에는 판정을 다시 바꿀 수 있다**(D-11). `SUGGESTION_TERM_ALREADY_HANDLED` 같은 코드를 만들지 않았다

**DD-4**
- [ ] 이벤트 record에 엔티티·컬렉션·프록시가 없다
- [ ] `implement`가 `EventPublisher` 포트만 주입한다
- [ ] `draftdocument`가 `reviewrequest`·`workspace`의 **`implement`·`service`를 참조하지 않는다.** 어댑터가 상대 도메인의 `infra`(Repository)만 참조한다
- [ ] **`workspace` 패키지의 파일을 한 줄도 고치지 않았다**
- [ ] 4상태 전이 4건(`markReviewRequested`·`reopen`·`markRevised`·종단 거부)이 각각 테스트로 도달한다
- [ ] **비참여자에게 404를 준다**(403이 아니다)
- [ ] 이벤트 검증 테스트에 `@Transactional`이 없다

### 통합 태스크 (세 도메인 공통)

| ID | 내용 | 의존 |
| --- | --- | --- |
| T-INT-1 | `spring.flyway.out-of-order=true`(개발·테스트 프로파일만, 운영은 `false`) 추가 + `BaseEntityAuditingTest`에 남은 `ddl-auto=create-drop` + `flyway.enabled=false` 우회 정리 | 세 도메인 Phase 1 |
| T-INT-2 | 크로스 도메인 어댑터 배선 + `mode=real` 전환 + 스텁 제거 + 통합 테스트 | 세 도메인 Phase 4 |
| T-INT-3 | `SecurityConfig` 추가 후 `addFilters = false`를 인가 테스트로 전환, `memberId` 파라미터를 인증 주체 해석으로 교체 | 인증 도메인(별건) |

**ReviewRequest Phase 1~2가 전체 크리티컬 패스다.** DD-4가 `ReviewRequestQueryPort`의 실제 어댑터를 기다리므로, RR 담당이 Phase 1을 먼저 머지해야 한다.

---

## 13. 리스크와 열린 질문

| 항목 | 내용 | 대응 |
| --- | --- | --- |
| **겹치는 anchor** | 두 제안어의 anchor가 겹치는데 둘 다 수용되면 치환 결과가 깨진다. 수동 등록이라 사람이 겹치게 넣을 수 있다 | `DraftBodyComposer`가 겹침을 감지해 뒤쪽을 건너뛰거나 예외를 던진다. `compose_overlappingAnchors`로 동작을 못박는다. 등록 시점 겹침 검증을 둘지는 DD-2에서 판단 |
| **`suggestionTerm` 값의 출처** | DOMAIN.md는 "사전집 참조, 표기 복사 금지"라 하지만 MVP1에 Dictionary 도메인이 없어 참조할 대상이 없다 | Phase 2는 문자열로 받는다. Dictionary 도메인이 생기면 `DictionaryQueryPort`로 대표어 존재를 검증하는 것으로 승격 제안 |
| **`draftBody` 초기값** | SHRINK 태그대로 Phase 4에 서버 파생으로 바꾸면 요청 스펙이 바뀐다 | 필드에 `deprecated = true`를 미리 달아 프론트 의존을 막는다 |
| **Document 본문 10,000자 제약** | DOMAIN.md 제약이지만 Document 도메인이 없어 Phase 4까지 검증되지 않는다 | `DocumentQueryPort` 배선 시 함께 확인 |
| **권한 검사가 Phase 4까지 없다** | Phase 1~3은 다른 워크스페이스의 초안도 조회할 수 있다 | `memberId`가 요청 파라미터라 어차피 신뢰할 수 없고 인증 전까지 운영 배포 대상이 아니다. 7절에 명시했다 |
| **`BaseEntity` 리팩터링이 진행 중이다** | `refacotr/WLSH-86-base-entity`가 `@CreationTimestamp`·`@UpdateTimestamp`를 Spring Data 애노테이션으로 교체하고 있다 | 신규 도메인은 `BaseEntity`를 그대로 상속하므로 영향이 없다. **머지 직후 develop을 동기화**한다(10절) |

### DOMAIN.md 수정 (2026-09-10 반영 완료)

1. `DraftDocument.draftBody` 설명 → "교정완료 시 생성되는 본문"(D-9) ✔
2. `SuggestionTerm.rejectReason` → 거절 시 필수임을 설명에 추가 ✔
3. 관계 표 `Document (+Dictionary) --DictionaryContrast--> DraftDocument`에 "1차 MVP는 수동 생성" 각주 ✔
4. `DraftDocument.status`를 4상태로(D-8) ✔
5. `suggestions` 컬렉션 행에 "자식 FK 단방향" 주석 ✔
6. 정책 절에 초안 4상태 축·잠금 규칙(D-11)·애플리케이션 검증(D-10) 추가 ✔

### 리뷰 체크리스트 대조

| 출처 | 항목 | 이 문서 반영 위치 |
| --- | --- | --- |
| ARCHITECTURE.md | service가 비즈니스 흐름으로 읽히는가 / Repository 직접 참조 없는가 / implement가 하나의 역할인가 / 레이어 건너뛰기 없는가 / 도메인 모델이 presentation까지 올라가지 않는가 / domain에 Spring·Web 의존 없는가 / 이벤트 페이로드가 불변 record인가 / 핸들러가 멱등인가 | 12절 DoD |
| TEST.md | 계층별 전략 / AssertJ / DisplayName 어투 / Fixture Builder / 실패 케이스 네이밍 | 11절 |
| LOG.md | `[클래스.메서드] ` prefix / placeholder / Service는 상태 변경 완료 시점 / request body 로그 금지 | 12절 DoD |
| API.md | `/api` 접두사 / 생성 201·조회 200 / 도메인 모델 직렬화 금지 / userId를 본문에 담지 않음 | 7절 |
| EXCEPTION.md | 도메인별 ErrorCode enum / `{DOMAIN}_{REASON}` 형식 / status를 상수가 보유 | 8절 |
