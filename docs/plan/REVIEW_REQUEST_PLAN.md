# ReviewRequest 도메인 구현 계획

리뷰 요청(ReviewRequest) 도메인의 구현 계획이다. `docs/DOMAIN.md`·`docs/ARCHITECTURE.md`·`docs/TEST.md`·`docs/API.md`·`docs/LOG.md`·`docs/EXCEPTION.md`를 먼저 읽고, 이 문서는 그 규칙을 이 도메인에 적용한 결과로 읽는다.

세 초안·리뷰 도메인 계획 문서는 같은 목차를 쓴다 — `DRAFT_DOCUMENT_PLAN.md`, `DRAFT_DICTIONARY_PLAN.md`.

**여러 세션으로 나눠 구현한다면 `EXECUTION_ORDER.md`를 먼저 읽는다.** 도메인을 가로지르는 태스크 순서, 공유 파일의 주인, 커밋 단위, 동시 실행 제약이 거기에 있다.

**이 도메인이 전체 크리티컬 패스다.** DraftDocument Phase 4와 DraftDictionary Phase 4가 각각 `ReviewRequestStatus`와 `RevisionDictionary`를 포트 대상으로 필요로 하므로, **Phase 1~2를 먼저 develop에 올린다.**

---

## 1. 범위와 목표

**담당 애그리게이트**: `ReviewRequest`(루트), `Reviewer`, `RevisionDocument`, `RevisionDictionary`, `Review`, `Comment`, `Reexamine`, `Revise`

**목표**: 초안(문서·사전)에서 만든 개정안을 리뷰어가 검토·승인하고, 조건을 충족하면 원본에 반영해 새 버전을 발행한다.

**관련 요구사항**: `REQ-REV-007`(결정 기록 저장 — 제품 핵심 차별점), `REQ-UPD-004`(사전집 갱신 승인 — ADMIN 이상), `NFR-REV-001`(승인 권한 검증 — 미만은 403 + 감사 로그), `NFR-REV-002`(동시 수정 제어 — 낙관적 락), `NFR-UPD-001`(Human-in-the-Loop)

> `docs/REQUIREMENTS.md`의 `REQ-REV-*`는 전부 **사전 후보어 리뷰**(후보 목록·대표어 선택·정의 편집) 기준이고, 그 대부분은 `DraftDictionary` 도메인이 담당한다. **`type = DOCUMENT` 경로에는 대응 요구사항 ID가 없다** — 도메인 모델에만 존재하는 흐름이다. 요구사항 보강 제안을 13절에 적는다.

### MVP1 범위 밖

| 대상 | 사유 |
| --- | --- |
| `RevisedTerm` | **제외 확정**(D-4, 2026-09-10). `RevisionDocument.proposedBody`가 본문 전체를 스냅샷으로 담으므로 항목 단위 스냅샷을 둘 이유가 약하다. 게다가 한글명이 `SuggestionTerm`과 똑같이 "제안어"여서 유비쿼터스 언어 원칙에 위반되고, FK가 `draftDocumentId`라 이 도메인 엔티티가 DraftDocument를 직접 참조한다. `docs/DOMAIN.md`에서 취소선 처리했다 |
| Notification 수신 | UL에서 "미정". 이벤트는 발행하지만 소비자가 없어도 된다 |
| `DocumentVersion`·`DictionaryVersion` 실제 발행 | Document·Dictionary 도메인 미착수. **포트 시그니처와 발행 호출까지만** 하고 실제 버전 생성은 그쪽 몫이다 |

---

## 2. 결정 대기표

### 2-1. 확정된 결정

이 도메인의 핵심 설계가 여기서 정해졌다. **GitHub Pull Request 모델을 따른다.**

| ID | 확정 내용 | DOMAIN.md 수정 제안 |
| --- | --- | --- |
| **D-1** | **`반대`(OPPOSED) verdict를 두지 않는다.** `Review.verdict`는 `승인` / `변경요청` 두 가지다 | `Review.verdict`에서 `반대` 삭제 |
| **D-1** | **`반려` 상태를 두지 않는다.** GitHub PR의 close에 대응하는 **`취소`만** 둔다. 상태는 6개 — `리뷰대기 / 리뷰중 / 변경요청 / 승인 / 반영완료 / 취소` | `status`에서 `반려` 삭제 |
| **D-1** | **집계는 리뷰어별 최신 판정 하나만 본다.** `Review` 행은 이력으로 전부 남기되 정족수 계산은 회원별 최신 1건. 따라서 **같은 사람이 여러 번 리뷰할 수 있어야 한다.** 변경요청했던 사람이 다시 승인하면 변경요청 수가 0이 된다 | `Review`에 회원 식별자 필드 추가(아래) |
| **D-1** | **재교정으로 회차가 올라가도 이전 승인은 유지한다**(GitHub 기본 동작). `Review.targetRound`는 이력·표시용이고 **집계 필터로 쓰지 않는다** | `targetRound` 설명에 "집계에 쓰지 않음" 추가 |
| **D-3** | **리비전이 소유한다.** `ReviewRequest.revisionId`를 **제거**하고 `type`만 남긴다. `RevisionDocument`/`RevisionDictionary`가 `reviewRequestId`를 갖고 조회는 `findByReviewRequestId`. DB로 지킬 수 없는 다형 FK가 사라지고, 재교정 회차마다 리비전이 생기는 1:N 구조와도 맞는다 | `ReviewRequest.revisionId` 행 삭제 |
| **D-2** | D-3으로 해소. 전 도메인 PK는 `Long` + `IDENTITY`이고 `UUID`를 쓰지 않는다 | `revisionId : UUID`, `Reexamine.revisionId : UUID`, `Comment.targetItemId : UUID` 표기 정리 |
| **Reviewer** | **지정은 하되 강제하지 않는다.** GitHub의 review request처럼 알림·목록 필터 용도이고, **지정되지 않은 워크스페이스 참여자도 리뷰를 달 수 있고 정족수에 산입된다.** `Reviewer.required` 필드는 **사용하지 않는다** | `required` 미사용 명시 또는 삭제 |
| **리뷰어 수 정책** | `docs/DOMAIN.md`의 "리뷰 요청에 지정한 리뷰어 수가 설정값보다 적으면 요청을 만들 수 없다"는 **생성 제약이 아니라 발행(Revise) 제약**이다 — "최소 리뷰 인원보다 적게 리뷰가 달렸으면 리비전을 발행할 수 없다"는 뜻. **요청 생성 시 리뷰어 수를 검증하지 않는다** | 문구를 발행 제약으로 명확화 — 반영 완료 |
| **D-12·D-13** | 정족수는 **실시간 조회**다. `ReviewRequest`에 스냅샷 필드를 두지 않고 **발행 시점의 `Workspace.ruleSet` 값**으로 계산한다(GitHub가 브랜치 보호 규칙을 실시간 참조하는 것과 같다). DOMAIN.md 미확정 항목이 모델 추가 없이 해소된다 | 없음 — 반영 완료 |
| **D-18** | 도메인 레벨 패키지 구조만 지키고 하위 디렉터리는 담당자 판단. 기본값은 `presentation/dto/`·`service/model/`, **ErrorCode는 `{domain}/exception/`** | — |
| **D-4** | **`RevisedTerm`을 MVP1에서 제외한다**(1절) | `RevisedTerm` 표 취소선 |
| **D-5** | **`RevisionDictionary`는 `draftDictionaryId` 참조만 갖는다.** 스냅샷 엔티티(`ProposedTerm`)를 만들지 않는다. 초안이 `리뷰요청됨`인 동안 후보어가 잠기므로(DraftDictionary D-11) **초안 자체가 스냅샷 역할을 한다.** 다만 변경요청으로 초안이 다시 열리면 이전 회차 내용은 재현할 수 없다 — 받아들인 한계다 | `proposedTerms` 행 취소선 |
| **D-14** | **수정본을 생성한다**(원본을 수정하지 않는다). `baseVersionNo` + `proposedBody` + `resultVersionNo` + `DocumentVersion` 구조가 이미 이 방식을 함의하고 있었다. `REQUIREMENTS.md:90`의 미결정-9가 해소됐다 | 없음 |
| **D-17** | **`docs/API.md`에 페이징·정렬 규격과 버저닝 방침을 신설한다**(10절 규약과 동일). 버저닝은 `/api` 유지 | — |
| **D-16** | **`docs/ARCHITECTURE.md`를 수정하지 않는다.** 나열된 애노테이션은 예시이고 핵심은 "JPA 매핑 애노테이션은 허용한다 / Spring·Web 의존은 두지 않는다"다 | — |
| **정족수 상한** | **실효 정족수 = `ruleSet` 값이다.** `min(ruleSet, 참여자 수)`를 계산하지 않는다 — 상한 검증은 룰셋을 설정·수정하는 **Workspace 쪽 책임**이다(`RuleSet.java:11-12` 주석이 이미 예고). 참여자 이탈로 정족수를 채울 수 없게 되면 Admin 이상이 발행 조건을 우회한다. `WorkspacePolicyPort`에서 `participantCount`가 사라진다 | `:478` 문구를 이 방식으로 |
| **포트 소유** | **각 소비 도메인이 포트를 정의하고 자기 어댑터까지 구현한다.** 어댑터는 `{domain}/infra/adapter/`에 두고 제공 도메인의 `infra`(Repository)만 참조한다 — 제공 도메인의 `implement`를 참조하면 `infra -> implement` 역방향이 된다 | — |

#### 발행(Revise) 가능 조건 — 이 도메인의 핵심 규칙

| 정족수 (`Workspace.ruleSet`의 해당 유형 값) | 발행 조건 |
| --- | --- |
| **0** | 승인·변경요청 존재 여부와 **무관하게 Admin 이상이 언제든 발행할 수 있다** |
| **1 이상** | `승인한 회원 수 >= 정족수` **AND** `변경요청 수 == 0` |

