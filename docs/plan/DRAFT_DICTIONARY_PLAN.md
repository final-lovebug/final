# DraftDictionary 도메인 구현 계획

사전 초안(DraftDictionary) 도메인의 구현 계획이다. `docs/DOMAIN.md`·`docs/ARCHITECTURE.md`·`docs/TEST.md`·`docs/API.md`·`docs/LOG.md`·`docs/EXCEPTION.md`를 먼저 읽고, 이 문서는 그 규칙을 이 도메인에 적용한 결과로 읽는다.

세 초안·리뷰 도메인 계획 문서는 같은 목차를 쓴다 — `DRAFT_DOCUMENT_PLAN.md`, `REVIEW_REQUEST_PLAN.md`.

**여러 세션으로 나눠 구현한다면 `EXECUTION_ORDER.md`를 먼저 읽는다.** 도메인을 가로지르는 태스크 순서, 공유 파일의 주인, 커밋 단위, 동시 실행 제약이 거기에 있다.

---

## 1. 범위와 목표

**담당 애그리게이트**: `DraftDictionary`(루트), `CandidateTerm`

**목표**: 문서에서 모은 용어 후보를 사람이 등재승인·동의어편입·거절·보류로 판정하고, 대표어와 정의를 다듬어 사전집 등재안으로 넘긴다.

**관련 요구사항**: `REQ-REV-001`(후보 목록 조회 — 필터링), `REQ-REV-002`(후보 상세·근거 비교), `REQ-REV-003`(대표어 선택·수정), `REQ-REV-004`(정의 편집), `REQ-REV-005`(후보 승인 — ADMIN 이상), `REQ-REV-006`(후보 기각 + 사유), `REQ-REV-007`(결정 기록 저장 — 제품 핵심 차별점), `REQ-REV-008`(일괄 승인·기각), `REQ-UPD-003`(신규 용어 등록 요청), `NFR-UPD-001`(Human-in-the-Loop — AI 판정만으로 사전집이 바뀌지 않는다)

### MVP1 범위 밖

`docs/DOMAIN.md`에서 취소선 처리된 것은 **구현하지 않는다.**

| 대상 | 사유 | 대체 |
| --- | --- | --- |
| `ExtractTerm`(용어 추출) | 표 제목이 취소선 | 초안과 후보어를 **수동 생성·등록 API**로 만든다 |

`docs/DOMAIN.md` 관계 표는 `Document --ExtractTerm--> DraftDictionary`로 쓰여 있으나, **MVP1의 실제 경로는 `Member --수동 생성--> DraftDictionary`** 다. 관계 표에 "1차 MVP는 수동 생성" 각주를 반영했다(2026-09-10).

추출 작업 자체의 상태(`대기/실행중/성공/실패`)와 진행 조회(`REQ-EXT-008`)도 이 도메인 범위가 아니다. AI 추출이 붙을 때 별도 도메인으로 설계하고, **후보어 등록 유스케이스를 배치로 호출**하는 형태가 된다. `REQ-EXT-*` 전체(임베딩·클러스터링·LLM 판정·동의어 그룹·동형이의)는 이 계획에 포함되지 않는다.

`REQ-REV-006`의 "기각된 후보는 재추출 시 재노출 억제"는 추출이 있어야 의미가 있으므로 MVP1에서는 기각 사유 기록까지만 한다.

---

## 2. 결정 대기표

### 2-1. 확정된 결정

| ID | 확정 내용 | DOMAIN.md 수정 제안 |
| --- | --- | --- |
| **D-18** | 도메인 레벨 패키지 구조(`presentation`/`service`/`implement`/`infra`/`domain`/`exception`)만 지키고 하위 디렉터리는 담당자 판단. 기본값은 `presentation/dto/`·`service/model/`(Member 선례), **ErrorCode는 `{domain}/exception/`**(ARCHITECTURE.md 규정) | — |
| **권한** | `Permission{OWNER, ADMIN, REGULAR}`이 이미 구현돼 있고 **Owner와 Admin은 사실상 동급**이다(`DOMAIN.md:459`, 9/8 확정). "Admin 이상만 용어 추출"은 **`validateAtLeast(workspaceId, memberId, Permission.ADMIN)`** 한 줄로 처리하고 Owner 전용 분기를 만들지 않는다 | — |
| **`sourceDocumentIds` 다중성** | DOMAIN.md 타입 칸은 단수 `DocumentId`지만 필드명이 복수이고 설명이 "여러 문서에서 모을 수 있음"이다. **복수로 읽고 `@ElementCollection`으로 매핑한다** | 타입을 `List<DocumentId>`로 수정 |
| **`candidates` 컬렉션** | DOMAIN.md에 `candidates : List<CandidateTerm>`가 살아 있지만 **JPA 컬렉션으로 매핑하지 않는다.** `DraftDocument.suggestions`가 취소선 처리된 것과 대칭을 맞추고, 후보어가 수백 건까지 늘 수 있어 루트에서 전부 로딩하면 목록·페이징이 불가능해진다 | `candidates` 행에 "자식 FK 단방향" 주석 또는 취소선 |
| **D-6** | **`CandidateTerm`에 `rejectReason`과 `handledBy`를 추가한다.** `SuggestionTerm`은 둘 다 갖고 있어 부대칭이었고, 두 필드 없이는 `REQ-REV-006`(기각 사유 입력)과 `REQ-REV-007`(누가·언제·무엇을·왜)을 구현할 수 없다. 별도 결정 이력 엔티티는 두지 않는다(D-11과 같은 방향) | `CandidateTerm` 표에 2행 추가 |
| **D-7** | **`mergeTargetTermId`를 추가한다.** `resultTermId`는 "승인 후 생성된 표준 용어"라 의미가 다르고, 재사용하면 등재승인과 동의어편입을 구분할 수 없다. Dictionary 도메인이 없어 지금은 값만 저장하고 Phase 4에 `DictionaryQueryPort`로 존재를 검증한다 | `CandidateTerm` 표에 1행 추가 |
| **D-8** | **초안 상태 축을 4상태로 두 도메인 통일** — `EXAMINING / EXAMINED / REVIEW_REQUESTED / REVISED`. 3상태를 택하면 `ReviewRequestRevisedEvent`를 받아도 반영할 상태가 없어 초안이 영구히 `리뷰요청됨`으로 남고 D-10의 "진행 중" 판정을 상태로 할 수 없다 | `status` 값 목록을 4상태로 |
| **D-11** | **교정중이면 후보어 판정을 자유롭게 변경하고, 교정완료 이후에는 잠근다.** 오타 한 번에 결정이 굳지 않는다. 감사 기록은 최종 상태 + `handledBy` + `updatedAt`으로 남기고 **변경 이력 엔티티를 두지 않는다** | 정책 절에 잠금 규칙 추가 |
| **D-10** | **애플리케이션 검증만 한다.** "진행 중"은 `status != REVISED` AND `deleted_at is null`이다. `UNIQUE(dictionary_id)`를 걸지 않는다 — 소프트 삭제를 쓰는데 MySQL 8.4에 부분 유니크 인덱스가 없고, `deleted_at`을 `NOT NULL`로 바꾸는 안은 `common/domain/BaseEntity` 변경을 수반한다 | 정책 절에 "애플리케이션 검증" 명시 |
| **D-15** | **`DocumentQueryPort.isOutdated` 시그니처만 두고 스텁은 `false`를 반환한다.** 추출이 MVP1 밖이라 "outdated 문서는 추출 대상이 아니다" 정책 자체가 유예된다. Document 도메인이 붙을 때 어댑터 하나만 교체한다 | outdated 미확정 항목에 이 우회를 부기 |
| **D-17** | **`docs/API.md`에 페이징·정렬 규격과 버저닝 방침을 신설한다**(10절 규약과 동일). 버저닝은 `/api` 유지 | — |
| **D-16** | **`docs/ARCHITECTURE.md`를 수정하지 않는다.** 나열된 애노테이션은 예시이고 핵심은 "JPA 매핑 애노테이션은 허용한다 / Spring·Web 의존은 두지 않는다"다 | — |
| **포트 소유** | **각 소비 도메인이 포트를 정의하고 자기 어댑터까지 구현한다.** 어댑터는 `{domain}/infra/adapter/`에 두고 제공 도메인의 `infra`(Repository)만 참조한다 | — |
| **리뷰 요청 조건** | **승인한 후보어가 0건이면 리뷰 요청을 만들 수 없다.** DOMAIN.md에 없던 규칙이지만 전부 거절한 초안으로 등재안을 만드는 것은 무의미하다 | 정책 절에 추가 |

