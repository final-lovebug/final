# RevisionLog 구현 계획

2026-09-13 작성. 다른 7개 계획 문서와 같은 13절 목차를 쓴다. **이 문서는 큰 흐름 확정(2026-09-10) 이후에 쓰였으므로 `R-*`에 뒤집힌 서술이 없다.**

결정 원장은 `docs/plan/CONFLICTS.md`이고 이 도메인의 결정은 `D-55`~`D-61`이다. 실행 순서 규약은 `docs/plan/EXECUTION_ORDER.md`를 따른다.

---

## 1. 범위와 목표

사전집과 문서는 둘 다 **확정될 때마다 행이 쌓이는 append-only 버전 이력**이다. 그런데 그 이력이 「언제·누가」만 말하고 **「무엇이 어떻게 달라졌는지」는 말하지 못한다.** `GET .../dictionary/versions`는 `versionNo`·`publishedAt`·`termCount`뿐이고, `GET .../documents/{documentId}/versions`는 버전 메타뿐이다.

RevisionLog은 **새 버전이 확정되는 순간 이전 버전과 대조해 그 결과를 얼려 둔다.** 조회는 싸고, 과거 기록이 나중에 재계산되어 달라지지 않는다 — 이력이 되려면 변하지 않아야 한다(`D-56`).

동시에 **`DictionaryRevisedEvent`와 `DocumentEditedEvent`의 첫 소비자**가 된다. Notification이 리뷰 5종의 첫 소비자가 되며 `X-12`(「이벤트는 발행하고 소비자가 없어도 된다」)를 절반 해소했고, 이 도메인이 나머지 둘을 가져간다.

충족하는 요구사항 — `REQ-RL-001`(사전집 개정 이력) · `REQ-RL-002`(문서 개정 이력) · `REQ-RL-003`(타임라인 조회) · `REQ-RL-004`(변경 항목 조회) · `REQ-RL-005`(영향도 등급) · `NFR-RL-001`(중복 생성 방지). 더불어 `REQ-DIC-007`(버전 간 비교)을 **연속 버전에 한해** 충족하고 `NFR-DIC-001`(이력 append-only 적재)을 충족한다.

### MVP1 범위 밖

- **임의의 두 버전 비교**(r3 ↔ r7). 개정 이력은 **연속한 두 버전**만 설명한다. 프런트 화면도 지금은 최신 한 쌍만 그린다. `REQ-DIC-007`은 그래서 부분 충족이다
- **문서 본문 텍스트 diff**(`D-61`). 취소선이 들어간 `v2 → v3` 인라인 비교는 문장 분할·오프셋 규격(`REQ-DOC-005`, 대기)이 선행돼야 한다
- **`공식 전환` 등급**. 백엔드 `Dictionary`에 초안/공식 구분이 없다(`D-59`). 결정 대기다
- **프런트엔드 연동.** `fetchDictionaryRevisionTimeline`·`fetchDocumentVersions`는 여전히 목데이터다. 응답 형태만 그 화면에 맞춰 둔다
- **발행 이벤트.** 이 도메인은 순수 소비자다. 「사전집 r7이 발행되었습니다」 알림은 Notification이 `DictionaryRevisedEvent`를 추가 구독하는 `NT-*` 몫이다
- **LocalStack.** Notification과 같은 판단 — 브로커를 띄운 왕복 검증은 하지 않는다

---

## 2. 결정 대기표

### 2-1. 확정된 결정

`CONFLICTS.md` 3-3절의 `D-55`~`D-61`을 그대로 따른다. ID로만 참조한다.

| ID | 한 줄 요약 |
| --- | --- |
| `D-55` | RevisionLog을 8번째 도메인으로 신설. 접두사 `RL-`, Flyway 대역 800~899 |
| `D-56` | 발행 시점에 자동 생성·저장. 조회 시점 재계산을 하지 않는다 |
| `D-57` | 사전집·문서 두 축을 한 엔티티가 `targetType`으로 관장한다 |
| `D-58` | 멱등은 `(workspaceId, targetType, targetId, versionNo)` 유니크 |
| `D-59` | 등급 4종. 프런트의 `공식 전환`은 범위 밖 |
| `D-60` | diff 키는 `Term.preferredForm`. 대표어 변경은 삭제+추가로 나타난다 |
| `D-61` | 문서 축은 본문 텍스트 diff를 하지 않는다. v1은 백필한다 |