**실효 정족수는 `ruleSet` 값 그대로다.** `min(ruleSet 값, 참여자 수)`를 계산하지 않는다 — 상한(참여자 수 이하) 검증은 룰셋을 설정·수정하는 Workspace 쪽 책임이고(`DOMAIN.md:491`, `RuleSet.java:11-12`), 참여자 이탈로 정족수를 채울 수 없게 된 교착은 **Admin 이상의 발행**으로 푼다. 이 도메인은 참여자 수를 조회하지 않는다.

판정은 `ReviseEligibilityCalculator`(implement) **한 곳에만** 둔다.

### 2-2. 남은 결정 대기

**없다. 2026-09-10에 전건 확정했다.** 모든 Phase에 착수할 수 있다.

`docs/DOMAIN.md` 미확정 블록 중 이 도메인에 걸리는 것은 **참여자 삭제 방식**(내보낸 참여자를 행 삭제로 지울지 이력으로 남길지)뿐이고, 우리 설계는 어느 쪽이 되어도 동작한다. 13절 참조.

---

## 3. 도메인 모델

### ReviewRequest (애그리게이트 루트)

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | `@GeneratedValue(IDENTITY)` |
| `workspaceId` | `Long` | `workspace_id` | X | 논리 FK |
| `type` | `ReviewRequestType` | `type` | X | `DOCUMENT` / `DICTIONARY` |
| `title` | `String` | `title` | X | |
| `description` | `String` | `description` | O | `TEXT` |
| `requesterId` | `Long` | `requester_id` | X | 논리 FK(Member) |
| `status` | `ReviewRequestStatus` | `status` | X | 6개 값 |
| `approvedAt` | `OffsetDateTime` | `approved_at` | O | 정족수 충족 시점 |
| `revisedAt` | `OffsetDateTime` | `revised_at` | O | 반영 시점 |
| `version` | `Long` | `version` | X | `@Version` 낙관적 락(`NFR-REV-002`). Phase 4 |
| `createdBy` | `Long` | `created_by` | X | `updatable = false` |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | `BaseEntity` 상속 |

**`revisionId`는 없다**(D-3). `reviewers` 컬렉션 필드도 두지 않는다 — 자식 FK 단방향이다.

`createdBy`는 `requesterId`와 값이 같아지지만 둘 다 둔다. `createdBy`는 감사 필드 규약이고 `requesterId`는 도메인 개념이다.

### Reviewer — 지정 전용

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `reviewRequestId` | `Long` | `review_request_id` | X | **같은 도메인이라 DB FK를 건다** |
| `memberId` | `Long` | `member_id` | X | 워크스페이스 참여자여야 함(Phase 4 포트 검증) |
| `assignedAt` | `OffsetDateTime` | `assigned_at` | X | |
| `createdBy` | `Long` | `created_by` | X | 리뷰어를 지정한 사람 |

**`required` 필드를 두지 않는다**(2-1절). 지정은 알림·목록 필터 용도이고 리뷰 자격을 제한하지 않는다.

### RevisionDocument

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `reviewRequestId` | `Long` | `review_request_id` | X | **소유 방향**(D-3). DB FK |
| `documentId` | `Long` | `document_id` | X | 논리 FK |
| `baseVersionNo` | `int` | `base_version_no` | X | |
| `draftDocumentId` | `Long` | `draft_document_id` | X | 유래 초안. 논리 FK(크로스 도메인) |
| `proposedBody` | `String` | `proposed_body` | X | `TEXT`. **본문 전체 스냅샷** |
| `reexamineRound` | `int` | `reexamine_round` | X | 0이 최초 제출 |
| `resultVersionNo` | `int` | `result_version_no` | O | Revise 후 채워짐 |
| `createdBy` | `Long` | `created_by` | X | |

### RevisionDictionary

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `reviewRequestId` | `Long` | `review_request_id` | X | **소유 방향**(D-3). DB FK |
| `dictionaryId` | `Long` | `dictionary_id` | X | 논리 FK |
| `baseVersionNo` | `int` | `base_version_no` | X | |
| `draftDictionaryId` | `Long` | `draft_dictionary_id` | X | 유래 초안. 논리 FK |
| `reexamineRound` | `int` | `reexamine_round` | X | 0이 최초 제출(DOMAIN.md에 설명 누락 — `RevisionDocument`와 동일하게 읽는다) |
| `resultVersionNo` | `int` | `result_version_no` | O | |
| `createdBy` | `Long` | `created_by` | X | |

`proposedTerms` 필드는 **두지 않는다**(D-5). 등재 제안 항목은 `draftDictionaryId`로 초안을 다시 조회해 얻는다. 초안이 `리뷰요청됨`인 동안 후보어가 잠기므로 초안이 스냅샷 역할을 한다.

**리뷰 당시 내용을 항상 재현할 수는 없다.** 변경요청으로 초안이 `교정중`으로 열려 판정이 바뀌면 이전 회차 내용은 사라진다. 이 한계를 받아들이고 스냅샷 엔티티를 두지 않기로 확정했다 — 필요해지면 MVP2에 `ProposedTerm`(FK `revisionDictionaryId`)을 신설한다(13절).

### Review

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `reviewRequestId` | `Long` | `review_request_id` | X | DB FK |
| **`memberId`** | `Long` | `member_id` | X | **DOMAIN.md는 `reviewerId : ReviewerId`지만, 지정되지 않은 참여자도 리뷰할 수 있으므로 회원을 직접 가리킨다.** 집계 기준 키다 |
| `targetRound` | `int` | `target_round` | X | 어느 회차를 봤는지. **이력·표시용이고 집계 필터로 쓰지 않는다** |
| `verdict` | `ReviewVerdict` | `verdict` | X | `APPROVED` / `CHANGES_REQUESTED` |
| `submittedAt` | `OffsetDateTime` | `submitted_at` | X | |
| `createdBy` | `Long` | `created_by` | X | |

`comments` 컬렉션 필드는 두지 않는다 — 자식 FK 단방향이다.

### Comment

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `reviewId` | `Long` | `review_id` | X | DB FK |
| `authorId` | `Long` | `author_id` | X | **사람만**(AI 코멘트 금지) |
| `content` | `String` | `content` | X | `TEXT` |
| `anchor` | `TextRange` | `start_offset`, `end_offset` | O | `@Embedded`. `common/domain/TextRange`(10절). 전체 대상이면 null |
| `targetItemId` | `Long` | `target_item_id` | O | 사전 리비전에서는 후보어 단위. **`UUID`가 아니라 `Long`**(D-2) |
| `parentId` | `Long` | `parent_id` | O | 답글. 자기 참조 |
| `resolved` | `boolean` | `resolved` | X | |
| `createdBy` | `Long` | `created_by` | X | |

### Reexamine (이력)

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| **`reviewRequestId`** | `Long` | `review_request_id` | X | **DOMAIN.md는 `revisionId : UUID`지만, 리비전은 `(reviewRequestId, round)`로 찾을 수 있으므로 요청을 직접 가리킨다**(D-2·D-3) |
| `round` | `int` | `round` | X | 1부터 증가 |
| `performedBy` | `Long` | `performed_by` | X | 보통 요청자 |
| `addressedCommentIds` | `List<Long>` | 별도 테이블 | — | `@ElementCollection` |
| `performedAt` | `OffsetDateTime` | `performed_at` | X | |
| `createdBy` | `Long` | `created_by` | X | |

**이력이라 생성 이후 변하지 않는다.** `updatedAt`은 `BaseEntity`가 주지만 갱신 경로를 만들지 않는다.

### Revise (이력)

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `reviewRequestId` | `Long` | `review_request_id` | X | DB FK |
| `resultVersionNo` | `int` | `result_version_no` | X | 새로 만들어진 버전 |
| `performedBy` | `Long` | `performed_by` | X | |
| `performedAt` | `OffsetDateTime` | `performed_at` | X | |
| `createdBy` | `Long` | `created_by` | X | |

**`targetType`을 두지 않는다** — `reviewRequestId`로 `ReviewRequest.type`을 알 수 있어 중복이다. DOMAIN.md 수정 제안에 포함한다.

### 애그리게이트 경계와 파일 소유권

`ReviewRequest`가 루트지만 자식이 많아 **JPA 컬렉션으로 묶지 않는다.** 전부 자식 FK 단방향 + 별도 Repository다. 불변식은 implement가 두 객체를 함께 받아 검증한다.

파일이 많으므로 **하위 개념별로 소유권을 나눠** 도메인 내부에서도 병렬 작업한다.

| 묶음 | 클래스 | 태스크 |
| --- | --- | --- |
| A | `ReviewRequest`, `ReviewRequestType`, `ReviewRequestStatus`, `Reviewer` | RR-1, RR-2a |
| B | `RevisionDocument`, `RevisionDictionary` | RR-2b, RR-2c |
| C | `Review`, `ReviewVerdict`, `Comment` | RR-3a, RR-3b |
| D | `Reexamine`, `Revise` | RR-4a, RR-4b |

### enum

```
ReviewRequestType { DOCUMENT, DICTIONARY }
ReviewRequestStatus { PENDING_REVIEW, IN_REVIEW, CHANGES_REQUESTED, APPROVED, REVISED, CANCELED }
//                    리뷰대기 / 리뷰중 / 변경요청 / 승인 / 반영완료 / 취소   — 반려 없음
ReviewVerdict { APPROVED, CHANGES_REQUESTED }   // 반대 없음
```

---

## 4. 상태 전이와 도메인 메서드