### 2-2. 남은 결정 대기

**없다. 2026-09-10에 전건 확정했다.** 모든 Phase에 착수할 수 있다.

`docs/DOMAIN.md` 미확정 블록 중 이 도메인에 걸리는 것은 `Document`의 outdated 판별(D-15로 우회)과 알림 설정 모델(이벤트 소비자 미정)이다.

---

## 3. 도메인 모델

### DraftDictionary (애그리게이트 루트)

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | `@GeneratedValue(IDENTITY)` |
| `workspaceId` | `Long` | `workspace_id` | X | 논리 FK — 크로스 도메인이라 DB 제약 없음 |
| `dictionaryId` | `Long` | `dictionary_id` | X | 어디에 등재될지. 논리 FK |
| `sourceDocumentIds` | `List<Long>` | 별도 테이블 | — | `@ElementCollection` + `@CollectionTable("draft_dictionary_source_document")` |
| `status` | `DraftDictionaryStatus` | `status` | X | `@Enumerated(STRING)` |
| `createdBy` | `Long` | `created_by` | X | `updatable = false`. 정적 팩토리 인자로 받는다 |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | `BaseEntity` 상속 |

`candidates` 컬렉션 필드는 두지 않는다(2-1절).

### CandidateTerm

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `draftDictionaryId` | `Long` | `draft_dictionary_id` | X | **같은 도메인이라 DB FK를 건다** |
| `form` | `String` | `form` | X | 문서에서 추출된 표현 |
| `proposedDefinition` | `String` | `proposed_definition` | O | `TEXT`. AI 초안 또는 사람 작성 |
| `proposedEnglishName` | `String` | `proposed_english_name` | O | 코드·DB 네이밍 기준이 될 값 |
| `occurredDocumentIds` | `List<Long>` | 별도 테이블 | — | `@ElementCollection` |
| `occurrenceCount` | `int` | `occurrence_count` | X | 등재 우선순위 판단. 1 이상 |
| `contextSnippets` | `List<String>` | 별도 테이블 | — | `@ElementCollection`, 컬럼 `TEXT`. 판단 근거(`REQ-REV-002`·`REQ-EXT-007`) |
| `status` | `CandidateTermStatus` | `status` | X | |
| `resultTermId` | `Long` | `result_term_id` | O | 승인 후 생성된 표준 용어. Phase 4에 채워진다 |
| `rejectReason` | `String` | `reject_reason` | O | `TEXT`. **`거절` 시 필수**(D-6) |
| `mergeTargetTermId` | `Long` | `merge_target_term_id` | O | **`동의어편입` 시 필수**(D-7). 논리 FK — Dictionary 도메인이 없어 Phase 4까지 존재를 검증하지 않는다 |
| `handledBy` | `Long` | `handled_by` | O | 처리자. `REQ-REV-007`의 "누가"(D-6) |
| `createdBy` | `Long` | `created_by` | X | |

> `handledBy`는 DOMAIN.md의 `CandidateTerm` 표에 없었지만 `SuggestionTerm`에는 있었다. `REQ-REV-007`이 "누가·언제·무엇을·왜 정했는지"를 요구하므로 `rejectReason`·`mergeTargetTermId`와 함께 **`docs/DOMAIN.md`에 추가했다**(2026-09-10). "언제"는 `updatedAt`, "왜"는 `rejectReason`이 담는다.

### 애그리게이트 경계

`DraftDictionary`와 `CandidateTerm`은 **같은 애그리게이트**지만 JPA 컬렉션으로 묶지 않는다(2-1절). 대신

- 조회는 `CandidateTermRepository`로 직접 한다.
- 불변식(초안이 `교정완료` 이후면 후보어를 바꿀 수 없다)은 `CandidateTermDecisionProcessor`(implement)가 두 도메인 객체를 함께 받아 검증한다.
- 루트를 삭제하면 후보어와 `@ElementCollection` 테이블 행을 애플리케이션에서 함께 지운다(소프트 삭제는 루트만).

### enum

```
DraftDictionaryStatus { EXAMINING, EXAMINED, REVIEW_REQUESTED, REVISED }  // 교정중 / 교정완료 / 리뷰요청됨 / 반영완료
CandidateTermStatus { PENDING, REGISTRATION_APPROVED, MERGED_AS_SYNONYM, REJECTED, ON_HOLD }
//                    대기 / 등재승인 / 동의어편입 / 거절 / 보류
```

DOMAIN.md의 한국어 값을 그대로 옮긴 것이다. `APPROVED`처럼 줄이지 않는다 — `등재승인`과 `동의어편입`은 둘 다 "승인"의 일종이라 구분이 필요하다.

---

## 4. 상태 전이와 도메인 메서드

### DraftDictionaryStatus

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (신규) | `EXAMINING` | `DraftDictionary.create(...)` | — |
| `EXAMINING` | `EXAMINED` | `markExamined()` | **미처리(`PENDING`) 후보어 0건**. 위반 시 `DRAFT_DICTIONARY_CANDIDATE_TERM_UNDECIDED_EXISTS`(409) |
| `EXAMINED` | `REVIEW_REQUESTED` | `markReviewRequested()` | **`등재승인` 또는 `동의어편입`이 1건 이상**. 위반 시 `DRAFT_DICTIONARY_NO_APPROVED_CANDIDATE`(409) |
| `REVIEW_REQUESTED` | `EXAMINED` | `reopen()` | `ReviewRequestCanceledEvent` 수신. 교정완료를 유지하므로 **다시 요청할 수 있다** |
| **`REVIEW_REQUESTED`** | **`EXAMINING`** | **`reopenForRedecision()`** | `ReviewRequestChangesRequestedEvent` 수신. **후보어 판정을 다시 해야 새 회차 개정안을 만들 수 있다** |
| `REVIEW_REQUESTED` | `REVISED` | `markRevised()` | `ReviewRequestRevisedEvent` 수신. **종단** |
| `REVISED` | — | — | 종단. 이력으로만 남는다 |

**`REVIEW_REQUESTED → EXAMINING`은 이 도메인에만 있다.** `DraftDocument`는 재교정 본문을 리뷰 요청 쪽에서 직접 받으므로 초안을 다시 열지 않는다. 사전은 후보어 판정이 개정안의 내용 자체라서 초안을 열어야 한다 — 상태 축은 통일하되 전이는 도메인별로 다르다.

D-8 확정으로 **`requestReview()` 하나가 담당했던 조건이 두 전이로 갈렸다.** 미처리 0건은 교정완료의 조건이고, 승인 1건 이상은 리뷰 요청 생성의 조건이다. 그래서 `POST .../examine-completion` 엔드포인트가 새로 필요하다(7절).