#### 이름 규약 — 이 도메인의 첫 번째 함정

`Revision` 접두사는 ReviewRequest가 먼저 쓰고 있다. **셋을 섞으면 안 된다.**

| 이름 | 뜻 | 시점 |
| --- | --- | --- |
| `RevisionDocument` · `RevisionDictionary` | 개정**안** — 리뷰에 올라간 변경 **제안** | 반영 **전** |
| `Revise` | 반영 — 승인된 개정안을 확정하는 **행위** | 반영 **순간** |
| **`RevisionLog`** | **개정 이력 — 반영이 끝난 뒤 남는 결과 기록** | 반영 **후** |

`RevisedTerm`이 한글명 충돌만으로 폐기된 선례가 있다. `UBIQUITOUS_LANGUAGE.md` 9절이 이 표를 갖는다.

### 2-2. 남은 결정 대기

- **`공식 전환` 등급.** 되살리려면 `Dictionary`에 초안/공식 상태가 먼저 있어야 하고 그것은 `DIC-*` 태스크다. 지금은 `RevisionLogGrade`에 값을 만들지 않는다
- **`DocumentCreatedEvent` 신설.** v1 백필이 그 자리를 메우고 있으나 정공법은 업로드가 이벤트를 발행하는 것이다. `document` 패키지를 고쳐야 하므로 `DOC-*`로 넘긴다
- **삭제된 문서의 이력을 목록에서 감출지.** 지금은 감추지 않는다 — 개정 이력은 보존 대상이다. 화면이 삭제된 문서를 가리키게 되면 다시 본다
- **보존 기간.** 삭제 경로를 두지 않았다. 행이 쌓이면 정리 정책이 필요하다

---

## 3. 도메인 모델

`DOMAIN.md` «RevisionLog» 절의 속성 표가 기준이다. 아래는 구현 수준 표이며 **`nullable` 칸은 `X`가 NOT NULL, `O`가 nullable**이다(`DOMAIN.md`의 `필수` 칸과 반대).

### RevisionLog

| 필드 | 타입 | 컬럼 | nullable | 축 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | 공통 | IDENTITY |
| `workspaceId` | `Long` | `workspace_id` | X | 공통 | `updatable = false` |
| `targetType` | `RevisionLogTargetType` | `target_type` | X | 공통 | `varchar(20)`, STRING |
| `targetId` | `Long` | `target_id` | X | 공통 | 사전집은 `dictionaryId`, 문서는 `documentId` |
| `versionNo` | `int` | `version_no` | X | 공통 | |
| `previousVersionNo` | `Integer` | `previous_version_no` | **O** | 공통 | 첫 버전이면 null |
| `origin` | `RevisionOrigin` | `origin` | X | 공통 | `varchar(20)`, STRING |
| `summary` | `String` | `summary` | X | 공통 | `varchar(255)`. 자동 생성 |
| `addedCount` | `int` | `added_count` | X | 공통 | |
| `changedCount` | `int` | `changed_count` | X | 공통 | |
| `removedCount` | `int` | `removed_count` | X | 공통 | |
| `publishedBy` | `Long` | `published_by` | X | 공통 | 반영·편집을 수행한 회원 |
| `publishedAt` | `OffsetDateTime` | `published_at` | X | 공통 | 원본 버전의 확정일시와 같다 |
| `grade` | `RevisionLogGrade` | `grade` | **O** | **사전집만** | `varchar(20)`, STRING |
| `affectedDocumentCount` | `int` | `affected_document_count` | X | **사전집만** | 기본 0 |
| `baseDictionaryVersionNo` | `Integer` | `base_dictionary_version_no` | **O** | **문서만** | `DocumentVersion.dictionaryVersionNo` 승계 |