### ReviewRequestStatus

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (신규) | `PENDING_REVIEW` | `create(...)` | 초안이 교정완료. **리뷰어 수를 검증하지 않는다**(2-1절) |
| `PENDING_REVIEW` | `IN_REVIEW` | `startReview()` | 첫 `Review` 제출 |
| `IN_REVIEW` | `CHANGES_REQUESTED` | `requestChanges()` | 최신 집계에 `CHANGES_REQUESTED`가 1건 이상 |
| `CHANGES_REQUESTED` | `IN_REVIEW` | `resumeReview()` | 변경요청 수가 0이 됨 — **변경요청했던 회원이 다시 승인**하거나 재교정이 수행됨 |
| `IN_REVIEW` | `APPROVED` | `approve(approvedAt)` | 정족수 1 이상이고 `승인 수 >= 실효 정족수` AND `변경요청 == 0` |
| `APPROVED` | `CHANGES_REQUESTED` | `requestChanges()` | 승인 후에도 새 변경요청이 달릴 수 있다(GitHub 동작) |
| `APPROVED` | `REVISED` | `markRevised(revisedAt)` | `Revise` 수행. Admin 이상 |
| `PENDING_REVIEW`/`IN_REVIEW`/`CHANGES_REQUESTED` | `REVISED` | `markRevised(revisedAt)` | **정족수가 0인 경우에만.** Admin이 언제든 발행 가능하므로 `APPROVED`를 거치지 않는다 |
| `PENDING_REVIEW`/`IN_REVIEW`/`CHANGES_REQUESTED`/`APPROVED` | `CANCELED` | `cancel(actorId)` | 요청자 본인 또는 Admin 이상 |
| `REVISED`/`CANCELED` | — | — | 종단 |

`APPROVED`는 **정족수 1 이상 경로의 중간 상태**다. 정족수가 0이면 이 상태를 거치지 않고 바로 `REVISED`로 갈 수 있다.

`approvedAt`은 정족수를 처음 충족한 시점에 기록하고, 이후 변경요청으로 되돌아갔다가 다시 충족하면 갱신한다.

### ReviewVerdict

`Review`는 **이력이라 verdict 전이가 없다.** 제출 시 확정되고 수정하지 않는다. 같은 회원이 판정을 바꾸려면 **새 `Review`를 제출**하며, 집계는 최신 1건만 본다.

`UNIQUE(review_request_id, member_id, target_round)`를 **걸지 않는다** — 같은 회차에서도 판정을 바꿔 다시 제출할 수 있어야 한다(변경요청 → 승인).

### 도메인 메서드 시그니처

```java
// ReviewRequest
static ReviewRequest create(Long workspaceId, ReviewRequestType type, String title,
        String description, Long requesterId, Long createdBy);
void changeContent(String title, String description);
void startReview();
void requestChanges();
void resumeReview();
void approve(OffsetDateTime approvedAt);
void markRevised(OffsetDateTime revisedAt);
void cancel(Long actorId);
boolean isBlockingDraftCreation();   // status ∈ {PENDING_REVIEW, IN_REVIEW, CHANGES_REQUESTED}
boolean isReviewable();
boolean isReexaminable();            // status == CHANGES_REQUESTED
boolean isRevised();

// Review
static Review submit(Long reviewRequestId, Long memberId, int targetRound, ReviewVerdict verdict, Long createdBy);
boolean isApproval();

// Comment
static Comment create(Long reviewId, Long authorId, String content, TextRange anchor,
        Long targetItemId, Long parentId, Long createdBy);
void resolve();
void reopen();

// Reexamine
static Reexamine perform(Long reviewRequestId, int round, Long performedBy, List<Long> addressedCommentIds);
```

`isBlockingDraftCreation()`이 **DraftDocument·DraftDictionary가 소비하는 포트의 구현 근거**다. 이 세 상태 동안 대상에 새 초안을 만들 수 없다.

---

## 5. 스키마와 Flyway