Phase 3까지는 `EXAMINING`·`EXAMINED`·`REVIEW_REQUESTED`가 쓰인다(리뷰 요청 반쪽 구현). `REVISED`와 두 `reopen`은 Phase 4 이벤트 수신으로 도달한다.

### CandidateTermStatus

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (신규) | `PENDING` | 생성 | — |
| `PENDING` | `REGISTRATION_APPROVED` | `approveRegistration(handlerId)` | 초안이 `EXAMINING` |
| `PENDING` | `MERGED_AS_SYNONYM` | `mergeAsSynonym(handlerId, mergeTargetTermId)` | 초안이 `EXAMINING`, **편입 대상 필수**(D-7) |
| `PENDING` | `REJECTED` | `reject(handlerId, rejectReason)` | 초안이 `EXAMINING`, **사유 필수**(D-6) |
| `PENDING` | `ON_HOLD` | `hold(handlerId)` | 초안이 `EXAMINING` |
| 판정됨(`ON_HOLD` 포함) | 다른 상태 | 판정 메서드 재호출 | **초안이 `EXAMINING`이면 허용한다**(D-11). `EXAMINED` 이후에는 `DRAFT_DICTIONARY_NOT_EXAMINABLE`(409) |
| `REGISTRATION_APPROVED` | (`resultTermId` 채움) | `linkResultTerm(termId)` | 반영 완료 후. Phase 4 |

### 도메인 메서드 시그니처

```java
// DraftDictionary
static DraftDictionary create(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long createdBy);
void replaceSourceDocuments(List<Long> documentIds);
void markExamined();                  // P3 — 미처리 0건이어야 한다
void markReviewRequested();           // P3 반쪽 구현 → P4 이벤트 수신
void reopen();                        // P4 — 리뷰 취소 시 EXAMINED로
void reopenForRedecision();           // P4 — 변경요청 시 EXAMINING으로
void markRevised();                   // P4 — 종단
boolean isExamining();
boolean isExamined();
boolean isRevised();

// CandidateTerm
static CandidateTerm create(Long draftDictionaryId, String form, String proposedDefinition,
        String proposedEnglishName, List<Long> occurredDocumentIds, int occurrenceCount,
        List<String> contextSnippets, Long createdBy);
void editProposal(String form, String proposedDefinition, String proposedEnglishName);  // REQ-REV-003·004
void approveRegistration(Long handlerId);
void mergeAsSynonym(Long handlerId, Long mergeTargetTermId);
void reject(Long handlerId, String rejectReason);
void hold(Long handlerId);
void linkResultTerm(Long termId);
boolean isPending();
boolean isDecided();
```

`markExamined()`와 `markReviewRequested()`가 조건(미처리 0건, 승인 1건 이상)을 스스로 검증할 수 없다 — 도메인 객체가 후보어 컬렉션을 들고 있지 않기 때문이다. `DraftDictionaryReviewReadinessValidator`(implement)가 개수를 세어 검증한 뒤 전이 메서드를 호출한다. 두 조건을 각각 어느 전이가 지키는지는 4절 전이표를 따른다.

---

## 5. 스키마와 Flyway