**축마다 비는 칸이 셋 있다.** 테이블을 둘로 가르지 않는 이유는 타임라인 조회가 두 축을 **같은 모양으로** 읽기 때문이다(`D-57`). 가르면 목록 쿼리·응답 DTO·프런트 훅이 전부 두 벌이 된다. 엔티티는 축별 정적 팩터리 둘(`forDictionary`·`forDocument`)만 노출해 **잘못된 조합을 만들 수 없게** 한다 — 문서 축에 `grade`를 넣는 경로가 아예 없다.

### RevisionLogEntry

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | IDENTITY |
| `revisionLogId` | `Long` | `revision_log_id` | X | `updatable = false`. **같은 도메인이라 FK를 건다** |
| `changeType` | `RevisionLogChangeType` | `change_type` | X | `varchar(20)`, STRING |
| `subject` | `String` | `subject` | X | `varchar(100)`. 사전집은 `preferredForm`, 문서는 `originTerm` |
| `subjectEnglishName` | `String` | `subject_english_name` | **O** | `varchar(100)`. 사전집 전용 |
| `replacement` | `String` | `replacement` | **O** | `varchar(100)`. 문서 전용 — 적용된 대체어 |
| `detail` | `String` | `detail` | **O** | `varchar(255)`. 「정의 수정」 등 |

`subject`의 상한 100은 우연이 아니라 `Term.preferredForm`·`SuggestionTerm.originTerm`과 같은 값이다. **한쪽이 늘면 여기도 늘어야 한다.**

### 애그리게이트 경계

`RevisionLog`은 `RevisionLogEntry` 컬렉션을 **매달지 않는다.** 항목이 수백 개가 될 수 있고 타임라인 조회는 항목을 전혀 쓰지 않는다 — `RevisionLogEntryRepository`가 상세 조회에서만 따로 읽는다. `Dictionary`가 `Term`을 매달지 않는 것과 같은 판단이다.

**다른 도메인과는 연관 매핑 없이 FK id 필드로만 잇고 DB FK도 걸지 않는다.** 문서·워크스페이스가 소프트 삭제돼도 개정 이력은 남아야 하고, FK를 걸면 대역을 넘나드는 마이그레이션 순서 의존이 생긴다.

### enum

```java
public enum RevisionLogTargetType { DICTIONARY, DOCUMENT }

public enum RevisionOrigin { UPLOAD, DIRECT_EDIT, REVIEW_REVISE }

public enum RevisionLogGrade { INITIAL, NO_IMPACT, NEW_TERMS, RECHECK_REQUIRED }

public enum RevisionLogChangeType { ADDED, CHANGED, REMOVED }
```

`RevisionLogGrade`는 프런트 `DictionaryRevisionGrade`(`'새 지적' | '영향 없음' | '재검사' | '공식 전환' | '—'`) 5종 중 **4종에 대응한다.** `공식 전환`은 만들지 않는다(`D-59`). **tone은 저장하지 않는다** — 프런트가 `grade`에서 파생한다(Notification `D-49`와 같은 판단).

`RevisionOrigin`은 `DOMAIN.md` «문서» 정책의 「본문을 바꾸는 세 경로」와 1:1이다. 사전집 축은 **언제나 `REVIEW_REVISE`**다 — 「새 버전은 리뷰 승인(Revise)의 반영으로만 생긴다」가 사전집 정책이다.

---

## 4. 상태 전이와 도메인 메서드

**개정 이력은 상태 기계가 아니다.** 만들어진 뒤 바뀌지 않는다 — 그것이 이력의 정의다(`D-56`).

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (없음) | 기록됨 | `forDictionary(...)` / `forDocument(...)` | 해당 버전의 행이 아직 없을 때만 |
| 기록됨 | 기록됨 | — | **변경 메서드가 없다.** 수정·삭제 경로를 두지 않는다 |

전 필드에 `updatable = false`를 건다. `Term`이 같은 이유로 그렇게 돼 있다.

### 도메인 메서드 시그니처