**대역: 600–699** (`backend/CLAUDE.md`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 | Phase |
| --- | --- | --- |
| `V600__create_review_request.sql` | `review_request` | 1 |
| `V610__create_reviewer.sql` | `reviewer` | 2 |
| `V620__create_revision.sql` | `revision_document`, `revision_dictionary` | 2 |
| `V630__create_review_and_comment.sql` | `review`, `comment` | 3 |
| `V640__create_reexamine_and_revise.sql` | `reexamine`, `reexamine_addressed_comment`, `revise` | 4 |
| `V650__add_review_request_version.sql` | `version` 컬럼(낙관적 락) | 4 |

```sql
-- V600
create table review_request (
    id           bigint       not null auto_increment,
    workspace_id bigint       not null,
    type         varchar(20)  not null,
    title        varchar(255) not null,
    description  text,
    requester_id bigint       not null,
    status       varchar(30)  not null,
    approved_at  datetime(6),
    revised_at   datetime(6),
    created_by   bigint       not null,
    created_at   datetime(6)  not null,
    updated_at   datetime(6)  not null,
    deleted_at   datetime(6),
    primary key (id)
);

create index idx_review_request_workspace_status on review_request (workspace_id, status);
create index idx_review_request_type on review_request (type, status);

-- V630 (일부)
create table review (
    id                bigint      not null auto_increment,
    review_request_id bigint      not null,
    member_id         bigint      not null,
    target_round      int         not null,
    verdict           varchar(30) not null,
    submitted_at      datetime(6) not null,
    created_by        bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_review_review_request
        foreign key (review_request_id) references review_request (id)
);

create index idx_review_request_member_submitted
    on review (review_request_id, member_id, submitted_at desc);
```

- `workspace_id`·`requester_id`·`member_id`·`document_id`·`dictionary_id`·`draft_document_id`·`draft_dictionary_id`에는 **FK 제약을 걸지 않는다**(크로스 도메인 논리 FK). 같은 도메인 내부는 건다.
- `idx_review_request_member_submitted`가 **리뷰어별 최신 판정 집계의 인덱스**다. `member_id`로 그룹핑해 `submitted_at` 최댓값을 찾는 쿼리를 타야 한다.
- `UNIQUE(review_request_id, member_id, target_round)`를 **걸지 않는다**(4절).
- 마이그레이션은 **Phase 1 필수 산출물**이다. `support/RepositoryTestSupport`가 Flyway로 스키마를 만든다.

---

## 6. 패키지와 파일

```
com.ubidict.backend.reviewrequest
├── presentation
│   ├── ReviewRequestController.java             P1
│   ├── ReviewerController.java                  P2a
│   ├── RevisionController.java                  P2b/c
│   ├── ReviewController.java                    P3a
│   ├── CommentController.java                   P3b
│   ├── ReexamineController.java                 P4a
│   ├── ReviseController.java                    P4b
│   └── dto
│       ├── CreateReviewRequestRequest.java      P1
│       ├── UpdateReviewRequestRequest.java      P1
│       ├── ReviewRequestResponse.java           P1
│       ├── AssignReviewerRequest.java           P2a
│       ├── ReviewerResponse.java                P2a
│       ├── SubmitRevisionDocumentRequest.java   P2b
│       ├── SubmitRevisionDictionaryRequest.java P2c
│       ├── RevisionResponse.java                P2b/c
│       ├── SubmitReviewRequest.java             P3a
│       ├── ReviewResponse.java                  P3a
│       ├── ReviewProgressResponse.java          P3c
│       ├── AddCommentRequest.java               P3b
│       ├── ResolveCommentRequest.java           P3b
│       ├── CommentResponse.java                 P3b
│       ├── PerformReexamineRequest.java         P4a
│       ├── ReexamineResponse.java               P4a
│       └── ReviseResponse.java                  P4b
├── service
│   ├── ReviewRequestService.java                P1
│   ├── ReviewerService.java                     P2a
│   ├── RevisionService.java                     P2b/c
│   ├── ReviewService.java                       P3a
│   ├── CommentService.java                      P3b
│   ├── ReexamineService.java                    P4a
│   ├── ReviseService.java                       P4b
│   └── model
│       ├── CreateReviewRequestCommand.java      P1
│       ├── UpdateReviewRequestCommand.java      P1
│       ├── CancelReviewRequestCommand.java      P1
│       ├── ReviewRequestResult.java             P1
│       ├── ReviewRequestSearchQuery.java        P2d
│       ├── AssignReviewerCommand.java           P2a
│       ├── ReviewerResult.java                  P2a
│       ├── SubmitRevisionCommand.java           P2b/c
│       ├── RevisionResult.java                  P2b/c
│       ├── SubmitReviewCommand.java             P3a
│       ├── ReviewResult.java                    P3a
│       ├── ReviewProgressResult.java            P3c
│       ├── AddCommentCommand.java               P3b
│       ├── ResolveCommentCommand.java           P3b
│       ├── CommentResult.java                   P3b
│       ├── PerformReexamineCommand.java         P4a
│       ├── PerformReviseCommand.java            P4b
│       ├── RequestDocumentReviewCommand.java    P4
│       └── RequestDictionaryReviewCommand.java  P4
├── implement
│   ├── ReviewRequestReader.java                 P1
│   ├── ReviewRequestWriter.java                 P1
│   ├── ReviewRequestRemover.java                P1
│   ├── ReviewerReader.java                      P2a
│   ├── ReviewerWriter.java                      P2a
│   ├── ReviewerDuplicationValidator.java        P2a
│   ├── RevisionReader.java                      P2b/c
│   ├── RevisionWriter.java                      P2b/c
│   ├── RevisionTypeValidator.java               P2b/c
│   ├── ReviewReader.java                        P3a
│   ├── ReviewWriter.java                        P3a
│   ├── LatestReviewAggregator.java              P3c  ← 회원별 최신 판정 집계
│   ├── ReviseEligibilityCalculator.java         P3c  ← 발행 가능 판정
│   ├── ReviewRequestStatusPolicy.java           P3c
│   ├── CommentReader.java                       P3b
│   ├── CommentWriter.java                       P3b
│   ├── ReexamineWriter.java                     P4a
│   ├── ReexamineRoundCalculator.java            P4a
│   ├── ReviseProcessor.java                     P4b
│   ├── ApprovalAuthorityValidator.java          P4c
│   └── ReviewRequestEventPublisher.java         P4d
├── infra
│   ├── ReviewRequestRepository.java             P1
│   ├── ReviewerRepository.java                  P2a
│   ├── RevisionDocumentRepository.java          P2b
│   ├── RevisionDictionaryRepository.java        P2c
│   ├── ReviewRepository.java                    P3a
│   ├── CommentRepository.java                   P3b
│   ├── ReexamineRepository.java                 P4a
│   ├── ReviseRepository.java                    P4b
│   ├── port
│   │   ├── WorkspacePolicyPort.java             P3c
│   │   ├── DraftDocumentQueryPort.java          P4
│   │   ├── DraftDictionaryQueryPort.java        P4
│   │   ├── DocumentVersionPublishPort.java      P4b
│   │   ├── DictionaryVersionPublishPort.java    P4b
│   │   ├── DraftDocumentSnapshot.java           P4
│   │   ├── DraftDictionarySnapshot.java         P4
│   │   └── stub
│   │       └── (각 포트의 프로퍼티 조건부 스텁)
│   └── adapter
│       ├── DraftDocumentReviewRequestQueryAdapter.java    P4  ← DD가 정의한 포트 구현
│       └── DraftDictionaryReviewRequestQueryAdapter.java  P4  ← DI가 정의한 포트 구현
├── domain
│   ├── ReviewRequest.java                       P1
│   ├── ReviewRequestType.java                   P1
│   ├── ReviewRequestStatus.java                 P1
│   ├── Reviewer.java                            P2a
│   ├── RevisionDocument.java                    P2b
│   ├── RevisionDictionary.java                  P2c
│   ├── Review.java                              P3a
│   ├── ReviewVerdict.java                       P3a
│   ├── Comment.java                             P3b
│   ├── Reexamine.java                           P4a
│   ├── Revise.java                              P4b
│   └── event
│       ├── ReviewRequestCreatedEvent.java       P4d
│       ├── ReviewSubmittedEvent.java            P4d
│       ├── ReviewRequestChangesRequestedEvent.java P4d
│       ├── ReviewRequestRevisedEvent.java       P4d
│       └── ReviewRequestCanceledEvent.java      P4d
└── exception
    └── ReviewRequestErrorCode.java              P1
```

---

## 7. API 명세

접두사 `/api`(버저닝 없음). 컨트롤러는 **`ResponseEntity<T>`를 반환**하고 `@ResponseStatus`를 쓰지 않는다. **Swagger 애노테이션은 쓰지 않는다.**

요청자 식별은 **`@RequestParam Long memberId`** 다. 컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 **"인증 도입 전까지 운영 배포 대상이 아니다"**를 적는다.

| Phase | Method | Path | 요청 | 응답 | 성공 | 에러 | 태그 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | POST | `/api/review-requests` | `CreateReviewRequestRequest{workspaceId, type, title, description?}` | `ReviewRequestResponse` | 201 | 400 `COMMON_INVALID_REQUEST` | **INTERNALIZE** |
| 1 | GET | `/api/review-requests/{reviewRequestId}` | — | `ReviewRequestResponse` | 200 | 404 `REVIEW_REQUEST_NOT_FOUND` | KEEP |
| 1 | PATCH | `/api/review-requests/{reviewRequestId}` | `UpdateReviewRequestRequest{title?, description?}` | `ReviewRequestResponse` | 200 | 404 | KEEP |
| 1 | POST | `/api/review-requests/{id}/cancellation` | — | `ReviewRequestResponse` | 200 | 403 `..._NOT_REQUESTER`, 409 `..._INVALID_STATUS_TRANSITION` | KEEP |
| 2d | GET | `/api/review-requests` | `workspaceId`(필수), `type?`, `status?`, `requesterId?`, `reviewerMemberId?`, `page`, `size`, `sort` | `PageResponse<ReviewRequestResponse>` | 200 | 400 `..._WORKSPACE_ID_REQUIRED` | KEEP |
| 2a | POST | `/api/review-requests/{id}/reviewers` | `AssignReviewerRequest{memberId}` | `ReviewerResponse` | 201 | 409 `..._DUPLICATE_REVIEWER`, 403 `..._NOT_A_PARTICIPANT`(P3c 이후) | KEEP |
| 2a | GET | `/api/review-requests/{id}/reviewers` | — | `List<ReviewerResponse>` | 200 | 404 | KEEP |
| 2a | DELETE | `/api/review-requests/{id}/reviewers/{reviewerId}` | — | — | 204 | 404 `..._REVIEWER_NOT_FOUND` | KEEP |
| 2b | POST | `/api/review-requests/{id}/revision-documents` | `SubmitRevisionDocumentRequest{documentId, baseVersionNo, draftDocumentId, proposedBody}` | `RevisionResponse` | 201 | 409 `..._REVISION_ALREADY_EXISTS`, 400 `..._TYPE_MISMATCHED` | **INTERNALIZE** |
| 2b | GET | `/api/review-requests/{id}/revision-documents` | `round?` | `List<RevisionResponse>` | 200 | 404 `..._REVISION_NOT_FOUND` | KEEP |
| 2c | POST | `/api/review-requests/{id}/revision-dictionaries` | `SubmitRevisionDictionaryRequest{dictionaryId, baseVersionNo, draftDictionaryId}` | `RevisionResponse` | 201 | 409, 400 `..._TYPE_MISMATCHED` | **INTERNALIZE** |
| 2c | GET | `/api/review-requests/{id}/revision-dictionaries` | `round?` | `List<RevisionResponse>` | 200 | 404 | KEEP |
| 3a | POST | `/api/review-requests/{id}/reviews` | `SubmitReviewRequest{targetRound, verdict, comments?[]}` | `ReviewResponse` | 201 | 403 `..._NOT_A_PARTICIPANT`, 409 `..._NOT_REVIEWABLE_STATUS` | KEEP |
| 3a | GET | `/api/review-requests/{id}/reviews` | `targetRound?` | `List<ReviewResponse>` | 200 | 404 | KEEP |
| 3c | GET | `/api/review-requests/{id}/review-progress` | — | `ReviewProgressResponse{requiredReviewerCount, approvedCount, changesRequestedCount, reviseEligible}` | 200 | 404 | KEEP |
| 3b | POST | `/api/reviews/{reviewId}/comments` | `AddCommentRequest{content, anchor?, targetItemId?, parentId?}` | `CommentResponse` | 201 | 404 `..._REVIEW_NOT_FOUND`, 400 `..._INVALID_COMMENT_PARENT` | KEEP |
| 3b | GET | `/api/review-requests/{id}/comments` | `resolved?`, `targetItemId?` | `List<CommentResponse>`(트리) | 200 | 404 | KEEP |
| 3b | PATCH | `/api/comments/{commentId}/resolution` | `ResolveCommentRequest{resolved}` | `CommentResponse` | 200 | 404 `..._COMMENT_NOT_FOUND` | KEEP |
| 4a | POST | `/api/review-requests/{id}/reexaminations` | `PerformReexamineRequest{proposedBody?, addressedCommentIds?}` | `ReexamineResponse{round, performedAt}` | 201 | 409 `..._NOT_REEXAMINABLE`, 403 `..._NOT_REQUESTER` | KEEP |
| 4a | GET | `/api/review-requests/{id}/reexaminations` | — | `List<ReexamineResponse>` | 200 | 404 | KEEP |
| 4b | POST | `/api/review-requests/{id}/revision` | — | `ReviseResponse{resultVersionNo, performedAt}` | 200 | 403 `..._ACCESS_DENIED`, 409 `..._NOT_ELIGIBLE_FOR_REVISE`, 409 `..._ALREADY_REVISED`, 409 `..._CONCURRENT_MODIFICATION` | KEEP |
| 4 | POST | `/api/draft-documents/{id}/review-request` | `{title, description?, reviewerMemberIds?}` | `ReviewRequestResponse` | 201 | 409 `DRAFT_DOCUMENT_NOT_EXAMINED` | KEEP |
| 4 | POST | `/api/draft-dictionaries/{id}/review-request` | `{title, description?, reviewerMemberIds?}` | `ReviewRequestResponse` | 201 | 409 `DRAFT_DICTIONARY_CANDIDATE_TERM_UNDECIDED_EXISTS` | KEEP |

### 임시 API 태그

**INTERNALIZE** 대상 3건은 Phase 4에 **엔드포인트를 제거**한다.

| 엔드포인트 | 이유 | 대체 |
| --- | --- | --- |
| `POST /api/review-requests` | 리비전 없이 요청만 만드는 상태가 실제 흐름에 없다. 리뷰 요청은 초안에서 시작한다 | `POST /api/draft-documents/{id}/review-request`, `POST /api/draft-dictionaries/{id}/review-request` |
| `POST .../revision-documents` | 리비전은 요청 생성 시 **한 트랜잭션에서 함께** 만들어져야 한다 | `RequestDocumentReviewCommand` 내부 조립 |
| `POST .../revision-dictionaries` | 동일 | `RequestDictionaryReviewCommand` 내부 조립 |

`service` 메서드(`SubmitRevisionCommand` 처리)는 남고 컨트롤러만 사라진다. 그래서 **Phase 1~2의 컨트롤러를 얇게 유지한다** — Command 조립 외의 로직을 컨트롤러에 두지 않으면 엔드포인트 삭제가 파일 1개 수정으로 끝난다.

경로에 `/manual`·`/temp`를 붙이지 않는다. 임시성은 `deprecated` 플래그와 이 문서로 표현한다.

### 권한

| 대상 | 규칙 |
| --- | --- |
| 조회·리뷰 제출·코멘트 | 워크스페이스 참여자. **참여자가 아니면 403이 아니라 404**(리소스 존재를 드러내지 않는다 — `WorkspaceAccessValidator` 선례) |
| 취소 | 요청자 본인 또는 Admin 이상 |
| **발행(`POST .../revision`)** | **Admin 이상**(`NFR-REV-001`, `REQ-UPD-004`). 미만은 403 + WARN 감사 로그 |
| 재교정 | 요청자 본인 또는 Admin 이상 |

**Owner와 Admin은 사실상 동급**(`DOMAIN.md:459`)이므로 `validateAtLeast(workspaceId, memberId, Permission.ADMIN)` 한 줄이면 되고 Owner 전용 분기를 만들지 않는다.

---

## 8. ErrorCode

`reviewrequest/exception/ReviewRequestErrorCode.java`. `CommonErrorCode`와 같은 형태로 만든다.

| 상수 | status | message | Phase |
| --- | --- | --- | --- |
| `REVIEW_REQUEST_NOT_FOUND` | 404 | 리뷰 요청을 찾을 수 없습니다. | 1 |
| `REVIEW_REQUEST_TITLE_REQUIRED` | 400 | 제목은 필수입니다. | 1 |
| `REVIEW_REQUEST_INVALID_TYPE` | 400 | 요청 유형이 올바르지 않습니다. | 1 |
| `REVIEW_REQUEST_INVALID_STATUS_TRANSITION` | 409 | 현재 상태에서 수행할 수 없는 작업입니다. | 1 |
| `REVIEW_REQUEST_NOT_REQUESTER` | 403 | 요청자만 수행할 수 있습니다. | 1 |
| `REVIEW_REQUEST_WORKSPACE_ID_REQUIRED` | 400 | 워크스페이스 식별자는 필수입니다. | 2d |
| `REVIEW_REQUEST_REVIEWER_NOT_FOUND` | 404 | 리뷰어를 찾을 수 없습니다. | 2a |
| `REVIEW_REQUEST_DUPLICATE_REVIEWER` | 409 | 이미 지정된 리뷰어입니다. | 2a |
| `REVIEW_REQUEST_REVISION_NOT_FOUND` | 404 | 개정안을 찾을 수 없습니다. | 2b/c |
| `REVIEW_REQUEST_REVISION_ALREADY_EXISTS` | 409 | 해당 회차의 개정안이 이미 있습니다. | 2b/c |
| `REVIEW_REQUEST_TYPE_MISMATCHED` | 400 | 요청 유형과 개정안 종류가 맞지 않습니다. | 2b/c |
| `REVIEW_REQUEST_NOT_A_PARTICIPANT` | 403 | 워크스페이스 참여자만 리뷰할 수 있습니다. | 3a |
| `REVIEW_REQUEST_NOT_REVIEWABLE_STATUS` | 409 | 리뷰할 수 없는 상태입니다. | 3a |
| `REVIEW_REQUEST_STALE_TARGET_ROUND` | 400 | 대상 회차가 올바르지 않습니다. | 3a |
| `REVIEW_REQUEST_REVIEW_NOT_FOUND` | 404 | 리뷰를 찾을 수 없습니다. | 3b |
| `REVIEW_REQUEST_COMMENT_NOT_FOUND` | 404 | 코멘트를 찾을 수 없습니다. | 3b |
| `REVIEW_REQUEST_COMMENT_CONTENT_REQUIRED` | 400 | 코멘트 내용은 필수입니다. | 3b |
| `REVIEW_REQUEST_INVALID_COMMENT_PARENT` | 400 | 상위 코멘트가 올바르지 않습니다. | 3b |
| `REVIEW_REQUEST_NOT_REEXAMINABLE` | 409 | 변경요청 상태에서만 재교정할 수 있습니다. | 4a |
| `REVIEW_REQUEST_NOT_ELIGIBLE_FOR_REVISE` | 409 | 발행 조건을 충족하지 않았습니다. | 4b |
| `REVIEW_REQUEST_ALREADY_REVISED` | 409 | 이미 반영된 리뷰 요청입니다. | 4b |
| `REVIEW_REQUEST_ACCESS_DENIED` | 403 | 워크스페이스 관리자 이상만 수행할 수 있습니다. | 4c |
| `REVIEW_REQUEST_CONCURRENT_MODIFICATION` | 409 | 다른 사용자가 먼저 처리했습니다. 다시 시도해 주세요. | 4c |

`REVIEW_REQUEST_REJECTED` 계열 코드는 **두지 않는다** — 반려 상태가 없다(D-1).

`REVIEWER_LIMIT_EXCEEDED`도 **두지 않는다.** 리뷰어 지정 시 **각 대상이 워크스페이스 참여자인지만 검증하면**(`isParticipant`) 중복 방지 유니크와 합쳐져 지정 수가 참여자 수를 넘을 수 없다. 별도 상한 검사가 필요 없고 참여자 수를 조회하지 않아도 된다(2-1절 정족수 상한 결정과 같은 이유). 참여자가 아닌 회원을 지정하면 `NOT_A_PARTICIPANT`(403)다.

---

## 9. 크로스 도메인 계약

### 소비 포트 (이 도메인이 정의하고 스텁까지 제공)

| 포트 | 시그니처 | 제공 도메인 | 스텁 동작 | Phase |
| --- | --- | --- | --- | --- |
| `WorkspacePolicyPort` | `int requiredReviewerCount(Long workspaceId, ReviewRequestType type)`, `boolean isParticipant(Long workspaceId, Long memberId)`, `boolean hasAdminPermission(Long workspaceId, Long memberId)` | Workspace(**머지됨**) | `0` / `true` / `true` | **3c** |
| `DraftDocumentQueryPort` | `Optional<DraftDocumentSnapshot> read(Long draftDocumentId)` | DraftDocument | 고정 스냅샷 | 4 |
| `DraftDictionaryQueryPort` | `Optional<DraftDictionarySnapshot> read(Long draftDictionaryId)` | DraftDictionary | 고정 스냅샷 | 4 |
| `DocumentVersionPublishPort` | `int publish(Long documentId, int baseVersionNo, String body)` → 새 versionNo | Document(미착수) | 증가하는 더미 번호 | 4b |
| `DictionaryVersionPublishPort` | `int publish(Long dictionaryId, int baseVersionNo, List<Long> approvedCandidateTermIds)` → 새 versionNo | Dictionary(미착수) | 증가하는 더미 번호 | 4b |

스냅샷 record — `DraftDocumentSnapshot(Long documentId, int baseVersionNo, String draftBody, boolean examined)`, `DraftDictionarySnapshot(Long dictionaryId, DraftDictionaryStatus status, List<Long> approvedCandidateTermIds)`. **엔티티를 포트 시그니처에 노출하지 않는다.**

**`WorkspacePolicyPort`가 Phase 3부터 필요하다** — `ReviseEligibilityCalculator`가 정족수를 실시간 조회하기 때문이다(D-12).

`participantCount`는 **두지 않는다.** 실효 정족수가 `ruleSet` 값 그대로이므로 참여자 수가 필요 없다(2-1절).

### 어댑터 배치 (2026-09-10 확정)

**포트는 우리가 정의하고 어댑터도 우리가 구현한다.** 어댑터는 `reviewrequest/infra/adapter/`에 두고 **제공 도메인의 `infra`(Repository)만 참조한다** — 같은 레이어끼리라 방향 위반이 아니다. 제공 도메인의 `implement`(`ParticipantReader`·`WorkspaceAccessValidator` 등)를 참조하면 `infra -> implement`가 되어 `docs/ARCHITECTURE.md`의 역방향 참조 금지를 어긴다. 이 규약 덕분에 **`workspace` 패키지의 파일을 한 줄도 고치지 않는다.**

`WorkspacePolicyPort`의 실제 어댑터는 `workspace/infra/WorkspaceRepository`(룰셋)와 `ParticipantRepository`(참여 여부·권한)를 참조한다. `WorkspaceAccessValidator.validateAtLeast(...)`는 예외를 던지고 값을 돌려주지 않으므로 `boolean`을 반환하는 포트에 그대로 쓸 수 없다 — 어댑터가 `Participant.permission`을 읽어 `Permission.isAtLeast(...)`로 판단한다.

어댑터 선택은 프로퍼티로 한다 — `app.crossdomain.{name}.mode=stub|real`(기본 `stub`, `matchIfMissing = true`). `InMemoryEventPublisher`의 `@ConditionalOnProperty` 패턴을 따르고 `@ConditionalOnMissingBean`은 쓰지 않는다.

### 제공 포트 (다른 도메인이 우리를 볼 때 — 우리가 어댑터를 구현한다)

| 소비자가 정의한 포트 | 구현할 어댑터 | 근거 메서드 |
| --- | --- | --- |
| `draftdocument/infra/port/ReviewRequestQueryPort.hasOngoingDocumentReview(Long documentId)` | `DraftDocumentReviewRequestQueryAdapter` | `RevisionDocument.documentId`로 요청을 찾아 `isBlockingDraftCreation()` |
| `draftdictionary/infra/port/ReviewRequestQueryPort.hasOngoingDictionaryReview(Long dictionaryId)` | `DraftDictionaryReviewRequestQueryAdapter` | `RevisionDictionary.dictionaryId`로 요청을 찾아 `isBlockingDraftCreation()` |

**포트 시그니처는 소비자가 정의하고 우리는 구현만 한다.** 시그니처가 위와 다르면 소비 도메인의 정의를 따른다.

### 발행 이벤트

| 이벤트 | 페이로드 | 수신자 |
| --- | --- | --- |
| `ReviewRequestCreatedEvent` | `(Long reviewRequestId, Long workspaceId, ReviewRequestType type, Long sourceDraftId, Long requesterId, OffsetDateTime occurredAt)` | 초안 도메인(상태 전이), Notification(미정) |
| `ReviewSubmittedEvent` | `(Long reviewRequestId, Long reviewId, Long memberId, ReviewVerdict verdict, int targetRound, OffsetDateTime occurredAt)` | Notification(미정) |
| `ReviewRequestChangesRequestedEvent` | `(Long reviewRequestId, ReviewRequestType type, Long sourceDraftId, Long requesterId, OffsetDateTime occurredAt)` | **초안 도메인(사전 재개)**, Notification(미정) |
| `ReviewRequestRevisedEvent` | `(Long reviewRequestId, ReviewRequestType type, Long targetId, int resultVersionNo, OffsetDateTime occurredAt)` | 초안 도메인(종료), Document·Dictionary |
| `ReviewRequestCanceledEvent` | `(Long reviewRequestId, ReviewRequestType type, Long sourceDraftId, OffsetDateTime occurredAt)` | 초안 도메인(재개) |

**불변 record, 식별자·원시값·enum·시각만 담는다.** enum은 값 객체로 취급해 허용된다. 도메인 모델이 곧 JPA 엔티티이므로 엔티티·지연 로딩 프록시·연관 컬렉션을 담으면 안 된다. 인메모리 어댑터는 참조를 그대로 넘겨 로컬에서 위반이 드러나지 않으므로 **리뷰에서 확인한다.**

`ReviewRequestChangesRequestedEvent`에 **`type`과 `sourceDraftId`를 넣는 이유**: D-8을 4상태로 확정하면서 `DraftDictionary`가 이 이벤트를 받아 초안을 `교정중`으로 되돌려야 하게 됐다(후보어 판정을 다시 해야 새 회차 개정안을 만들 수 있다). 어느 초안인지 알려주지 않으면 수신 측이 리비전을 역조회해야 하고, 그건 초안 도메인이 우리 테이블을 들여다보는 것이 된다. `sourceDraftId`는 리비전의 `draftDocumentId` / `draftDictionaryId`다.

`implement`는 `common/infra/event/EventPublisher` 포트만 주입받는다.

### 수신 이벤트

| 이벤트 | 발행자 | 처리 | Phase |
| --- | --- | --- | --- |
| `DraftDictionaryReviewRequestedEvent` | DraftDictionary | 리뷰 요청 + 사전 리비전 생성 | 4 |
| 참여자 이탈 이벤트 | Workspace | 지정 리뷰어에서 제외 | **미정** — 13절 |

핸들러는 **멱등**하게 만든다. 같은 이벤트를 두 번 받아도 리뷰 요청이 하나만 생겨야 한다.

---

## 10. 공유 파일 규약

### 공유 자산과 주인

| 자산 | 최초 필요 | 규약 |
| --- | --- | --- |
| `common/domain/TextRange` | **DraftDocument Phase 2에서 만든다.** 우리는 Phase 3b(`Comment.anchor`)에서 **재사용**한다 | `@Embeddable` record, 필드 `startOffset`/`endOffset`, 컬럼 `start_offset`/`end_offset`, `startOffset <= endOffset` 검증. **직접 만들지 않는다** — DD가 올린 것을 쓴다 |
| `common/presentation/PageResponse`, `common/service/PageResult` | **DraftDocument (DD-2)** — 선착순이 아니다(`EXECUTION_ORDER.md` 3절) | 요청 `page`(0-base) / `size`(기본 20, 최대 100) / `sort=필드,방향`(화이트리스트 외 400). 응답 JSON 키 `content`/`page`/`size`/`totalElements`/`totalPages`. **service는 Spring `Page`를 반환하지 않는다** |

### 수정 금지 파일

`common/**`(위 자산 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`. 필요하면 통합 태스크로 넘기고 PR에 이유를 적는다.

### git worktree 운영

작업은 `../final-reviewrequest` worktree에서 한다. 워킹 디렉터리는 분리되지만 `.git`·Docker·호스트 포트는 공유된다.

| 항목 | 규칙 | 이유 |
| --- | --- | --- |
| 브랜치명 | `feat/WLSH-{티켓}-reviewrequest` | `.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다. 패턴이 없으면 티켓 추적이 끊긴다 |
| **`bootRun` 동시 실행 금지** | 한 번에 한 worktree만 | `spring-boot-docker-compose`가 worktree마다 별도 Compose 프로젝트(디렉터리명 기준)를 띄운다. MySQL·MongoDB·Redis·LGTM이 3배로 뜨고 호스트 포트 8080·3000이 겹쳐 `port is already allocated`로 죽는다 |
| **`./gradlew test` 동시 실행 주의** | 세 worktree에서 동시에 돌리지 않는다 | `RepositoryTestSupport`가 MySQL 컨테이너를, `IntegrationTestSupport`가 `TestcontainersConfiguration`(MySQL+Mongo+Redis+LGTM)을 띄운다. worktree 3개면 Gradle 데몬도 3개다. 루트 `CLAUDE.md`는 LGTM과 빌드 JVM이 겹치면 Docker Desktop 기본 메모리에서 OOM으로 죽는다고 기록한다 |
| **머지 순서** | **Phase 1~2를 가장 먼저 develop에 올린다** | DD Phase 4·DI Phase 4가 우리 상태·리비전을 포트 대상으로 기다린다. 이 도메인이 늦으면 두 도메인이 동시에 멈춘다 |
| 파일 소유권 | `reviewrequest/**` + Flyway 600–699 | worktree는 편집 충돌을 숨기고 머지 시점에 되살린다 |

**도메인 내부 병렬** — 파일이 많으므로 3절의 묶음 A/B/C/D로 소유권을 나눈다. RR-2a·2b·2c·2d는 서로 다른 파일만 건드리므로 동시에 진행할 수 있다.

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

AssertJ를 쓴다. JUnit `assertEquals`·`assertThrows`는 쓰지 않는다. Mockito는 BDD 스타일. `@DisplayName`은 한글 `~다.` 어투, 메서드명은 영어, 실패 케이스는 `{메서드명}_{실패이유}`.

### 테스트 클래스와 필수 케이스

| 클래스 | 계층 | 필수 케이스 | Phase |
| --- | --- | --- | --- |
| `domain/ReviewRequestTest` | Unit | **4절 전이표의 각 행과 1:1 대응.** `create`("리뷰 요청을 생성하면 리뷰대기 상태로 시작한다.") / `startReview` / `requestChanges` / `resumeReview` / `approve` / `markRevised` / `cancel` / `cancel_afterRevised`("반영된 리뷰 요청은 취소할 수 없다.") / `markRevised_notApproved` / `create_titleIsBlank` | 1·3c |
| `infra/ReviewRequestRepositoryTest` | Repository | `findByWorkspaceIdAndStatus` / `save_auditingFieldsAreSet` / `findById_deleted` | 1 |
| `service/ReviewRequestServiceTest` | Service | `create` / `update` / `cancel` / `cancel_notRequester`(403) | 1 |
| `presentation/ReviewRequestControllerTest` | Controller | `create`(201) / `create_typeIsInvalid`(400) / `read_notFound`(404 + 코드) | 1 |
| `domain/ReviewerTest` | Unit | `create` / `create_memberIdIsNull` | 2a |
| `implement/ReviewerDuplicationValidatorTest` | Unit | `validate_duplicate` / **`validate_notAParticipant`**("워크스페이스 참여자가 아닌 회원을 리뷰어로 지정하면 예외가 발생한다." — 2a는 스텁, 실제 검증은 3c부터) | 2a·3c |
| `infra/ReviewerRepositoryTest` | Repository | `existsByReviewRequestIdAndMemberId` / `countByReviewRequestId` | 2a |
| `domain/RevisionDocumentTest` | Unit | `create`("최초 제출한 개정안의 재교정 회차는 0이다.") / `create_proposedBodyIsBlank` | 2b |
| `implement/RevisionTypeValidatorTest` | Unit | `validate_typeMismatched`("사전 유형 요청에 문서 개정안을 붙이면 예외가 발생한다.") | 2b/c |
| `infra/RevisionDocumentRepositoryTest` | Repository | `findByReviewRequestId` / `findByReviewRequestIdAndReexamineRound` | 2b |
| `service/ReviewRequestSearchServiceTest` | Service | `search_filtersByStatus` / **`search_filtersByReviewerMemberId`**(조인) / `search_requiresWorkspaceId`(400) | 2d |
| `domain/ReviewTest` | Unit | `submit` / `submit_targetRoundIsNegative` | 3a |
| `domain/CommentTest` | Unit | `create`("전체 대상 코멘트는 위치가 비어 있을 수 있다.") / `create_contentIsBlank` / `create_parentIsSelf` / `resolve` | 3b |
| **`implement/LatestReviewAggregatorTest`** | Unit | **`aggregate_keepsOnlyLatestPerMember`**("같은 회원이 여러 번 리뷰하면 최신 판정만 집계한다.") / `aggregate_ignoresTargetRound`("회차가 달라도 함께 집계한다.") | 3c |
| **`implement/ReviseEligibilityCalculatorTest`** | Unit | **`isEligible_zeroQuorum`**("정족수가 0이면 변경요청이 있어도 발행할 수 있다.") / **`isEligible_changesRequestedRemains`**("변경요청이 남아 있으면 발행할 수 없다.") / **`isEligible_reReviewClearsChangesRequested`**("변경요청했던 리뷰어가 다시 승인하면 발행할 수 있다.") / **`isEligible_approvalSurvivesReexamine`**("재교정 후에도 이전 승인이 유지된다.") / `isEligible_quorumNotReached`("승인 수가 정족수에 못 미치면 발행할 수 없다.") | 3c |
| `infra/ReviewRepositoryTest` | Repository | `findByReviewRequestId` / `countByVerdict` / 최신 판정 조회 쿼리 검증 | 3a |
| `infra/CommentRepositoryTest` | Repository | `findByReviewRequestId`("리뷰 요청에 달린 코멘트를 리뷰 조인으로 조회한다.") / N+1 없음 | 3b |
| `service/ReviewSubmitServiceTest` | Service | `submit`("리뷰를 제출하면 리뷰 요청이 리뷰중이 된다.") / `submit_reachesQuorum`("정족수를 채우면 승인 상태가 된다.") / `submit_changesRequested` / `submit_notAParticipant`(403) / `submit_resubmitBySameMember`("같은 회원이 판정을 바꿔 다시 제출할 수 있다.") | 3a·3c |
| `service/CommentServiceTest` | Service | `addComment` / `addReply` / `resolve` / `addComment_invalidParent` | 3b |
| `domain/ReexamineTest` | Unit | `perform`("최초 재교정 회차는 1이다.") / `perform_roundIsZero` | 4a |
| `implement/ReexamineRoundCalculatorTest` | Unit | `nextRound` / `nextRound_firstReexamine` | 4a |
| `implement/ApprovalAuthorityValidatorTest` | Unit | `validate_regularPermission`("REGULAR가 반영을 시도하면 예외가 발생한다.") | 4c |
| `service/ReexamineServiceTest` | Service | `perform`("재교정하면 회차가 1 증가하고 리뷰중으로 돌아간다.") / `perform_notChangesRequested`(409) | 4a |
| `service/ReviseServiceTest` | Service | `perform`("발행하면 반영완료가 되고 반영일시가 기록된다.") / `perform_notEligible`(409) / `perform_alreadyRevised`(409) / `perform_zeroQuorumWithoutApproval`("정족수가 0이면 승인 없이 발행된다.") | 4b |
| `service/ReviewRequestConcurrencyTest` | Service | `revise_concurrentModification`("같은 요청을 동시에 발행하면 충돌을 감지한다.") | 4c |
| `service/ReviewRequestEventServiceTest` | Service | `approve_publishesEvent` / `revise_publishesEvent` / 같은 이벤트 2회 수신 시 멱등 | 4d |
| `presentation/ReviewControllerTest`, `CommentControllerTest`, `ReviseControllerTest` | Controller | 각 성공 1 + 검증실패 1 + 인가실패 1 + 상태충돌 1. `revise_regularPermission`(403 + `..._ACCESS_DENIED`), `read_notParticipant`(**404**) | 3·4 |

**`ReviseEligibilityCalculatorTest`와 `LatestReviewAggregatorTest`가 이 도메인의 핵심 테스트다.** 발행 조건 두 분기와 "리뷰어별 최신 판정만 집계"가 여기서 검증된다.

### Fixture

`reviewrequest/fixture/`에 둔다(src/test 전용). 정적 팩토리 + 내부 `static class XxxBuilder`, 기본값 보유, `ReflectionTestUtils.setField`로 식별자 주입.

- `ReviewRequestFixture.reviewRequest()` — `id`, `workspaceId`, `type`, `status` 노출
- `ReviewerFixture.reviewer()` — `id`, `reviewRequestId`, `memberId` 노출
- `RevisionDocumentFixture.revisionDocument()` / `RevisionDictionaryFixture.revisionDictionary()`
- `ReviewFixture.review()` — `memberId`, `verdict`, `targetRound`, `submittedAt` 노출(집계 테스트에 필수)
- `CommentFixture.comment()` — `reviewId`, `anchor`, `parentId`, `resolved` 노출

---

## 12. Phase와 태스크

| Phase | 목표 | 명시적 제외 |
| --- | --- | --- |
| 1 | 리뷰 요청 한 건을 만들고 읽고 고치고 취소한다 | 리뷰어, 리비전, 리뷰, 상태 전이(취소 제외) |
| 2 | 리뷰어를 지정하고 유형별 개정안을 등록하고 목록을 조회한다 | 리뷰 제출, 정족수 |
| 3 | 리뷰·코멘트를 받고 상태를 전이시키며 발행 가능 여부를 계산한다 | 재교정, 실제 발행, 인가 |
| 4 | 재교정 회차를 돌리고 승인된 개정안을 발행한다 | Notification 소비자, 실제 버전 생성 |

| ID | 태스크 | 산출물 | 일수 | 의존 | 병렬 |
| --- | --- | --- | --- | --- | --- |
| **RR-1** | 루트 CRUD + 취소 | 도메인 3 + `V600__` + Repository + implement 3 + Service + 모델 4 + Controller + DTO 3 + ErrorCode + 테스트 4 | 2 | — | — |
| **RR-2a** | 리뷰어 지정 | `Reviewer` + `V610__` + Repository + implement 3 + Service + 모델 3 + Controller + DTO 2 + 테스트 4 | 1.5 | RR-1 | 2b/2c/2d와 병렬 |
| **RR-2b** | 문서 개정안 | `RevisionDocument` + `V620__`(공유) + Repository + implement 3 + Service + 모델 2 + Controller + DTO 2 + 테스트 3 | 1.5 | RR-1, **DD-1** | |
| **RR-2c** | 사전 개정안 | `RevisionDictionary` + Repository + 테스트 2 | 1 | RR-1, **DI-1** | |
| **RR-2d** | 목록 조회 | `SearchQuery` + 조인 쿼리 + 테스트 1 | 1 | RR-1 | |
| **RR-3a** | 리뷰 제출 | `Review`, `ReviewVerdict` + `V630__`(공유) + Repository + implement 2 + Service + 모델 2 + Controller + DTO 2 + 테스트 4 | 2 | RR-2a | 3b와 부분 병렬 |
| **RR-3b** | 코멘트 | `Comment` + Repository + implement 2 + Service + 모델 3 + Controller + DTO 3 + 테스트 3 | 2 | RR-3a(도메인), **DD-2**(`TextRange`) | |
| **RR-3c** | 상태 전이 + 발행 판정 | 전이 메서드 7 + `LatestReviewAggregator` + `ReviseEligibilityCalculator` + `StatusPolicy` + `WorkspacePolicyPort` + 스텁 + 모델 1 + DTO 1 + 테스트 3 | 2 | RR-3a | — |
| **RR-4a** | 재교정 | `Reexamine` + `V640__`(공유) + Repository + implement 2 + Service + 모델 1 + Controller + DTO 2 + 테스트 3 | 2 | RR-3c | 4b와 병렬 |
| **RR-4b** | 발행 | `Revise` + Repository + `ReviseProcessor` + 발행 포트 2 + 스텁 2 + Service + 모델 1 + Controller + DTO 1 + 테스트 3 | 2 | RR-3c | |
| **RR-4c** | 인가 + 낙관적 락 | `ApprovalAuthorityValidator` + `@Version` + `V650__` + 테스트 3 | 1.5 | RR-4a, RR-4b | — |
| **RR-4d** | 이벤트 | 이벤트 record 5 + `EventPublisher` + 어댑터 2 + 테스트 2 | 1.5 | RR-4a, RR-4b | — |

합계 약 20인일. 2명이 나누면 묶음 A/B와 C/D로 갈라 진행할 수 있다.

### Phase별 DoD

**공통 (모든 Phase)**
- [ ] 각 Service가 **Repository를 직접 주입받지 않는다** — implement만 주입한다
- [ ] `domain` 패키지에 `@Component`·`@Transactional`·`ResponseEntity`·`HttpStatus`가 없다
- [ ] 컨트롤러가 도메인 모델을 직접 직렬화하지 않는다(Result → Response 변환만)
- [ ] Service의 상태 변경 지점에 `[XxxService.메서드명] ` prefix INFO 로그가 있다
- [ ] `./gradlew spotlessApply && ./gradlew check` 초록

**RR-1**
- [ ] `V600__create_review_request.sql`이 PR에 포함되고 Repository 테스트가 Flyway 스키마 위에서 통과한다
- [ ] `ReviewRequest`에 `revisionId` 필드가 **없다**(D-3)
- [ ] `ReviewRequestStatus`가 6개다 — `반려`가 없다(D-1)
- [ ] **가능한 한 빨리 develop에 머지한다** — DD-4·DI-4가 기다린다
- [ ] `docs/API.md` **끝에 자기 도메인 절만** 추가해 엔드포인트를 적는다. 공통 규칙·페이징·버저닝 절은 건드리지 않는다(`EXECUTION_ORDER.md` 3절)

**RR-2**
- [ ] 리비전이 `reviewRequestId`를 소유한다(D-3)
- [ ] `Reviewer`에 `required` 필드가 **없다**
- [ ] `type`과 리비전 종류의 정합성이 도메인 메서드로 검증된다(service `if`가 아니다)
- [ ] 생성 시 **리뷰어 수를 검증하지 않는다**(2-1절 — 발행 제약이다)
- [ ] `reviewerMemberId` 필터가 조인 쿼리로 동작하고 N+1이 없다
- [ ] `PageResponse` JSON 키가 공유 규약과 일치한다

**RR-3**
- [ ] `Review`가 `memberId`를 갖는다(지정되지 않은 참여자도 리뷰 가능)
- [ ] `ReviewVerdict`가 2개다 — `반대`가 없다(D-1)
- [ ] **같은 회원이 판정을 바꿔 다시 제출할 수 있다** — `UNIQUE(review_request_id, member_id, target_round)`를 걸지 않았다
- [ ] 집계가 **회원별 최신 1건** 기준이고 `targetRound`를 필터로 쓰지 않는다
- [ ] 4절 전이표의 **모든 행이 `ReviewRequestTest`의 테스트 메서드와 1:1 대응**한다
- [ ] 발행 판정이 `ReviseEligibilityCalculator` **한 곳에만** 있다
- [ ] `ReviseEligibilityCalculatorTest`의 5개 케이스가 통과한다
- [ ] **실효 정족수가 `ruleSet` 값 그대로다.** `participantCount`를 조회하지 않고 `min(...)` 계산이 없다(2-1절)
- [ ] 리뷰어 지정이 `isParticipant`로 검증되고 `REVIEWER_LIMIT_EXCEEDED` 같은 코드를 만들지 않았다
- [ ] 비참여자에게 404를 준다(403이 아니다)
- [ ] `TextRange`를 직접 만들지 않고 DD가 올린 것을 쓴다

**RR-4**
- [ ] 발행이 **원본을 수정하지 않고 새 버전을 만든다**(D-14). `resultVersionNo`가 채워진다
- [ ] `NFR-REV-001`: 발행이 ADMIN 미만에 403 + WARN 감사 로그를 남긴다(테스트로 확인)
- [ ] `NFR-REV-002`: `@Version`이 `ReviewRequest`에 있고 충돌 테스트가 통과한다
- [ ] 이벤트 record 5개에 엔티티·컬렉션·프록시가 없다
- [ ] `implement`가 `EventPublisher` 포트만 주입한다
- [ ] `Term`·`DocumentVersion`·`DictionaryVersion`을 **직접 만들지 않고** 포트로 넘긴다
- [ ] DD·DI가 정의한 포트의 어댑터 2개를 제공했다. 그 어댑터가 상대 도메인의 `infra`(Repository)만 참조한다
- [ ] **`workspace` 패키지의 파일을 한 줄도 고치지 않았다**
- [ ] `ReviewRequestChangesRequestedEvent`에 `type`·`sourceDraftId`가 있다 — DraftDictionary가 초안을 다시 열기 위해 필요하다(9절)
- [ ] `POST /api/review-requests` 등 INTERNALIZE 3건 제거가 `docs/API.md`에 반영됐다
- [ ] 이벤트 검증 테스트에 `@Transactional`이 없다

### 통합 태스크 (세 도메인 공통)

| ID | 내용 | 의존 |
| --- | --- | --- |
| T-INT-1 | `spring.flyway.out-of-order=true`(개발·테스트 프로파일만, 운영은 `false`) 추가 + `BaseEntityAuditingTest`에 남은 `ddl-auto=create-drop` + `flyway.enabled=false` 우회 정리 | 세 도메인 Phase 1 |
| T-INT-2 | 크로스 도메인 어댑터 배선 + `mode=real` 전환 + 스텁 제거 + 통합 테스트 3건(초안 생성 차단, 리뷰 요청 시 초안 상태 변경, 사전집 흐름 1개 제약) | 세 도메인 Phase 4 |
| T-INT-3 | `SecurityConfig` 추가 후 `addFilters = false`를 인가 테스트로 전환, `memberId` 파라미터를 인증 주체 해석으로 교체 | 인증 도메인(별건) |

---

## 13. 리스크와 열린 질문

| 항목 | 내용 | 대응 |
| --- | --- | --- |
| **참여자 이탈 이벤트** | "참여자를 내보내면 진행 중 리뷰 요청의 리뷰어에서 제외된다"(`DOMAIN.md:481`)를 지키려면 Workspace가 이벤트를 발행해야 한다. 발행 여부 미정 | 리뷰어 지정이 강제가 아니고 정족수는 "달린 승인 수"로 세므로, **지정 해제만 하면 되고 정족수 재계산은 자동**이다. 급하지 않다. Workspace 담당과 합의 |
| **참여자 삭제 방식 미확정** | DOMAIN.md 미확정 — 내보낸 참여자를 행 삭제로 지울지 이력으로 남길지. 행을 지우면 그 사람이 남긴 `Review`·`Comment`의 `memberId`가 고아가 된다 | "내보낸 참여자가 남긴 문서·리뷰·코멘트는 그대로 유지된다"(`DOMAIN.md:480`)가 이미 정책이므로 우리는 `memberId`를 논리 FK로 유지하고 조인하지 않는다. **어느 쪽으로 확정돼도 우리 설계는 그대로 동작한다** |
| **정족수 실시간 조회의 부작용** | 룰셋을 올리면 진행 중이던 요청이 갑자기 발행 불가가 된다 | GitHub도 같은 동작이다. D-12로 확정된 사항이며 `review-progress` 응답에 `requiredReviewerCount`와 `approvedCount`를 함께 넣어 사용자가 이유를 알 수 있게 한다 |
| **정족수 교착** | 참여자가 빠져 룰셋 값을 채울 수 없게 되면 승인만으로는 영구히 발행할 수 없다 | 상한 자동 완화를 하지 않기로 확정했다(2-1절). **Admin 이상의 발행**으로 푼다. 룰셋 상한 검증은 Workspace가 룰셋 수정 유스케이스에서 맡는다 |
| **`type = DOCUMENT` 요구사항 부재** | `REQ-REV-*`가 전부 사전 후보어 리뷰 기준이라 문서 리뷰 경로에 대응 ID가 없다 | 그룹을 신설하지 않고 **`REQ-UPD-004` 상세 설명에 정족수·재교정을 보강**하기로 확정했다(2026-09-10). 반영 완료 |
| **사전 재교정이 이전 회차 내용을 지운다** | D-5로 사전 개정안이 초안을 참조만 하므로, 변경요청으로 초안이 다시 열리면 이전 회차 리뷰 당시 내용을 재현할 수 없다 | 받아들인 한계다. 항목별 스냅샷이 실제로 필요해지면 `ProposedTerm`(FK `revisionDictionaryId`)을 MVP2에 신설한다 |
| **`BaseEntity` 리팩터링이 진행 중이다** | `refacotr/WLSH-86-base-entity`가 감사 애노테이션을 Spring Data 방식으로 교체하고 있다 | 신규 도메인은 `BaseEntity`를 그대로 상속하므로 영향이 없다. 머지 직후 develop을 동기화한다 |
| **Comment 트리 조회** | 답글이 자기 참조라 깊이가 깊어지면 N+1이 난다 | MVP1은 **1단 답글까지만** 허용하고(`parentId`의 `parentId`가 null이어야 함) 한 번에 로딩한다. `create_parentIsSelf`와 함께 깊이 제한 테스트를 둔다 |
| **RR이 크리티컬 패스** | Phase 1~2 지연이 DD·DI Phase 4를 모두 미룬다 | 묶음 A/B/C/D 분할로 2명 배치 가능. RR-1 → RR-2를 최우선으로 머지 |

### DOMAIN.md 수정 (2026-09-10 반영 완료)

1. `ReviewRequest.revisionId` 행 취소선 + 소유 방향 설명(D-3) ✔
2. `ReviewRequest.status`에서 `반려` 삭제, 6개로(D-1) ✔
3. `Review.verdict`에서 `반대` 삭제, 2개로(D-1) ✔
4. `Review.reviewerId : ReviewerId` → `memberId : MemberId`(지정되지 않은 참여자도 리뷰 가능) ✔
5. `Review.targetRound` 설명에 "집계에 쓰지 않고 이력·표시용" + 기준점 추가 ✔
6. `Reviewer.required` 취소선 + 미사용 명시 ✔
7. `Reexamine.revisionId : UUID` → `reviewRequestId : ReviewRequestId` ✔
8. `Comment.targetItemId : UUID` → `CandidateTermId` ✔
9. `Revise.targetType` 삭제(`ReviewRequest.type`과 중복) ✔
10. `RevisionDictionary.reexamineRound`에 "0이 최초 제출" 설명 추가 ✔
11. `RevisionDictionary.proposedTerms` 취소선(D-5) ✔
12. `RevisedTerm` 표 취소선 + 사유 3줄(D-4) ✔
13. 리뷰어 수 정책 문구를 **발행 제약**으로 명확화 + 정족수 상한 위임 ✔
14. `ReviewRequest.description` 타입 `string` → `String` ✔
15. `ReviewRequest.reviewers` 취소선 + "자식 FK 단방향" ✔
16. `ReviewRequest`에 `version`(낙관적 락) 행 추가 ✔
17. `Review.comments` 취소선 + 재제출·최신 판정 집계 규칙 인용 블록 ✔
18. RuleSet 절과 정책 절의 **룰셋 이력·스냅샷 미확정 항목 해소**(D-12·D-13) ✔
19. Notification `type`에서 `반려` 제거 ✔
20. 관계 표에 자식 소유 화살표 5행 추가 ✔

### 리뷰 체크리스트 대조

| 출처 | 항목 | 이 문서 반영 위치 |
| --- | --- | --- |
| ARCHITECTURE.md | service가 비즈니스 흐름으로 읽히는가 / Repository 직접 참조 없는가 / implement가 하나의 역할인가 / 레이어 건너뛰기 없는가 / 도메인 모델이 presentation까지 올라가지 않는가 / domain에 Spring·Web 의존 없는가 / 도메인 간 직접 참조가 늘지 않는가 / 이벤트 페이로드가 불변 record인가 / 핸들러가 멱등인가 / 트랜잭션 경계가 service에 있는가 | 12절 DoD |
| TEST.md | 계층별 전략 / AssertJ / DisplayName 어투 / Fixture Builder / 실패 케이스 네이밍 | 11절 |
| LOG.md | `[클래스.메서드] ` prefix / placeholder / Service는 상태 변경 완료 시점 / 403 감사 로그 | 12절 DoD |
| API.md | `/api` 접두사 / 생성 201·조회 200 / 도메인 모델 직렬화 금지 / userId를 본문에 담지 않음 | 7절 |
| EXCEPTION.md | 도메인별 ErrorCode enum / `{DOMAIN}_{REASON}` 형식 / status를 상수가 보유 | 8절 |