**대역: 500–599** (`backend/CLAUDE.md`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 | Phase |
| --- | --- | --- |
| `V500__create_draft_dictionary.sql` | `draft_dictionary`, `draft_dictionary_source_document` | 1 |
| `V510__create_candidate_term.sql` | `candidate_term`, `candidate_term_occurred_document`, `candidate_term_context_snippet` | 2 |
| `V520__add_candidate_term_decision_columns.sql` | `reject_reason`, `merge_target_term_id`, `handled_by` (D-6·D-7) | 3 |

```sql
-- V500
create table draft_dictionary (
    id            bigint      not null auto_increment,
    workspace_id  bigint      not null,
    dictionary_id bigint      not null,
    status        varchar(20) not null,
    created_by    bigint      not null,
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    deleted_at    datetime(6),
    primary key (id)
);

create index idx_draft_dictionary_dictionary on draft_dictionary (dictionary_id);
create index idx_draft_dictionary_workspace on draft_dictionary (workspace_id);

create table draft_dictionary_source_document (
    draft_dictionary_id bigint not null,
    document_id         bigint not null,
    primary key (draft_dictionary_id, document_id),
    constraint fk_dd_source_document_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id)
);

-- V510
create table candidate_term (
    id                    bigint       not null auto_increment,
    draft_dictionary_id   bigint       not null,
    form                  varchar(255) not null,
    proposed_definition   text,
    proposed_english_name varchar(255),
    occurrence_count      int          not null,
    status                varchar(20)  not null,
    result_term_id        bigint,
    created_by            bigint       not null,
    created_at            datetime(6)  not null,
    updated_at            datetime(6)  not null,
    deleted_at            datetime(6),
    primary key (id),
    constraint fk_candidate_term_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id),
    constraint uk_candidate_term_form unique (draft_dictionary_id, form)
);

create index idx_candidate_term_draft_dictionary_status
    on candidate_term (draft_dictionary_id, status);
create index idx_candidate_term_occurrence
    on candidate_term (draft_dictionary_id, occurrence_count desc);
```

- `workspace_id`·`dictionary_id`·`document_id`·`result_term_id`에는 **FK 제약을 걸지 않는다**(크로스 도메인 논리 FK). 같은 도메인 내부(`draft_dictionary_id`)는 건다.
- **`UNIQUE(draft_dictionary_id, form)`은 건다** — 한 초안 안에 같은 표기의 후보어가 둘 있을 이유가 없고, 후보어는 소프트 삭제 대상이 아니라 충돌하지 않는다.
- **`UNIQUE(dictionary_id)`는 걸지 않는다.** "사전집당 흐름 1개"는 소프트 삭제와 충돌하고 `RevisionDictionary`도 함께 봐야 한다. `DraftDictionaryCreationPolicyValidator`가 `status != REVISED` AND `deleted_at is null`로 검증한다(D-10).
- `occurrence_count desc` 인덱스는 `REQ-REV-001`의 출현 횟수 정렬을 위한 것이다.
- 마이그레이션은 **Phase 1 필수 산출물**이다. `support/RepositoryTestSupport`가 Flyway로 스키마를 만들므로 마이그레이션 없이는 Repository 테스트가 테이블을 찾지 못한다.

---

## 6. 패키지와 파일

```
com.ubidict.backend.draftdictionary
├── presentation
│   ├── DraftDictionaryController.java            P1
│   ├── CandidateTermController.java              P2
│   └── dto
│       ├── CreateDraftDictionaryRequest.java     P1
│       ├── UpdateSourceDocumentsRequest.java     P1
│       ├── DraftDictionaryResponse.java          P1
│       ├── AddCandidateTermRequest.java          P2
│       ├── EditCandidateTermRequest.java         P2
│       ├── CandidateTermResponse.java            P2
│       ├── MergeCandidateTermRequest.java        P3
│       ├── RejectCandidateTermRequest.java       P3
│       ├── BulkDecideCandidateTermsRequest.java  P3
│       ├── BulkDecisionResponse.java             P3
│       └── ExamineProgressResponse.java          P3
├── service
│   ├── DraftDictionaryService.java               P1
│   ├── CandidateTermService.java                 P2
│   └── model
│       ├── CreateDraftDictionaryCommand.java     P1
│       ├── UpdateSourceDocumentsCommand.java     P1
│       ├── DraftDictionaryResult.java            P1
│       ├── AddCandidateTermCommand.java          P2
│       ├── EditCandidateTermCommand.java         P2
│       ├── CandidateTermSearchQuery.java         P2
│       ├── CandidateTermResult.java              P2
│       ├── DraftDictionarySearchQuery.java       P2
│       ├── DecideCandidateTermCommand.java       P3
│       ├── BulkDecideCandidateTermsCommand.java  P3
│       ├── BulkDecisionResult.java               P3
│       ├── CompleteExamineCommand.java             P3
│       ├── RequestDictionaryReviewCommand.java   P3
│       └── ExamineProgressResult.java            P3
├── implement
│   ├── DraftDictionaryReader.java                P1
│   ├── DraftDictionaryWriter.java                P1
│   ├── DraftDictionaryRemover.java               P1
│   ├── CandidateTermReader.java                  P2
│   ├── CandidateTermWriter.java                  P2
│   ├── CandidateTermOwnershipValidator.java      P2
│   ├── CandidateTermDecisionProcessor.java       P3
│   ├── CandidateTermBulkDecisionProcessor.java   P3
│   ├── DraftDictionaryReviewReadinessValidator.java  P3
│   ├── DraftDictionaryCreationPolicyValidator.java   P4
│   ├── CandidateTermResultLinker.java            P4
│   └── DraftDictionaryEventPublisher.java        P4
├── infra
│   ├── DraftDictionaryRepository.java            P1
│   ├── CandidateTermRepository.java              P2
│   └── port
│       ├── ReviewRequestQueryPort.java           P4
│       ├── DictionaryQueryPort.java              P4
│       ├── DocumentQueryPort.java                P4
│       ├── WorkspacePolicyPort.java              P4
│       ├── DictionarySnapshot.java               P4
│       └── stub
│           ├── StubReviewRequestQueryPort.java   P4
│           ├── StubDictionaryQueryPort.java      P4
│           ├── StubDocumentQueryPort.java        P4
│           └── StubWorkspacePolicyPort.java      P4
├── domain
│   ├── DraftDictionary.java                      P1
│   ├── DraftDictionaryStatus.java                P1
│   ├── CandidateTerm.java                        P2
│   ├── CandidateTermStatus.java                  P2
│   └── event
│       ├── DraftDictionaryCreatedEvent.java      P4
│       ├── DraftDictionaryReviewRequestedEvent.java  P4
│       └── CandidateTermDecidedEvent.java        P4
└── exception
    └── DraftDictionaryErrorCode.java             P1
```

`MemberWriter`가 Creator·Updater를 합친 선례를 따라 `~Writer` 하나로 생성·수정을 담는다.

---

## 7. API 명세

접두사 `/api`(버저닝 없음). 컨트롤러는 **`ResponseEntity<T>`를 반환**하고 `@ResponseStatus`를 쓰지 않는다. **Swagger 애노테이션은 쓰지 않는다**(명세는 `docs/API.md`가 담당).

요청자 식별은 **`@RequestParam Long memberId`** 다. 인증 계층(`NFR-USR-001`)이 없어 생긴 임시 방식이므로 컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 **"인증 도입 전까지 운영 배포 대상이 아니다"**를 적는다 — `WorkspaceController` 선례.

| Phase | Method | Path | 요청 | 응답 | 성공 | 에러 | 태그 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/api/draft-dictionaries` | `CreateDraftDictionaryRequest{workspaceId, dictionaryId, sourceDocumentIds}` | `DraftDictionaryResponse` | 201 | 400 `COMMON_INVALID_REQUEST`, 400 `..._SOURCE_DOCUMENT_REQUIRED` | **SHRINK** |
| 1 | GET | `/api/draft-dictionaries/{draftDictionaryId}` | — | `DraftDictionaryResponse` | 200 | 404 `DRAFT_DICTIONARY_NOT_FOUND` | KEEP |
| 1 | PUT | `/api/draft-dictionaries/{id}/source-documents` | `UpdateSourceDocumentsRequest{sourceDocumentIds}` | `DraftDictionaryResponse` | 200 | 404, 400 | KEEP |
| 1 | DELETE | `/api/draft-dictionaries/{draftDictionaryId}` | — | — | 204 | 404 | KEEP |
| 2 | GET | `/api/draft-dictionaries` | `workspaceId?`, `dictionaryId?`, `status?`, `page`, `size`, `sort` | `PageResponse<DraftDictionaryResponse>` | 200 | 400 | KEEP |
| 2 | POST | `/api/draft-dictionaries/{id}/candidate-terms` | `AddCandidateTermRequest{form, proposedDefinition?, proposedEnglishName?, occurredDocumentIds, occurrenceCount, contextSnippets?}` | `CandidateTermResponse` | 201 | 404, 409 `..._DUPLICATE_CANDIDATE_FORM`, 400 `..._INVALID_OCCURRENCE_COUNT` | KEEP |
| 2 | GET | `/api/draft-dictionaries/{id}/candidate-terms` | `status?`, `form?`(부분일치), `page`, `size`, `sort=occurrenceCount,desc` | `PageResponse<CandidateTermResponse>` | 200 | 404 | KEEP |
| 2 | GET | `/api/candidate-terms/{candidateTermId}` | — | `CandidateTermResponse` | 200 | 404 `..._CANDIDATE_TERM_NOT_FOUND` | KEEP |
| 2 | PATCH | `/api/candidate-terms/{candidateTermId}` | `EditCandidateTermRequest{form?, proposedDefinition?, proposedEnglishName?}` | `CandidateTermResponse` | 200 | 404, 409 `..._NOT_EXAMINABLE` | KEEP |
| 2 | DELETE | `/api/candidate-terms/{candidateTermId}` | — | — | 204 | 404 | KEEP |
| 3 | POST | `/api/candidate-terms/{id}/registration-approval` | — | `CandidateTermResponse` | 200 | 409 `..._NOT_EXAMINABLE` | KEEP |
| 3 | POST | `/api/candidate-terms/{id}/synonym-merge` | `MergeCandidateTermRequest{mergeTargetTermId}` | `CandidateTermResponse` | 200 | 409, 400 `..._MERGE_TARGET_REQUIRED` | KEEP |
| 3 | POST | `/api/candidate-terms/{id}/rejection` | `RejectCandidateTermRequest{rejectReason}` | `CandidateTermResponse` | 200 | 409, 400 `..._REJECT_REASON_REQUIRED` | KEEP |
| 3 | POST | `/api/candidate-terms/{id}/hold` | — | `CandidateTermResponse` | 200 | 409 | KEEP |
| 3 | POST | `/api/draft-dictionaries/{id}/candidate-terms/bulk-decision` | `BulkDecideCandidateTermsRequest{candidateTermIds, decision, rejectReason?, mergeTargetTermId?}` | `BulkDecisionResponse{succeeded, failed[]}` | 200 | 400, 404 | KEEP |
| 3 | POST | `/api/draft-dictionaries/{id}/examine-completion` | — | `DraftDictionaryResponse` | 200 | 409 `..._CANDIDATE_TERM_UNDECIDED_EXISTS`, 409 `..._ALREADY_EXAMINED` | KEEP |
| 3 | POST | `/api/draft-dictionaries/{id}/review-request` | — | `DraftDictionaryResponse` | 200 | 409 `..._NOT_EXAMINED`, 409 `..._NO_APPROVED_CANDIDATE`, 409 `..._ALREADY_REVIEW_REQUESTED` | **반쪽 구현** |
| 3 | GET | `/api/draft-dictionaries/{id}/examine-progress` | — | `ExamineProgressResponse{total, pending, approved, merged, rejected, onHold}` | 200 | 404 | KEEP |

**임시 API 태그** — `POST /api/draft-dictionaries`는 **SHRINK**다. MVP1에는 추출이 없어 사람이 초안을 시작하는 유일한 경로지만, Phase 4에 `DocumentQueryPort`가 붙으면 `sourceDocumentIds`의 **문서 존재 검증이 추가**된다(필드는 유지). 후보어 등록(`POST .../candidate-terms`)은 **KEEP**이다 — `REQ-DIC-004`가 "용어 수동 추가·수정·삭제"를 정식 요구사항으로 두므로 추출이 붙어도 남는다.

**`POST .../review-request`는 Phase 3에서 반쪽 구현이다.** 초안 상태를 `리뷰요청됨`으로만 바꾸고 **실제 `ReviewRequest`는 만들지 않는다.** 리뷰 요청 생성은 Phase 4(또는 ReviewRequest 도메인의 `POST /api/draft-dictionaries/{id}/review-request`)로 미룬다. 이 사실을 `docs/API.md`에도 적어 프론트가 오해하지 않게 한다.

`bulk-decision`(`REQ-REV-008`)은 일부 실패를 허용한다 — 이미 판정된 후보어가 섞여 있으면 전체를 롤백하지 않고 `succeeded`/`failed`로 나누어 보고한다.

**권한** — Phase 4에서 `WorkspacePolicyPort`로 검사한다. "Admin 이상만 용어 추출"(`DOMAIN.md:460`)을 초안 생성에 승계하고, **Owner와 Admin이 동급**이므로 `validateAtLeast(workspaceId, memberId, Permission.ADMIN)` 한 줄이면 된다. 참여자가 아니면 **403이 아니라 404**를 준다 — 403은 리소스 존재를 드러낸다(`WorkspaceAccessValidator` 선례). 미만 권한에는 403 + WARN 감사 로그(`NFR-REV-001`).

---

## 8. ErrorCode

`draftdictionary/exception/DraftDictionaryErrorCode.java`. `CommonErrorCode`와 같은 형태로 만든다.

| 상수 | status | message | Phase |
| --- | --- | --- | --- |
| `DRAFT_DICTIONARY_NOT_FOUND` | 404 | 사전 초안을 찾을 수 없습니다. | 1 |
| `DRAFT_DICTIONARY_SOURCE_DOCUMENT_REQUIRED` | 400 | 유래 문서는 최소 1건이 필요합니다. | 1 |
| `DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT` | 400 | 유래 문서가 중복되었습니다. | 1 |
| `DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_FOUND` | 404 | 후보어를 찾을 수 없습니다. | 2 |
| `DRAFT_DICTIONARY_DUPLICATE_CANDIDATE_FORM` | 409 | 이미 등록된 표기의 후보어입니다. | 2 |
| `DRAFT_DICTIONARY_CANDIDATE_TERM_MISMATCHED` | 400 | 해당 초안의 후보어가 아닙니다. | 2 |
| `DRAFT_DICTIONARY_INVALID_OCCURRENCE_COUNT` | 400 | 출현 횟수는 1 이상이어야 합니다. | 2 |
| `DRAFT_DICTIONARY_INVALID_FORM` | 400 | 후보어 표기가 올바르지 않습니다. | 2 |
| `DRAFT_DICTIONARY_REJECT_REASON_REQUIRED` | 400 | 기각 사유는 필수입니다. | 3 |
| `DRAFT_DICTIONARY_ALREADY_EXAMINED` | 409 | 이미 교정을 완료한 초안입니다. | 3 |
| `DRAFT_DICTIONARY_NOT_EXAMINED` | 409 | 교정을 완료하지 않은 초안은 리뷰를 요청할 수 없습니다. | 3 |
| `DRAFT_DICTIONARY_MERGE_TARGET_REQUIRED` | 400 | 동의어로 편입할 대상 용어가 필요합니다. | 3 |
| `DRAFT_DICTIONARY_CANDIDATE_TERM_UNDECIDED_EXISTS` | 409 | 판정하지 않은 후보어가 남아 있습니다. | 3 |
| `DRAFT_DICTIONARY_NO_APPROVED_CANDIDATE` | 409 | 승인한 후보어가 없어 리뷰를 요청할 수 없습니다. | 3 |
| `DRAFT_DICTIONARY_ALREADY_REVIEW_REQUESTED` | 409 | 이미 리뷰를 요청한 초안입니다. | 3 |
| `DRAFT_DICTIONARY_NOT_EXAMINABLE` | 409 | 리뷰 요청 중인 초안은 수정할 수 없습니다. | 3 |
| `DRAFT_DICTIONARY_ALREADY_EXISTS` | 409 | 해당 사전집에 진행 중인 초안이 있습니다. | 4 |
| `DRAFT_DICTIONARY_ACCESS_DENIED` | 403 | 워크스페이스 관리자 이상만 수행할 수 있습니다. | 4 |

---

## 9. 크로스 도메인 계약

### 소비 포트 (이 도메인이 정의하고 스텁까지 제공)

| 포트 | 시그니처 | 제공 도메인 | 스텁 동작 | Phase |
| --- | --- | --- | --- | --- |
| `ReviewRequestQueryPort` | `boolean hasOngoingDictionaryReview(Long dictionaryId)` | ReviewRequest | `false` | 4 |
| `DictionaryQueryPort` | `Optional<DictionarySnapshot> read(Long dictionaryId)`, `Set<String> preferredForms(Long dictionaryId)` | Dictionary(미착수) | 빈 집합 | 4 |
| `DocumentQueryPort` | `boolean existsAll(Collection<Long> documentIds)`, `boolean isOutdated(Long documentId)` | Document(미착수) | `true` / `false` | 4 |
| `WorkspacePolicyPort` | `boolean hasAdminPermission(Long workspaceId, Long memberId)`, `boolean isParticipant(Long workspaceId, Long memberId)` | Workspace(**머지됨**) | `true` | 4 |

`DictionarySnapshot`은 `record DictionarySnapshot(Long workspaceId, String name, int currentVersionNo)`. **엔티티를 포트 시그니처에 노출하지 않는다.**

### 어댑터 배치 (2026-09-10 확정)

**포트는 우리가 정의하고 어댑터도 우리가 구현한다.** 어댑터는 `draftdictionary/infra/adapter/`에 두고 **제공 도메인의 `infra`(Repository)만 참조한다** — 같은 레이어끼리라 방향 위반이 아니다. 제공 도메인의 `implement`(`ParticipantReader` 등)를 참조하면 `infra -> implement`가 되어 `docs/ARCHITECTURE.md`의 역방향 참조 금지를 어긴다. 이 규약 덕분에 **`workspace` 패키지의 파일을 한 줄도 고치지 않는다.**

`WorkspacePolicyPort`의 실제 어댑터는 `workspace/infra/ParticipantRepository`를 참조해 참여자 권한을 읽는다.

어댑터 선택은 프로퍼티로 한다 — `app.crossdomain.{name}.mode=stub|real`(기본 `stub`, `matchIfMissing = true`). `InMemoryEventPublisher`의 `@ConditionalOnProperty` 패턴을 따르고, `@ConditionalOnMissingBean`은 쓰지 않는다.

`DictionaryQueryPort.preferredForms()`는 **이미 사전집에 있는 대표어를 후보어로 다시 등록하는 것을 막는 용도**다(`NFR-DIC-002` 대표어 유일성). MVP1에는 Dictionary 도메인이 없어 스텁이 빈 집합을 반환하므로 검증이 사실상 통과한다.

### 제공 포트 (다른 도메인이 우리를 볼 때)

ReviewRequest가 `DraftDictionaryQueryPort`를 정의하고 우리가 어댑터를 구현한다. 예상 시그니처는 `Optional<DraftDictionarySnapshot> read(Long draftDictionaryId)`이고, 스냅샷에는 `dictionaryId`, `status`, 승인된 후보어 목록이 담긴다. **정확한 시그니처는 ReviewRequest 담당이 정의하고 우리는 구현만 한다**(소비자가 포트를 정의하는 규약).

### 발행 이벤트

| 이벤트 | 페이로드 | 수신자 |
| --- | --- | --- |
| `DraftDictionaryCreatedEvent` | `(Long draftDictionaryId, Long workspaceId, Long dictionaryId, Long createdBy, OffsetDateTime occurredAt)` | 없음(Notification 미정) |
| `DraftDictionaryReviewRequestedEvent` | `(Long draftDictionaryId, Long workspaceId, Long dictionaryId, Long requesterId, OffsetDateTime occurredAt)` | ReviewRequest(Phase 4) |
| `CandidateTermDecidedEvent` | `(Long candidateTermId, Long draftDictionaryId, CandidateTermStatus status, Long handledBy, OffsetDateTime occurredAt)` | 없음(Notification 미정) |

**불변 record, 식별자·원시값·enum·시각만 담는다.** enum은 값 객체로 취급해 허용된다. 도메인 모델이 곧 JPA 엔티티이므로 엔티티·지연 로딩 프록시·연관 컬렉션을 담으면 안 된다. 인메모리 어댑터는 참조를 그대로 넘겨 로컬에서 위반이 드러나지 않으므로 **리뷰에서 확인한다.**

`implement`는 `common/infra/event/EventPublisher` 포트만 주입받는다.

### 수신 이벤트

| 이벤트 | 발행자 | 처리 | Phase |
| --- | --- | --- | --- |
| `ReviewRequestCreatedEvent` | ReviewRequest | `markReviewRequested()` — Phase 3 반쪽 구현을 이벤트 수신으로 대체한다 | 4 |
| `ReviewRequestRevisedEvent` | ReviewRequest | 승인된 후보어에 `resultTermId` 연결(`CandidateTermResultLinker`) + `markRevised()`(종단) | 4 |
| `ReviewRequestCanceledEvent` | ReviewRequest | `reopen()` — 초안을 **`교정완료`로** 되돌린다. 다시 요청할 수 있다 | 4 |
| **`ReviewRequestChangesRequestedEvent`** | ReviewRequest | `reopenForRedecision()` — 초안을 **`교정중`으로** 되돌려 후보어를 다시 판정하게 한다 | 4 |

`ReviewRequestChangesRequestedEvent`의 페이로드에 **`type`과 `sourceDraftId`가 필요하다.** 현재 ReviewRequest 계획의 페이로드는 `(reviewRequestId, requesterId, occurredAt)`뿐이라 어느 초안인지 알 수 없다. RR 담당과 함께 페이로드를 확장한다(`REVIEW_REQUEST_PLAN.md` 9절에 반영).

핸들러는 **멱등**하게 만든다. 같은 이벤트를 두 번 받아도 `resultTermId`가 한 번만 채워지고 상태가 한 번만 바뀌어야 한다.

**`Term` 생성과 `DictionaryVersion` 발행은 이 도메인이 하지 않는다.** 승인 결과를 실제 표준 용어로 만드는 일은 Dictionary 도메인의 몫이고, 우리는 `resultTermId`를 받아 연결만 한다. 어느 쪽이 `Term`을 만들지는 Dictionary 도메인 착수 시 합의한다(13절).

---

## 10. 공유 파일 규약

### 공유 자산과 주인

| 자산 | 최초 필요 | 규약 |
| --- | --- | --- |
| `common/presentation/PageResponse`, `common/service/PageResult` | **DraftDocument (DD-2)** — 선착순이 아니다(`EXECUTION_ORDER.md` 3절) | 요청 `page`(0-base) / `size`(기본 20, 최대 100) / `sort=필드,방향`(화이트리스트 외 400). 응답 JSON 키 `content`/`page`/`size`/`totalElements`/`totalPages`. **service는 Spring `Page`를 반환하지 않는다** — infra 타입이 상위 레이어로 새는 것 |
| `common/domain/TextRange` | DraftDocument Phase 2에서 만든다 | 이 도메인은 쓰지 않는다(`anchor` 없음) |

### 수정 금지 파일

`common/**`(위 자산 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`. 필요하면 통합 태스크로 넘기고 PR에 이유를 적는다.

### git worktree 운영

작업은 `../final-draftdictionary` worktree에서 한다. 워킹 디렉터리는 분리되지만 `.git`·Docker·호스트 포트는 공유된다.

| 항목 | 규칙 | 이유 |
| --- | --- | --- |
| 브랜치명 | `feat/WLSH-{티켓}-draftdictionary` | `.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다. 패턴이 없으면 티켓 추적이 끊긴다 |
| **`bootRun` 동시 실행 금지** | 한 번에 한 worktree만 | `spring-boot-docker-compose`가 worktree마다 별도 Compose 프로젝트(디렉터리명 기준)를 띄운다. MySQL·MongoDB·Redis·LGTM이 3배로 뜨고 호스트 포트 8080·3000이 겹쳐 `port is already allocated`로 죽는다 |
| **`./gradlew test` 동시 실행 주의** | 세 worktree에서 동시에 돌리지 않는다 | `RepositoryTestSupport`가 MySQL 컨테이너를, `IntegrationTestSupport`가 `TestcontainersConfiguration`(MySQL+Mongo+Redis+LGTM)을 띄운다. worktree 3개면 Gradle 데몬도 3개다. 루트 `CLAUDE.md`는 LGTM과 빌드 JVM이 겹치면 Docker Desktop 기본 메모리에서 OOM으로 죽는다고 기록한다 |
| develop 동기화 | 공유 자산이 올라온 직후, 다른 도메인 Phase 1이 머지된 직후, `refacotr/WLSH-86-base-entity`(BaseEntity 감사 애노테이션 교체)가 머지된 직후 **즉시 머지** | 규약이 어긋난 채 오래 진행하면 나중에 세 브랜치를 동시에 고쳐야 한다 |
| 파일 소유권 | `draftdictionary/**` + Flyway 500–599 | worktree는 편집 충돌을 숨기고 머지 시점에 되살린다 |

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

AssertJ를 쓴다. JUnit `assertEquals`·`assertThrows`는 쓰지 않는다. Mockito는 BDD 스타일(`given(...).willReturn(...)`). `@DisplayName`은 한글 `~다.` 어투, 메서드명은 영어, 실패 케이스는 `{메서드명}_{실패이유}`. given/when/then 주석으로 구역을 나눈다.

### 테스트 클래스와 필수 케이스

| 클래스 | 계층 | 필수 케이스 | Phase |
| --- | --- | --- | --- |
| `domain/DraftDictionaryTest` | Unit | `create`("사전 초안을 생성하면 교정중 상태로 시작한다.") / `create_sourceDocumentsIsEmpty` / `markReviewRequested_alreadyRequested` / `markReviewRequested_notExamined` / **`reopen_returnsToExamined`**("리뷰가 취소되면 교정완료로 돌아간다.") / **`reopenForRedecision_returnsToExamining`**("변경요청을 받으면 교정중으로 돌아가 후보어를 다시 판정할 수 있다.") / `markRevised_isTerminal` | 1·3·4 |
| `infra/DraftDictionaryRepositoryTest` | Repository | `findByDictionaryId` / **`save_persistsSourceDocumentIds`**("유래 문서 목록이 함께 저장된다." — `@ElementCollection` 매핑) / `save_auditingFieldsAreSet` / `findById_deleted` | 1 |
| `implement/DraftDictionaryReaderTest` | Unit | `read_notFound` | 1 |
| `service/DraftDictionaryServiceTest` | Service | `create` / `updateSourceDocuments` / `delete` / `create_duplicateSourceDocument` | 1 |
| `presentation/DraftDictionaryControllerTest` | Controller | `create`(201) / `create_dictionaryIdIsNull`(400) / `read_notFound`(404 + 코드 확인) | 1 |
| `domain/CandidateTermTest` | Unit | `create`("후보어를 생성하면 대기 상태로 시작한다.") / `create_occurrenceCountIsZero` / `create_formIsBlank` / `editProposal` | 2 |
| `infra/CandidateTermRepositoryTest` | Repository | **`findByDraftDictionaryIdOrderByOccurrenceCountDesc`**("출현 횟수 내림차순으로 조회한다.") / `existsByDraftDictionaryIdAndForm` / **`save_persistsContextSnippets`** / `findByStatus` | 2 |
| `implement/CandidateTermOwnershipValidatorTest` | Unit | `validate_mismatched`("다른 초안의 후보어를 지정하면 예외가 발생한다.") | 2 |
| `service/CandidateTermServiceTest` | Service | `add` / `add_duplicateForm`(409) / **`edit_updatesDefinition`**("정의 초안을 사람이 수정할 수 있다." — `REQ-REV-004`) / `search_filtersByStatus`(`REQ-REV-001`) | 2 |
| `domain/CandidateTermDecisionTest` | Unit | `approveRegistration`("등재승인하면 처리자가 기록된다.") / `mergeAsSynonym_targetIsNull` / `reject_reasonIsBlank` / `hold` / **`approveRegistration_afterReject`**("교정 중이면 기각한 후보어를 다시 승인할 수 있다." — D-11) | 3 |
| `implement/DraftDictionaryReviewReadinessValidatorTest` | Unit | `validate_undecidedExists` / `validate_noApprovedCandidate` | 3 |
| `implement/CandidateTermBulkDecisionProcessorTest` | Unit | **`decideAll_partialFailure`**("이미 판정된 후보어가 섞여 있으면 성공과 실패를 나누어 보고한다." — `REQ-REV-008`) | 3 |
| `service/CandidateTermDecisionServiceTest` | Service | 4방향 각 1건 + `approve_afterReviewRequested`(409) | 3 |
| `service/DraftDictionaryReviewRequestServiceTest` | Service | `completeExamine`("판정을 모두 마치면 교정완료가 된다.") / `completeExamine_undecidedExists`(409) / `requestReview` / `requestReview_notExamined`(409) / `requestReview_noApprovedCandidate`(409) / `editCandidateTerm_afterExamined`(409) | 3 |
| `presentation/CandidateTermControllerTest` | Controller | `add`(201) / `add_formIsBlank`(400) / `search_sortsByOccurrenceCount` / `reject_reasonIsBlank`(400) / `bulkDecide_emptyIds`(400) | 2·3 |
| `implement/DraftDictionaryCreationPolicyValidatorTest` | Unit | `validate_draftAlreadyExists` / `validate_revisionAlreadyExists`(포트 mock 두 분기) | 4 |
| `presentation/DraftDictionaryAuthorizationTest` | Controller | `create_regularPermission`(403 + `..._ACCESS_DENIED`) / `create_notParticipant`(**404** — 존재를 드러내지 않는다) | 4 |
| `service/DraftDictionaryEventServiceTest` | Service | `requestReview_publishesEvent` / 같은 이벤트 2회 수신 시 멱등 | 4 |

### Fixture

`draftdictionary/fixture/`에 둔다(src/test 전용). 정적 팩토리 + 내부 `static class XxxBuilder`, 기본값 보유, `ReflectionTestUtils.setField`로 식별자 주입 — `WorkspaceFixture` 선례.

- `DraftDictionaryFixture.draftDictionary()` — `id`, `workspaceId`, `dictionaryId`, `status`, `sourceDocumentIds` 노출
- `CandidateTermFixture.candidateTerm()` — `id`, `draftDictionaryId`, `form`, `status`, `occurrenceCount` 노출

모든 필드를 받는 거대 fixture나 `create(id, form, ...)` 형태는 만들지 않는다.

---

## 12. Phase와 태스크

| Phase | 목표 | 명시적 제외 |
| --- | --- | --- |
| 1 | 사전 초안 한 건을 만들고 유래 문서 목록을 관리한다 | 후보어, 목록 조회, 상태 전이, 크로스 도메인 |
| 2 | 후보어를 등록·편집·삭제하고 상태·표기·출현 횟수로 조회한다 | 판정 전이, 리뷰 요청 |
| 3 | 후보어를 4방향으로 판정하고 초안을 리뷰 요청 상태로 넘긴다 | 크로스 도메인, 인가, `ReviewRequest` 실제 생성 |
| 4 | 사전집당 흐름 1개를 지키고 인가·이벤트를 붙인다 | Notification 소비자, AI 추출, `Term` 생성 |

| ID | 태스크 | 산출물 | 일수 | 의존 |
| --- | --- | --- | --- | --- |
| **DI-1** | 루트 CRUD | 도메인 2 + `V500__` + Repository + implement 3 + Service + Command/Result 3 + Controller + DTO 3 + ErrorCode + 테스트 5 | 2 | — |
| **DI-2** | 후보어 CRUD + 목록 | 도메인 2 + `V510__` + Repository + implement 3 + Service + 모델 5 + Controller + DTO 3 + 테스트 5 | 2 | DI-1 |
| **DI-3** | 판정 + 리뷰 요청 | `V520__`(D-6·D-7 필드) + 전이 메서드 6 + implement 3 + 모델 5 + DTO 5 + 테스트 6 | 2 | DI-2 |
| **DI-4** | 정책 + 인가 + 이벤트 | 포트 4 + 스텁 4 + `CreationPolicyValidator` + `ResultLinker` + `EventPublisher` + 이벤트 record 3 + 테스트 4 | 1.5 | DI-3, **RR-2** |

### Phase별 DoD

**공통 (모든 Phase)**
- [ ] `DraftDictionaryService`가 **Repository를 직접 주입받지 않는다** — implement만 주입한다
- [ ] `domain` 패키지에 `@Component`·`@Transactional`·`ResponseEntity`·`HttpStatus`가 없다
- [ ] 컨트롤러가 도메인 모델을 직접 직렬화하지 않는다(Result → Response 변환만)
- [ ] Service의 상태 변경 지점에 `[DraftDictionaryService.메서드명] ` prefix INFO 로그가 있다. 호출 스택을 읽어 자동 생성하지 않는다
- [ ] `./gradlew spotlessApply && ./gradlew check` 초록

**DI-1**
- [ ] `V500__create_draft_dictionary.sql`이 PR에 포함되고 Repository 테스트가 Flyway 스키마 위에서 통과한다
- [ ] `@ElementCollection` 매핑이 Repository 테스트로 검증된다
- [ ] `docs/API.md` **끝에 자기 도메인 절만** 추가해 엔드포인트 4건을 적는다. 공통 규칙·페이징·버저닝 절은 건드리지 않는다(`EXECUTION_ORDER.md` 3절)

**DI-2**
- [ ] `PageResponse` JSON 키가 공유 규약과 일치한다(먼저 만들어졌으면 그것을 쓰고, 우리가 처음이면 규약대로 만들어 develop에 올린다)
- [ ] `sort` 파라미터가 화이트리스트 밖 필드를 받으면 400이다
- [ ] 목록 조회에 N+1이 없음을 Repository 테스트로 확인한다(`@ElementCollection` 두 개가 걸려 있어 특히 주의)
- [ ] 목록 조회 서비스 메서드에 `@Transactional(readOnly = true)`가 있다
- [ ] `REQ-REV-001`(필터)·`REQ-REV-003`·`REQ-REV-004`(대표어·정의 편집)가 테스트로 커버된다

**DI-3**
- [ ] 전이 판단이 전부 도메인 메서드 안에 있고 service에 `if (status == ...)`가 없다
- [ ] 처리자·처리 시각·사유가 모두 저장된다(`REQ-REV-007`의 "누가·언제·무엇을·왜")
- [ ] **교정완료와 리뷰 요청이 별개 전이다.** 미처리 0건은 `markExamined()`가, 승인 1건 이상은 `markReviewRequested()`가 지킨다
- [ ] **교정중에는 판정을 다시 바꿀 수 있다**(D-11). `CANDIDATE_TERM_ALREADY_DECIDED` 같은 코드를 만들지 않았다
- [ ] `POST .../review-request`가 `ReviewRequest`를 만들지 않는다는 사실이 `docs/API.md`와 이 문서 7절에 적혀 있다
- [ ] `REQ-REV-005`·`REQ-REV-006`·`REQ-REV-008`이 테스트로 커버된다

**DI-4**
- [ ] 이벤트 record에 엔티티·컬렉션·프록시가 없다
- [ ] `implement`가 `EventPublisher` 포트만 주입한다
- [ ] `draftdictionary`가 다른 도메인의 **`implement`·`service`를 참조하지 않는다.** 어댑터가 상대 도메인의 `infra`(Repository)만 참조한다
- [ ] **`workspace` 패키지의 파일을 한 줄도 고치지 않았다**
- [ ] 4상태 전이 5건(`markReviewRequested`·`reopen`·`reopenForRedecision`·`markRevised`·종단 거부)이 각각 테스트로 도달한다
- [ ] 403 경로에 WARN 감사 로그가 남는다(`NFR-REV-001`)
- [ ] 비참여자에게 404를 준다(403이 아니다)
- [ ] 이벤트 검증 테스트에 `@Transactional`이 없다

### 통합 태스크 (세 도메인 공통)

| ID | 내용 | 의존 |
| --- | --- | --- |
| T-INT-1 | `spring.flyway.out-of-order=true`(개발·테스트 프로파일만, 운영은 `false`) 추가 + `BaseEntityAuditingTest`에 남은 `ddl-auto=create-drop` + `flyway.enabled=false` 우회 정리 | 세 도메인 Phase 1 |
| T-INT-2 | 크로스 도메인 어댑터 배선 + `mode=real` 전환 + 스텁 제거 + 통합 테스트 | 세 도메인 Phase 4 |
| T-INT-3 | `SecurityConfig` 추가 후 `addFilters = false`를 인가 테스트로 전환, `memberId` 파라미터를 인증 주체 해석으로 교체 | 인증 도메인(별건) |

**ReviewRequest Phase 2가 이 도메인의 Phase 4를 막는다.** DI-4가 `RevisionDictionary`의 존재를 확인해야 "사전집당 흐름 1개"를 판정할 수 있다.

---

## 13. 리스크와 열린 질문

| 항목 | 내용 | 대응 |
| --- | --- | --- |
| **`Term` 생성 주체** | 승인된 후보어를 실제 `Term`으로 만들고 `DictionaryVersion`을 발행하는 일이 Dictionary 도메인 몫인지, ReviewRequest의 `Revise`가 하는지 미정 | 이 도메인은 `resultTermId`를 **받아 연결만** 한다(`CandidateTermResultLinker`). Dictionary 도메인 착수 시 합의 |
| **`@ElementCollection` 3개** | `sourceDocumentIds`, `occurredDocumentIds`, `contextSnippets`가 모두 별도 테이블이다. 목록 조회에서 N+1이 나기 쉽다 | 목록 응답에는 컬렉션을 담지 않고 개수만 준다. 상세 조회에서만 fetch join으로 가져온다. Repository 테스트로 확인 |
| **후보어 개수** | 문서 여러 건에서 모으면 후보어가 수백 건이 될 수 있다 | 컬렉션 매핑을 안 하기로 한 이유다(2-1절). 일괄 판정(`REQ-REV-008`)도 페이지 단위로 제한하는 것을 검토 |
| **동의어편입 대상 검증** | `mergeTargetTermId`가 실제 존재하는 `Term`인지 확인할 수 없다(Dictionary 도메인 없음) | Phase 4에 `DictionaryQueryPort`로 검증 추가. 그전에는 값만 저장 |
| **`NFR-REV-002` 낙관적 락** | "version 컬럼 기반 낙관적 락"이 요구사항이지만 이 계획에 없다 | 후보어 동시 판정 충돌이 실제로 문제가 되면 `@Version`을 `CandidateTerm`에 추가한다. Phase 4 이후 별건 |
| **사전 재교정이 이전 회차 내용을 지운다** | D-5 확정으로 사전 개정안이 초안을 참조만 하므로, 변경요청으로 초안이 `교정중`으로 열려 후보어 판정이 바뀌면 **이전 회차 리뷰 당시 내용을 재현할 수 없다** | 받아들인 한계다(D-5). 항목별 스냅샷이 실제로 필요해지면 `ProposedTerm` 엔티티를 MVP2에 신설한다 |
| **`BaseEntity` 리팩터링이 진행 중이다** | `refacotr/WLSH-86-base-entity`가 감사 애노테이션을 Spring Data 방식으로 교체하고 있다 | 신규 도메인은 `BaseEntity`를 그대로 상속하므로 영향이 없다. **머지 직후 develop을 동기화**한다(10절) |

### DOMAIN.md 수정 (2026-09-10 반영 완료)

1. `DraftDictionary.sourceDocumentIds` 타입 → `List<DocumentId>` ✔
2. `DraftDictionary.candidates` → 취소선 + "자식 FK 단방향" 주석 ✔
3. `CandidateTerm`에 `rejectReason`(D-6), `mergeTargetTermId`(D-7), `handledBy` 추가 ✔
4. 관계 표 `Document --ExtractTerm--> DraftDictionary`에 "1차 MVP는 수동 생성" 각주 ✔
5. `DraftDictionary.status`를 4상태로(D-8) ✔
6. 정책 절에 리뷰 요청 조건("승인된 후보어 1건 이상")·초안 잠금(D-11)·애플리케이션 검증(D-10) 추가 ✔

### 리뷰 체크리스트 대조

| 출처 | 항목 | 이 문서 반영 위치 |
| --- | --- | --- |
| ARCHITECTURE.md | service가 비즈니스 흐름으로 읽히는가 / Repository 직접 참조 없는가 / implement가 하나의 역할인가 / 레이어 건너뛰기 없는가 / 도메인 모델이 presentation까지 올라가지 않는가 / domain에 Spring·Web 의존 없는가 / 도메인 간 직접 참조가 늘지 않는가 / 이벤트 페이로드가 불변 record인가 / 핸들러가 멱등인가 | 12절 DoD |
| TEST.md | 계층별 전략 / AssertJ / DisplayName 어투 / Fixture Builder / 실패 케이스 네이밍 | 11절 |
| LOG.md | `[클래스.메서드] ` prefix / placeholder / Service는 상태 변경 완료 시점 / 403 감사 로그 | 12절 DoD |
| API.md | `/api` 접두사 / 생성 201·조회 200 / 도메인 모델 직렬화 금지 / userId를 본문에 담지 않음 | 7절 |
| EXCEPTION.md | 도메인별 ErrorCode enum / `{DOMAIN}_{REASON}` 형식 / status를 상수가 보유 | 8절 |