```java
// RevisionLog
static RevisionLog forDictionary(Long workspaceId, Long dictionaryId, int versionNo, Integer previousVersionNo,
                                 RevisionLogGrade grade, int addedCount, int changedCount, int removedCount,
                                 int affectedDocumentCount, String summary, Long publishedBy,
                                 OffsetDateTime publishedAt);

static RevisionLog forDocument(Long workspaceId, Long documentId, int versionNo, Integer previousVersionNo,
                               RevisionOrigin origin, Integer baseDictionaryVersionNo, int changedCount,
                               String summary, Long publishedBy, OffsetDateTime publishedAt);

boolean isDictionary();
boolean isFirstVersion();      // previousVersionNo == null

// RevisionLogEntry
static RevisionLogEntry term(Long revisionLogId, RevisionLogChangeType changeType,
                             String subject, String subjectEnglishName, String detail);
static RevisionLogEntry replacement(Long revisionLogId, String originTerm, String suggestionTerm);
```

**축별 팩터리 둘로 가른 이유** — 한 생성자에 16개 인자를 넣으면 문서 축이 `grade`에 `null`을, 사전집 축이 `baseDictionaryVersionNo`에 `null`을 넣는 호출이 생기고, 그 `null`이 맞는 자리인지 코드만 봐서는 알 수 없다. **팩터리가 그 조합을 강제한다.**

`forDictionary`는 `origin`을 받지 않는다 — 언제나 `REVIEW_REVISE`라 인자로 두면 틀린 값을 넣을 수 있다.

---

## 5. 스키마와 Flyway

대역 800~899(`D-55`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 |
| --- | --- |
| `V800__create_revision_log.sql` | `revision_log` + `revision_log_entry` |

핵심 제약 둘.

- `uq_revision_log_target_version (workspace_id, target_type, target_id, version_no)` — **멱등의 근거**(`D-58`). at-least-once 재수신이 두 번째 insert에서 유니크 위반으로 튕긴다. Notification이 `dedupeKey` 문자열을 쓴 것(`D-51`)과 다른 이유는 **여기엔 자연 키가 이미 있기 때문**이다 — 한 사건이 대상 버전 하나당 정확히 한 행을 만든다
- `fk_revision_log_entry_log` — 항목에서 본체로 거는 **도메인 안쪽** FK. 다른 도메인 테이블에는 걸지 않는다

조회 인덱스는 `idx_revision_log_timeline (workspace_id, target_type, target_id, published_at)` 하나다. 축 필터와 최신순 페이징이 모두 이 앞쪽 컬럼을 탄다. 항목은 `idx_revision_log_entry_log (revision_log_id)`로 읽는다.

---

## 6. 패키지와 파일

```
revisionlog
├── domain
│   ├── RevisionLog · RevisionLogEntry
│   └── RevisionLogTargetType · RevisionOrigin · RevisionLogGrade · RevisionLogChangeType
├── exception
│   └── RevisionLogErrorCode
├── implement
│   ├── RevisionLogReader · RevisionLogAppender     ← 멱등이 사는 곳
│   ├── DictionaryDiffCalculator                    ← 사전집 diff 규칙이 사는 한 곳
│   ├── DictionaryGradeDecider                      ← 등급 판정이 사는 한 곳
│   ├── DocumentRevisionAssembler                   ← 문서 축 3경로 + v1 백필
│   ├── RevisionSummaryFactory
│   └── DocumentImpactCounter
├── infra
│   ├── RevisionLogRepository · RevisionLogEntryRepository
│   ├── port
│   │   ├── DictionaryTermQueryPort · TermSnapshot
│   │   ├── DocumentQueryPort · DocumentVersionSnapshot
│   │   ├── ReviewRequestQueryPort · DocumentRevisionSnapshot
│   │   └── DraftDocumentQueryPort · AppliedSuggestion
│   ├── adapter
│   │   └── …QueryAdapter 4개                       ← 스텁 없음. 빈 이름 명시
│   └── event
│       ├── InMemoryRevisionLogEventListener        ← @TransactionalEventListener + @Async
│       ├── SqsRevisionLogEventListener             ← @SqsListener
│       └── SubscribedEvents
├── service
│   ├── RevisionLogService · RevisionLogEventHandler ← 기술 무관 공용 핸들러
│   └── model/*Result · *Query
└── presentation
    ├── RevisionLogController
    └── dto/*Response
```

`common`에 더하는 것은 **없다.** SQS 발행 어댑터·봉투는 `NT-4`가 이미 만들어 뒀고 그대로 쓴다.

---

## 7. API 명세

`docs/API.md`의 `# **RevisionLog API**` 절이 정본이다. 요약만 옮긴다.

| Method | Path | 권한 |
| --- | --- | --- |
| GET | `/api/workspaces/{workspaceId}/revision-logs` | 참여자 |
| GET | `/api/workspaces/{workspaceId}/revision-logs/{revisionLogId}` | 참여자 |

**`POST`·`PATCH`·`DELETE`가 없다.** 개정 이력은 이벤트로만 만들어지고 만들어진 뒤 바뀌지 않는다.

목록은 `targetType`이 **필수**다. 두 축이 한 테이블에 있으므로 축을 고르지 않으면 사전집 버전과 문서 버전이 한 타임라인에 섞여 내려간다 — 어느 화면도 그것을 원하지 않는다. `targetId`는 선택이고, 문서 축에서는 사실상 필수다(문서 하나의 이력을 본다).

### 권한

참여 여부는 `WorkspaceAccessValidator`를 직접 주입해 검사한다(`D-19` 예외). 비참여 워크스페이스는 `404`다. **서열은 보지 않는다** — 개정 이력은 참여자 모두가 보는 자료다.

### 요청자 식별

**`@AuthenticationPrincipal Long memberId`**를 쓴다. `T-INT-3`이 끝나 `memberId` 요청 파라미터 방식은 폐기됐다. `NotificationController`에 남은 옛 방식은 병렬 머지의 잔재이므로 따라 하지 않는다.

---

## 8. ErrorCode

| 코드 | 상태 | 메시지 |
| --- | --- | --- |
| `REVISION_LOG_NOT_FOUND` | 404 | 개정 이력을 찾을 수 없습니다. |

**다른 워크스페이스의 개정 이력에도 같은 코드를 쓴다.** 코드가 갈리면 그 자체로 존재가 드러난다 — Notification이 「내 알림이 아님」에 별도 코드를 두지 않은 것과 같은 이유다.

`targetType` 누락·`sort` 화이트리스트 밖은 `CommonErrorCode.COMMON_INVALID_REQUEST`(400)를 쓴다. 권한 부족 코드는 만들지 않는다 — 이 도메인에 ADMIN 전용 경로가 없다.

---

## 9. 크로스 도메인 계약

### 소비 포트

어댑터는 **소비 도메인**인 `revisionlog/infra/adapter/`에 둔다(`D-33`). 제공 도메인의 `infra`(Repository)만 참조한다.

| 포트 | 메서드 | 프로퍼티 |
| --- | --- | --- |
| `DictionaryTermQueryPort` | `findDictionaryIdByVersion(Long, int)` · `readTerms(Long)` · `readVersion(Long, int)` | `app.crossdomain.dictionary.mode` (기존 재사용) |
| `DocumentQueryPort` | `readVersions(Long)` · `countAlignedBelow(Long, int)` | `app.crossdomain.document.mode` (기존 재사용) |
| `ReviewRequestQueryPort` | `findDocumentRevision(Long)` | `app.crossdomain.review-request.mode` (기존 재사용) |
| `DraftDocumentQueryPort` | `readAppliedSuggestions(Long)` | `app.crossdomain.draft-document.mode` (기존 재사용) |

**네 키가 모두 이미 선언돼 있고 전부 `real`이라 `application.yml`을 고치지 않는다.**

스냅샷 record — **포트 시그니처에 엔티티를 넣지 않는다.**

```java
record TermSnapshot(String preferredForm, String englishName, String definition) {}
record DictionaryVersionSnapshot(Long dictionaryId, Long publishedBy, OffsetDateTime publishedAt) {}
record DocumentVersionSnapshot(int versionNo, Integer dictionaryVersionNo, boolean edited,
                               OffsetDateTime publishedAt, Long publishedBy) {}
record DocumentRevisionSnapshot(Long workspaceId, Long documentId, Long draftDocumentId, int resultVersionNo,
                                Long performedBy) {}
record AppliedSuggestion(String originTerm, String suggestionTerm) {}
```

**스텁을 만들지 않는다.** `T-INT-2`가 「제공 도메인이 아직 없으면 스텁을 함께 만든다」의 전제가 소멸했다고 보고 스텁을 전부 지웠다. 어댑터는 `@ConditionalOnProperty(havingValue = "real")` 하나만 단다.

**빈 이름을 명시한다** — `@Component("documentQueryAdapterForRevisionLog")` 형태. 소비 도메인마다 같은 클래스명을 각자 정의하므로 Spring 기본 빈 이름이 전역에서 충돌한다. `draftdocument`의 `DocumentQueryAdapter`가 같은 이유로 이름을 갖고 있다.

### 제공 포트

없다. 다른 도메인이 개정 이력을 조회할 일이 없다.

### 수신 이벤트

| 이벤트 | 만드는 개정 이력 |
| --- | --- |
| `DictionaryRevisedEvent` | 사전집 축 1건 (+ 항목 N건) |
| `DocumentEditedEvent` | 문서 축 `DIRECT_EDIT` 1건 (+ v1 백필) |
| `ReviewRequestRevisedEvent` (`type == DOCUMENT`) | 문서 축 `REVIEW_REVISE` 1건 (+ 항목 N건, + v1 백필) |

**`type == DICTIONARY`인 `ReviewRequestRevisedEvent`는 무시한다.** 그 반영은 `DictionaryService.revise`가 `DictionaryRevisedEvent`를 따로 발행하므로, 둘 다 처리하면 같은 사전집 버전에 행을 두 번 만들려 든다(유니크가 막지만 그것에 기대지 않는다).

**세 이벤트 모두 필요한 값을 다 갖고 있지 않다.** `DictionaryRevisedEvent`에는 발행자가 없고, `ReviewRequestRevisedEvent`에는 문서 식별자가 없다. 포트로 되짚어 조회한다 — `ARCHITECTURE.md`의 「상태가 필요한 컨슈머는 식별자로 다시 조회한다」가 이것을 허용한다.

### 발행 이벤트

없다. 이 도메인은 순수 소비자다.

---

## 10. 공유 파일 규약

| 파일 | 규약 |
| --- | --- |
| `docs/API.md` | 파일 **끝**에 `# **RevisionLog API**` 절만 추가. 공통 규칙·페이징·버저닝·에러 형식 절은 건드리지 않는다 |
| `docs/REQUIREMENTS.md` · `DOMAIN.md` · `UBIQUITOUS_LANGUAGE.md` | `T-DOC-1` 소유. 이미 머지됐으므로 **그 변경만 담은 커밋을 따로 만든다**(`RL-1`의 첫 커밋) |
| `docs/plan/EXECUTION_ORDER.md` | 0절 접두사 표에 `RL-`, 3절 Flyway 대역 표에 800~899 |
| `backend/CLAUDE.md` | Flyway 대역 표에 `800 - 899` |
| `application.yml` (main·test) | **고치지 않는다.** 쓰는 키 넷이 이미 선언돼 있고 전부 `real`이다 |
| `build.gradle` | **고치지 않는다.** 새 의존성이 없다 |

### 수정 금지 파일

`common/**`, `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, **다른 도메인의 패키지 전체**. 이 도메인은 예외를 하나도 쓰지 않는다 — `dictionary`·`document`·`reviewrequest`·`draftdocument`를 **읽기만** 한다.

---

## 11. 테스트 계획

`docs/TEST.md`를 따른다 — RestAssuredMockMvc 필수, AssertJ만, `@DisplayName`은 「…한다.」로 끝나는 한글.

| 클래스 | 베이스 | 필수 케이스 |
| --- | --- | --- |
| `RevisionLogTest` | 순수 단위 | 축별 팩터리가 상대 축의 칸을 비운다, `isFirstVersion` |
| `RevisionLogEntryTest` | 순수 단위 | 두 팩터리의 칸 배치 |
| `DictionaryDiffCalculatorTest` | 순수 단위 | 추가·변경·삭제 3종, **대표어 변경이 삭제+추가로 나온다**, 정의만 달라도 `CHANGED` |
| `DictionaryGradeDeciderTest` | 순수 단위 | 등급 4종, **삭제가 하나라도 있으면 `RECHECK_REQUIRED`** |
| `DocumentRevisionAssemblerTest` | 순수 단위(포트 모킹) | 3경로 매핑, **`KEEP_ORIGINAL`은 항목이 되지 않는다**, v1 백필 |
| `RevisionSummaryFactoryTest` | 순수 단위 | 축·등급·경로별 한 줄 |
| `RevisionLogRepositoryTest` | `RepositoryTestSupport` | **같은 대상·버전 두 번 insert가 거부된다**, 축 필터 + 최신순 페이징 |
| `DictionaryTermQueryAdapterTest` 외 3 | `RepositoryTestSupport` | 스냅샷 매핑, 소프트 삭제 문서 제외 |
| `RevisionLogEventHandlerTest` | `IntegrationTestSupport` | **같은 이벤트를 두 번 처리해도 1건이다**, `type == DICTIONARY`인 반영 이벤트가 문서 축을 만들지 않는다 |
| `RevisionLogServiceTest` | `IntegrationTestSupport` | 사전집 2회 반영 → 2건, 문서 직접 편집 → v1 백필 + v2, 비참여자 404 |
| `RevisionLogControllerTest` | `@WebMvcTest` + `@WithLoginMember` | 목록·상세, `targetType` 누락 400, 없는 id 404, `sort` 화이트리스트 밖 400 |
| `RevisionLogEventWiringTest` | `IntegrationTestSupport` | 두 수신 어댑터가 `app.messaging.mode`로 **배타 선택**된다 |

**`IntegrationTestSupport`를 상속한 클래스에 `@Transactional`을 붙이지 않는다.** 붙이면 테스트 트랜잭션이 롤백으로 끝나 `@TransactionalEventListener(AFTER_COMMIT)`이 영원히 실행되지 않는다 — 이 도메인은 그 리스너가 곧 본체라 테스트가 통째로 무의미해진다.

### Fixture

`revisionlog/fixture/RevisionLogFixture` · `RevisionLogEntryFixture`. 체이닝 빌더에 기본값을 두고 `ReflectionTestUtils`로 `id`를 주입한다. Lombok `@Builder`를 쓰지 않는다.

---

## 12. Phase와 태스크

| 태스크 | 티켓 | 내용 | 의존 |
| --- | --- | --- | --- |
| `RL-1` | WLSH-159 | 문서 + 도메인 엔티티 2개 + enum 4개 + Flyway | — |
| `RL-2` | WLSH-160 | 사전집 축 — 포트·어댑터 2개 + diff·등급·요약·영향수 + 이벤트 핸들러 + 인메모리 리스너 | `RL-1` |
| `RL-3` | WLSH-161 | 문서 축 — 포트·어댑터 2개 + 3경로 조립 + v1 백필 | `RL-2` |
| `RL-4` | WLSH-162 | 조회 API 2개 | `RL-3` |
| `RL-5` | WLSH-163 | SQS 수신 어댑터 | `RL-3` |

브랜치는 `feat/WLSH-{티켓}-revision-log-phase-{번호}`. **티켓이 태스크당 하나이므로 브랜치도 태스크당 하나다** — `.githooks/prepare-commit-msg`가 브랜치명의 키를 메시지에 넣으므로 한 브랜치에 다른 티켓 번호를 적으면 키가 겹친다. **Phase 0 문서 커밋은 `RL-1` 브랜치의 첫 커밋으로 얹는다.**

### Phase별 DoD

- **`RL-1`** — 문서 9종이 갱신됐고, 엔티티 2개와 enum 4개가 있고, Flyway가 `RepositoryTestSupport`에서 실제로 적용되며, **유니크 제약이 중복 insert를 튕긴다**
- **`RL-2`** — `DictionaryRevisedEvent` 하나로 개정 이력 1건과 항목 N건이 생기고, **같은 이벤트를 두 번 줘도 1건이며**, 첫 버전은 `INITIAL`·삭제가 있으면 `RECHECK_REQUIRED`가 되고, 재검사 등급일 때만 영향 문서 수가 0보다 크다
- **`RL-3`** — 직접 편집은 항목 없는 `DIRECT_EDIT` 1건, 리뷰 반영은 **적용된 치환만** 담은 `REVIEW_REVISE` 1건을 만들고, **v1 `UPLOAD` 행이 백필되며**, `type == DICTIONARY`인 반영 이벤트는 문서 축을 만들지 않는다
- **`RL-4`** — 두 엔드포인트가 동작하고, `targetType`으로 축이 갈리며, 비참여 워크스페이스가 `404`다
- **`RL-5`** — `app.messaging.mode`로 두 수신 어댑터가 배타 선택되고, 둘이 **같은 핸들러**에 위임하며, 구독하지 않는 이벤트를 예외 없이 넘긴다

`RL-4`와 `RL-5`는 서로 의존하지 않아 병렬 가능하다.

---

## 13. 리스크와 열린 질문

### 리스크

- **영향 문서 수가 발행 시점 스냅샷이다.** 그 뒤에 올라온 문서는 세지 않는다. 「지금 재검사가 필요한 문서 수」를 원한다면 조회 시점에 세야 하는데, 그러면 과거 노트의 숫자가 계속 바뀌어 이력이 아니게 된다. **저장을 택한 대가이며 의도된 트레이드오프다**
- **영향 문서 수 계산이 N+1이다.** `document` 패키지에 쿼리 메서드를 더할 수 없어(3절 금지) 어댑터가 문서마다 현재 버전을 읽는다. 발행 때 한 번 도는 비동기 경로라 감수하지만, 문서가 수천 건이 되면 전용 쿼리를 `DOC-*`로 넘겨야 한다
- **이벤트 시그니처가 바뀌면 핸들러가 함께 바뀐다.** Notification이 `RR-4d` 머지 때 겪은 일과 같다. 컴파일이 잡아 주지만 **`targetDraftId`의 의미가 바뀌면 컴파일은 통과하고 값만 틀린다** — 그래서 문서 식별자를 그 필드에서 꺼내지 않고 `ReviewRequestQueryPort`로 되짚는다
- **`@Async` 리스너의 실패는 호출자에게 전파되지 않는다.** `AsyncEventConfig`의 핸들러가 ERROR 로그로만 남긴다. 개정 이력이 조용히 누락돼도 본 작업은 성공으로 보인다 — **의도된 트레이드오프다**(이력 실패가 사전집 반영을 되돌리면 안 된다). 다만 알림과 달리 **누락이 영구적으로 티가 나지 않는다** — 타임라인에 구멍이 나 있어도 아무도 모른다. 보정 배치는 열린 질문이다
- **SQS 경로가 실제 메시지로 검증되지 않는다.** LocalStack이 없다. **이벤트 record에 엔티티가 섞이지 않았는지를 리뷰에서 본다**

### 열린 질문

- 누락된 개정 이력의 보정 배치를 둘 것인가
- 개정 이력 보존 기간과 정리 정책
- 삭제된 문서의 이력을 목록에서 감출 것인가
- 임의의 두 버전 비교(`REQ-DIC-007`의 나머지 절반)를 어디에 둘 것인가 — 이 도메인인가 `dictionary`인가

### DOMAIN.md 수정

`RL-1`에서 반영한다 — `RevisionLog`·`RevisionLogEntry` 표 2개 신설, 「정책 · 제약」에 «개정 이력» 항목 추가, 관계 표 4행 추가.
