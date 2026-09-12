# Document 도메인 구현 계획

문서(Document) 도메인의 구현 계획이다. `docs/DOMAIN.md`·`docs/ARCHITECTURE.md`·`docs/TEST.md`·`docs/API.md`·`docs/LOG.md`·`docs/EXCEPTION.md`를 먼저 읽고, 이 문서는 그 규칙을 이 도메인에 적용한 결과로 읽는다.

여섯 도메인 계획 문서는 같은 목차를 쓴다 — `WORKSPACE_PLAN.md`, `DICTIONARY_PLAN.md`, `DRAFT_DOCUMENT_PLAN.md`, `DRAFT_DICTIONARY_PLAN.md`, `REVIEW_REQUEST_PLAN.md`.

**이 도메인은 이미 상당 부분 구현돼 있다.** 3·5·6·7절의 표에 `상태` 칸을 두어 `as-built`(현재 코드) / `추가` / `변경` / `제거` / `대기`를 구분한다.

**`docs/plan/CONFLICTS.md`를 함께 읽는다.** 큰 흐름 확정(`G-*`)·이번 세션 확정(`D-19`~`D-32`)·뒤집힌 결정(`R-*`)이 거기에 있고, 이 문서는 ID로 참조한다. **자기 서술과 `R-*`가 어긋나면 `R-*`를 따른다.**

**여러 세션으로 나눠 구현한다면 `EXECUTION_ORDER.md`를 먼저 읽는다.** 도메인을 가로지르는 태스크 순서, 공유 파일의 주인, 커밋 단위, 동시 실행 제약이 거기에 있다.

---

## 1. 범위와 목표

**담당 애그리게이트**: `Document`(루트, `DocumentVersion` 포함), `Label`, `DocumentLabel`

**목표**: 워크스페이스의 문서와 그 확정 버전을 관리하고, **사전집 기준으로 정렬됐는지(`aligned`)를 판정**해 용어 추출과 갱신의 대상 여부를 알려 준다.

**관련 요구사항**: `REQ-DOC-001`(업로드 — 완료), `REQ-DOC-002`(목록 — 완료), `REQ-DOC-003`(원문 보기 — 완료), `REQ-DOC-004`(삭제 — 완료), `REQ-DOC-006`(처리 상태 표시 — 진행중), `REQ-DOC-008`(수정 — 완료), `REQ-DOC-009`(라벨 — 완료), `REQ-DOC-010`(버전 이력 — 완료), `NFR-DOC-001`(업로드 제약 — `R-17`로 분담 변경), `NFR-WS-001`(데이터 격리)

**큰 흐름이 이 도메인에 준 변경 셋**

1. **직접 편집이 리뷰 경로를 벗어난다**(`G-9`) — 대조·초안·개정안 없이 즉시 새 리비전을 발행한다. **본문을 바꾸는 유스케이스가 이 도메인에 처음 생긴다.**
2. **`edited` 신규**(`G-10`·`G-11`) — 사람이 직접 고친 버전인지를 버전에 기록하고, `dictionaryVersionNo`는 이전 값을 승계한다.
3. **`outdated`를 `aligned`로 대체**(`R-18`·`D-31`) — 판정 조건이 `G-12`의 추출 허용 조건과 같아져 하나로 합친다.

### MVP1 범위 밖

| 대상 | 사유 | 대체 |
| --- | --- | --- |
| multipart 파일 업로드 | **`G-5` 확정** — 브라우저가 `txt`·`md`를 읽어 요청 본문 텍스트로 보내므로 서버가 파일을 받지 않는다 | 현재 `CreateDocumentRequest(title, content, labels)` JSON 방식을 그대로 쓴다 |
| S3·NoSQL 원문 저장소 | `NFR-DOC-002` **폐기 확정**(`R-16`) | `document_version.body`(`text`) |
| 확장자·크기·인코딩 검증 | 서버가 파일을 받지 않아 검증할 수단이 없다(`R-17`) | **클라이언트 책임.** 서버는 길이 검증(제목 200자·본문 10,000자)만 |
| `originRevisionId` | `D-26` — `RevisionDocument.resultVersionNo`가 같은 관계의 반대 방향이라 정보가 중복이다 | 컬럼을 만들지 않는다 |
| `REQ-DOC-005` 문단·문장 분할과 오프셋 | `SuggestionTerm.anchor` 규격과 함께 정해야 한다 | `draftdocument` 도메인 |
| `REQ-DOC-007` 문서 재업로드 | MVP2 | — |
| 역인덱스(`REQ-IDX-*`) | `D-29` — MVP2로 내렸다 | — |

**대조·초안 생성은 이 도메인이 하지 않는다.** 「갱신」은 `draftdocument`가 `POST /documents/{documentId}/drafts`로 연다. 이 도메인은 **그 대상 여부(`aligned`)를 판정해 알려주는 것**까지다.

---

## 2. 결정 대기표

### 2-1. 확정된 결정

| ID | 확정 내용 | 문서 수정 |
| --- | --- | --- |
| **G-5** | 업로드는 **파일을 서버로 보내지 않는다.** 브라우저가 `txt`·`md`를 읽어 요청 본문 텍스트로 담고 서버는 RDBMS에 직접 저장한다. **현재 구현이 그대로 정답이므로 업로드 경로는 손대지 않는다** | `R-16`·`R-17` |
| **G-6** | 업로드 시 `dictionaryVersionNo`는 `null`이다. **as-built와 일치**(`DocumentVersion.publishFirst`가 `null`을 넣는다) | — |
| **G-7** | 교정 반영본의 `dictionaryVersionNo`는 **발행 시점 최신 사전집 버전**을 따른다. `DocumentVersionPublishPort`가 이 값을 인자로 받아야 한다 | `R-13` |
| **G-9** | **직접 편집은 즉시 새 리비전으로 발행된다.** 대조·초안·개정안을 거치지 않는다 | `R-1`~`R-7` |
| **G-10** | **`DocumentVersion.edited`**(boolean, 기본 `false`). 직접 편집본은 `true`, 교정 반영본은 `false` | `DOMAIN.md` `DocumentVersion` 표에 1행 추가 |
| **G-11** | 편집본의 `dictionaryVersionNo`는 **이전 버전 값을 승계**한다 | 정책 절에 명시 |
| **G-12** | **추출 허용 조건** = `활성 사전집 versionNo == 최신 확정 버전의 dictionaryVersionNo` AND `edited == false`. 사전집이 없으면 양쪽이 `null`이라 참이 된다 | `DOMAIN.md` 문서 정책 |
| **D-31** | **`outdated`를 제거하고 파생 필드 `aligned`로 통합한다.** 조건이 `G-12`와 같아 두 축이 갈리지 않는다. `UBIQUITOUS_LANGUAGE.md`에 「정렬됨/aligned」를 등재한다 | `R-18` |
| **D-30** | **문서 초안이 진행 중인 문서도 직접 편집을 막는다.** 편집하면 초안의 `baseVersionNo`가 낡고, 그 초안이 발행되면 사용자가 고친 내용을 덮어써 날린다 | 정책 절에 추가 |
| **G-15** | 사전 초안의 **원천 문서도 직접 편집을 막는다.** `D-30`과 한 자리에서 검증한다 | 정책 절에 추가 |
| **D-19** | **접근 검증은 포트를 만들지 않고 `WorkspaceAccessValidator`를 직접 주입한다.** as-built가 이미 그 방식이므로 **`DocumentService`를 고치지 않는다** | `ARCHITECTURE.md` 예외 조항 |
| **D-25** | **ID 값 객체를 제거하고 `Long`으로 통일한다** — `DocumentId`·`DocumentVersionId`·`DocumentLabelId`·`LabelId` 4개와 `DocumentIdTest` | `DOMAIN.md` 속성 표 각주 |
| **D-26** | `originRevisionId`를 두지 않는다 | `DOMAIN.md` `DocumentVersion` 표 |
| **판정 규칙 한 곳** | `aligned` 판정을 **`DocumentVersion`의 static 메서드**에 두고 `DocumentVersionSummary`가 그것을 재호출한다. `isOutdated`가 「목록 조회는 본문을 빼고 읽으므로 엔티티가 아니라 조회 결과 모델이 같은 판정을 해야 한다. 규칙이 두 벌이 되지 않도록」이라며 택한 구조를 **그대로 이어받는다** | — |
| **본문 저장 위치** | 본문은 `DocumentVersion.body`에만 있다. `Document`에 본문 컬럼을 두지 않는다 — as-built이며 큰 흐름도 바꾸지 않았다 | — |
| **소프트 삭제** | 모든 엔티티가 `BaseEntity`를 상속한다. 실제 삭제 유스케이스는 `Document`에만 제공하고 나머지 엔티티의 `deletedAt`은 null로 유지한다 | — |
| **D-18** | 도메인 레벨 패키지 구조만 지키고 하위 디렉터리는 `presentation/dto/`·`service/model/`, ErrorCode는 `{domain}/exception/` — as-built가 이미 준수 | — |

**`G-9`가 이 도메인의 가장 중요한 결정이다.** 지금까지 `Document`는 "본문을 바꾸는 메서드가 없다"를 설계 원칙으로 삼았고 javadoc·`API.md`·`REQ-DOC-008`이 그것을 네 곳에서 못박고 있었다(`R-1`·`R-6`·`R-7`). 직접 편집이 들어오면 **`Document`가 버전을 전진시키는 주체가 된다** — 미사용 상태인 `PublishedVersion.next()`의 첫 호출자가 여기다.

### 2-2. 남은 결정 대기

**없다. 2026-09-10에 전건 확정했다.** 모든 Phase에 착수할 수 있다.

`docs/DOMAIN.md` «모델 반영 필요(미확정)» 블록 중 이 도메인에 걸리는 것은 없다. 라벨·outdated 항목은 2026-09-10에 해소됐고, outdated는 `D-31`로 한 번 더 바뀐다.

---

## 3. 도메인 모델

### Document (애그리게이트 루트)

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | `@GeneratedValue(IDENTITY)` |
| `workspaceId` | `Long` | `workspace_id` | X | as-built | `updatable = false`. DB FK 있음(같은 스키마) |
| `title` | `String` | `title` | X | as-built | `varchar(200)`. 워크스페이스 안에서 중복 허용 |
| `currentVersionNo` | `int` | `current_version_no` | X | as-built | **이 번호가 가리키는 `DocumentVersion`이 현재 본문이다.** 업로드 시 1 |
| `updaterId` | `Long` | `updater_id` | X | as-built | 최종수정자. 제목·라벨 수정, 편집 발행, 교정 반영 때 갱신 |
| `createdBy` | `Long` | `created_by` | X | as-built | `updatable = false`. 업로더 |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | as-built | `BaseEntity` 상속 |

본문 컬럼을 두지 않는다. `labels` 컬렉션도 매핑하지 않는다 — `DocumentLabel`이 `documentId`로 단방향 참조한다.

### DocumentVersion

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | |
| `documentId` | `Long` | `document_id` | X | as-built | **같은 도메인이라 DB FK를 건다** |
| `version` | `PublishedVersion` | `version_no`, `published_at` | X | as-built | `@Embedded` 값 객체 |
| `body` | `String` | `body` | X | as-built | `@Lob`, `text`. 10,000자. **본문의 유일한 저장 위치** |
| `dictionaryVersionNo` | `Integer` | `dictionary_version_no` | O | as-built | 이 버전이 통과한 사전집 버전. 업로드본은 `null`(`G-6`), 편집본은 승계(`G-11`), 교정 반영본은 최신(`G-7`) |
| **`edited`** | **`boolean`** | **`edited`** | **X** | **추가** | **기본 `false`, `updatable = false`.** 직접 편집 발행이면 `true`(`G-10`) |
| ~~`originRevisionId`~~ | — | — | — | **대기** | `D-26` — 두지 않는다 |
| `createdBy` | `Long` | `created_by` | X | as-built | |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | **변경** | `BaseEntity` 상속. 삭제 유스케이스는 제공하지 않는다 |

전 필드가 `updatable = false`다 — 확정 후 불변이다.

### Label

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | |
| `workspaceId` | `Long` | `workspace_id` | X | as-built | 라벨은 워크스페이스가 소유한다 |
| `name` | `String` | `name` | X | as-built | `varchar(20)`. **워크스페이스 안에서 유일.** 앞뒤 공백 제거 후 대소문자 구분 |
| `createdBy` | `Long` | `created_by` | X | as-built | |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | **변경** | `BaseEntity`. 삭제 유스케이스는 제공하지 않는다 |

### DocumentLabel

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | |
| `documentId` | `Long` | `document_id` | X | as-built | |
| `labelId` | `Long` | `label_id` | X | as-built | `(document_id, label_id)` 유일 |
| `createdBy` | `Long` | `created_by` | X | as-built | |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | **변경** | `BaseEntity`. 삭제 유스케이스는 제공하지 않는다 |

`MAX_LABELS_PER_DOCUMENT = 5`가 이 클래스의 `public static final` 상수다.

### 애그리게이트 경계

`Document`와 `DocumentVersion`은 **같은 애그리게이트**지만 JPA 컬렉션으로 묶지 않는다. `Label`은 워크스페이스가 소유하는 **별개 애그리게이트**이고 `DocumentLabel`이 둘을 잇는다.

- 연관관계 매핑(`@ManyToOne`/`@OneToMany`)을 쓰지 않는다. 애그리게이트 경계를 식별자로 넘어 지연 로딩 프록시가 상위 레이어로 새는 경로를 막는다 — DB에는 FK가 그대로 있다.
- 「현재 본문」 불변식(`currentVersionNo`가 가리키는 버전이 존재한다)은 **`DocumentVersionAppender`가 `Document`와 함께 받아** 지킨다.
- 문서를 소프트 삭제해도 버전·라벨 연결 행은 지우지 않는다 — 모든 조회가 문서에서 먼저 막힌다.

### 값 객체

`PublishedVersion`(`@Embeddable record`) — `versionNo`(1부터) + `publishedAt`. **둘이 함께 파생되기 때문에** 묶는다. `initial()`·`next()` 정적 팩토리를 갖고, `next()`는 `F-4`가 첫 호출자다.

### enum

없다. 이 도메인에는 상태 축이 없다 — 확정 버전만 쌓이고 `aligned`·`edited`는 파생 판정과 boolean이다.

---

## 4. 상태 전이와 도메인 메서드

이 도메인에는 enum 상태 축이 없다. 대신 **버전이 늘어나는 세 경로**가 상태 전이에 대응한다.

### 버전 발행 경로

| 경로 | 트리거 | `versionNo` | `dictionaryVersionNo` | `edited` | `aligned` 결과 | 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| **업로드** | `POST /documents` | 1 | `null`(`G-6`) | `false` | 사전집 없으면 참, 있으면 거짓 | as-built |
| **직접 편집** | `PATCH /documents/{id}/content` | `+1` | **이전 버전 값 승계**(`G-11`) | **`true`**(`G-10`) | **항상 거짓** — `edited`가 막는다 | **추가**(`G-9`) |
| **교정 반영** | `DocumentVersionPublishPort.publish(...)` | `+1` | **발행 시점 활성 사전집 버전**(`G-7`) | `false` | 참 | **추가** |

**세 경로가 모두 `Document.publishNext(...)`를 지난다.** 버전 번호를 올리는 곳이 하나여야 `currentVersionNo`와 실제 행이 어긋나지 않는다.

**`aligned` 판정** — `dictionaryVersionNo == activeVersionNo` AND `edited == false`. 활성 사전집이 없으면(`activeVersionNo == null`) 업로드본의 `dictionaryVersionNo`도 `null`이라 **같은 값으로 참**이 된다. 사전집이 없는 동안 모든 문서가 추출 대상이라는 `G-12`의 예외가 별도 분기 없이 성립한다.

### 편집 차단 조건

직접 편집은 아래 중 하나라도 참이면 거절한다(`409`).

| 조건 | 근거 | 포트 |
| --- | --- | --- |
| 이 문서에 진행 중인 문서 초안이 있다 | `D-30` | `DraftDocumentQueryPort.hasOngoingDraft(documentId)` |
| 이 문서가 진행 중인 사전 초안의 원천 문서다 | `G-15` | `DraftDictionaryQueryPort.isSourceOfOngoingDraft(documentId)` |

「진행 중」은 초안 `status != REVISED` AND 미삭제다(`D-10`). `REVIEW_REQUESTED` 상태가 개정안 진행을 함의하므로 **초안 조회 하나가 초안과 개정안을 동시에 덮는다**(`D-22`).

### 도메인 메서드 시그니처

```java
// Document
static Document create(Long workspaceId, String title, Long memberId);   // as-built
void rename(String title, Long memberId);                                // as-built
void touch(Long memberId);                                               // as-built
boolean belongsTo(Long workspaceId);                                     // as-built
int publishNext(Long memberId);        // 추가 — currentVersionNo를 올리고 updaterId를 갱신, 새 번호 반환

// DocumentVersion
static DocumentVersion publishFirst(Long documentId, String body, Long memberId);   // as-built
static DocumentVersion publishEdited(                                              // 추가
        Long documentId, PublishedVersion version, String body,
        Integer inheritedDictionaryVersionNo, Long memberId);                       // edited = true
static DocumentVersion publishRevised(                                             // 추가
        Long documentId, PublishedVersion version, String body,
        int activeDictionaryVersionNo, Long memberId);                              // edited = false
static boolean isAligned(Integer publishedDictionaryVersionNo, boolean edited,
        Integer activeDictionaryVersionNo);                                         // 추가 — 판정 규칙 단일 지점
boolean isAligned(Integer activeDictionaryVersionNo);                               // 추가 — 위임
int versionNo();                                                                    // as-built
OffsetDateTime publishedAt();                                                       // as-built
// 제거: isOutdated(Integer, Integer), isOutdated(Integer)                          // R-18

// Label
static Label create(Long workspaceId, String name, Long memberId);   // as-built
static String normalizeName(String name);                            // as-built

// DocumentLabel
static DocumentLabel of(Long documentId, Long labelId, Long memberId);   // as-built
```

`publishEdited`와 `publishRevised`를 나누는 이유는 **`edited`와 `dictionaryVersionNo`가 함께 결정되기 때문**이다. 인자 하나로 받으면 호출자가 두 값을 잘못 조합할 수 있다.

---

## 5. 스키마와 Flyway

**대역: 200–299**(`backend/CLAUDE.md`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 | 상태 | 태스크 |
| --- | --- | --- | --- |
| `V200__create_document_and_version.sql` | `document`, `document_version` | as-built | — |
| `V201__create_label.sql` | `label`, `document_label` | as-built | — |
| `V210__add_document_version_edited.sql` | `document_version.edited` 추가 | **추가** | `DOC-2` |
| `V220__add_document_version_deleted_at.sql` | `document_version.deleted_at` 추가 | **추가** | Phase 1 후속 |
| `V900__add_base_entity_deleted_at.sql` | `label.deleted_at`, `document_label.deleted_at` 추가 | **추가** | 공통 후속 |

```sql
-- V210
-- 사람이 직접 편집해 만든 버전인지. 대조·교정을 거친 반영본은 false다.
-- 이 값과 dictionary_version_no로 「사전집 기준에 정렬됨(aligned)」을 판정하며,
-- aligned가 아닌 문서는 용어 추출 대상에서 빠진다.
-- 기존 행은 모두 업로드본이거나 교정 반영본이므로 default false로 채운다.
alter table document_version
    add column edited tinyint(1) not null default 0;
```

**기존 두 파일은 고치지 않는다.** `Y-10`(workspace가 `V2__`로 대역 밖) 정리는 `T-INT-1`이 `spring.flyway.out-of-order=true`로 처리한다.

`edited`를 `Document`가 아니라 `document_version`에 두는 이유는 **버전 생성 시점에 값이 정해지기 때문**이고(4절), 덕분에 버전 이력에 "이 버전은 사람이 직접 고친 것"이 남는다.

**`aligned`는 컬럼을 만들지 않는다.** 활성 사전집 버전과 비교하는 파생 판정이라 저장하면 사전집이 새 버전을 발행할 때마다 모든 문서 행을 갱신해야 한다.

---

## 6. 패키지와 파일

```
com.ubidict.backend.document
├── presentation
│   ├── DocumentController                       as-built (+ 편집 엔드포인트 DOC-2)
│   ├── LabelController                          as-built
│   └── dto
│       ├── CreateDocumentRequest                as-built
│       ├── UpdateDocumentRequest                as-built
│       ├── EditDocumentContentRequest           DOC-2
│       ├── DocumentResponse                     as-built (+ aligned·edited, − outdated: DOC-3)
│       ├── DocumentSummaryResponse              as-built (+ aligned·edited, − outdated: DOC-3)
│       ├── DocumentVersionResponse              as-built (+ edited: DOC-3)
│       ├── DocumentVersionSummaryResponse       as-built (+ edited: DOC-3)
│       └── LabelResponse                        as-built
├── service
│   ├── DocumentService                          as-built (+ editContent DOC-2)
│   └── model
│       ├── CreateDocumentCommand                as-built
│       ├── UpdateDocumentCommand                as-built
│       ├── EditDocumentContentCommand           DOC-2
│       ├── DocumentResult                       as-built (+ aligned·edited: DOC-3)
│       ├── DocumentSummaryResult                as-built (+ aligned·edited: DOC-3)
│       ├── DocumentVersionResult                as-built (+ edited: DOC-3)
│       ├── DocumentVersionSummaryResult         as-built (+ edited: DOC-3)
│       └── LabelResult                          as-built
├── implement
│   ├── DocumentReader                           as-built
│   ├── DocumentAppender                         as-built
│   ├── DocumentUpdater                          as-built
│   ├── DocumentRemover                          as-built (+ 이벤트 발행 DOC-10)
│   ├── DocumentVersionReader                    as-built
│   ├── DocumentVersionAppender                  as-built (+ appendEdited·appendRevised DOC-2/DOC-6)
│   ├── DocumentEditGuard                        DOC-5   — 편집 차단 검증(D-30·G-15)
│   ├── DocumentAlignmentReader                  DOC-1   — 활성 사전집 버전을 읽어 aligned 판정
│   ├── DocumentLabelReader                      as-built
│   ├── DocumentLabelWriter                      as-built
│   ├── LabelReader                              as-built
│   └── LabelAppender                            as-built
├── infra
│   ├── DocumentRepository                       as-built (+ 페이징 DOC-7)
│   ├── DocumentVersionRepository                as-built
│   ├── DocumentVersionSummary                   as-built (isOutdated → isAligned: DOC-3)
│   ├── DocumentLabelRepository                  as-built
│   ├── LabelRepository                          as-built
│   ├── port
│   │   ├── DictionaryQueryPort                  DOC-1
│   │   ├── DraftDocumentQueryPort               DOC-5
│   │   └── DraftDictionaryQueryPort             DOC-5
│   └── adapter
│       ├── DictionaryQueryAdapter               DOC-1   — dictionary/infra만 참조
│       ├── DictionaryQueryStub                  DOC-1
│       ├── DraftDocumentQueryAdapter            DOC-5
│       ├── DraftDocumentQueryStub               DOC-5
│       ├── DraftDictionaryQueryAdapter          DOC-5
│       └── DraftDictionaryQueryStub             DOC-5
├── domain
│   ├── Document                                 as-built (+ publishNext DOC-2)
│   ├── DocumentVersion                          as-built (+ edited·isAligned, − isOutdated)
│   ├── PublishedVersion                         as-built
│   ├── Label                                    as-built
│   ├── DocumentLabel                            as-built
│   ├── ~~DocumentId~~                           제거 D-25
│   ├── ~~DocumentVersionId~~                    제거 D-25
│   ├── ~~DocumentLabelId~~                      제거 D-25
│   ├── ~~LabelId~~                              제거 D-25
│   └── event
│       ├── DocumentDeletedEvent                 DOC-10
│       └── DocumentEditedEvent                  DOC-10
└── exception
    ├── DocumentErrorCode                        as-built (+ 3건 DOC-2/DOC-5)
    └── LabelErrorCode                           as-built
```

**`DocumentService`는 접근 검증에 `WorkspaceAccessValidator`를 계속 직접 주입한다**(`D-19`). `infra/port/`에 워크스페이스 포트를 만들지 않는다.

---

## 7. API 명세

접두사 `/api`(버저닝 없음). 컨트롤러는 **`ResponseEntity<T>`를 반환**하고 `@ResponseStatus`를 쓰지 않는다. **Swagger 애노테이션은 쓰지 않는다**(명세는 `docs/API.md`가 담당).

요청자 식별은 **`@RequestParam Long memberId`**다. 인증 계층(`NFR-USR-001`)이 없어 생긴 임시 방식이며 인증 도입 시 전부 사라진다. 컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 **"인증 도입 전까지 운영 배포 대상이 아니다"**를 적는다 — `WorkspaceController` 선례.

경로는 모두 워크스페이스 하위에 중첩된다. `workspaceId`가 URL에 강제되면 데이터 격리(`NFR-WS-001`) 검증이 모든 엔드포인트에서 같은 모양이 된다.

| 상태 | Method | Path | 요청 | 응답 | 성공 | 권한 |
| --- | --- | --- | --- | --- | --- | --- |
| as-built | POST | `/api/workspaces/{workspaceId}/documents` | `CreateDocumentRequest{title, content, labels?}` | `DocumentResponse` | `201` | 참여자 |
| as-built | GET | `/api/workspaces/{workspaceId}/documents` | `label?`, `page?`, `size?`, `sort?` | `PageResponse<DocumentSummaryResponse>` | `200` | 참여자 |
| as-built | GET | `.../documents/{documentId}` | — | `DocumentResponse` | `200` | 참여자 |
| as-built | PATCH | `.../documents/{documentId}` | `UpdateDocumentRequest{title, labels}` | — | `204` | 참여자 |
| **추가** | **PATCH** | **`.../documents/{documentId}/content`** | **`EditDocumentContentRequest{content}`** | **`DocumentResponse`** | **`200`** | **참여자** |
| as-built | DELETE | `.../documents/{documentId}` | — | — | `204` | **ADMIN 이상** |
| as-built | GET | `.../documents/{documentId}/versions` | `page?`, `size?` | `PageResponse<DocumentVersionSummaryResponse>` | `200` | 참여자 |
| as-built | GET | `.../documents/{documentId}/versions/{versionNo}` | — | `DocumentVersionResponse` | `200` | 참여자 |
| as-built | GET | `/api/workspaces/{workspaceId}/labels` | — | `LabelResponse[]` | `200` | 참여자 |

**목록 두 개는 배열 → `PageResponse`로 바꾼다**(`Y-05`, `DOC-7`). 라벨 목록은 문서당 5개 상한과 필터 UI 용도라 **배열을 유지한다** — `API.md`가 「개수가 구조적으로 작은 목록은 예외」로 허용한다.

### 본문 편집 — 추가되는 유일한 엔드포인트

`PATCH /api/workspaces/{workspaceId}/documents/{documentId}/content?memberId={memberId}` → `200 OK`

**대조·초안·개정안을 거치지 않고 즉시 새 버전을 발행한다**(`G-9`). 응답이 `204`가 아닌 이유는 새 `currentVersionNo`와 갱신된 `aligned`를 클라이언트가 알아야 하기 때문이다.

- 새 버전은 `edited = true`, `dictionaryVersionNo`는 **이전 버전 값 승계**(`G-10`·`G-11`).
- `PATCH .../documents/{documentId}`(제목·라벨)와 **경로를 나눈다.** 본문 편집은 버전을 만들고 제목·라벨 수정은 만들지 않으므로 같은 엔드포인트에 섞으면 요청 하나가 두 의미를 갖는다.
- 진행 중인 문서 초안이 있거나(`D-30`) 진행 중인 사전 초안의 원천 문서이면(`G-15`) `409`로 거절한다.

### 응답 필드 변경 (`DOC-3`)

| 필드 | 상태 | 설명 |
| --- | --- | --- |
| ~~`outdated`~~ | **제거** | `R-18` — `aligned`가 대신한다 |
| `aligned` | **추가** | `dictionaryVersionNo == 활성 버전` AND `edited == false`. **참이면 추출 대상이고 갱신이 필요 없다** |
| `edited` | **추가** | 최신 확정 버전이 직접 편집본인지. `aligned`가 거짓인 이유를 구분하게 해 준다 |
| `dictionaryVersionNo` | as-built | 판정 근거를 볼 수 있게 남긴다 |

**세 필드를 함께 내리는 이유** — `aligned`만 주면 "왜 정렬되지 않았는지"(사전집이 올라갔는가 / 사람이 편집했는가)를 화면이 설명할 수 없다.

### 권한

| 대상 | 규칙 |
| --- | --- |
| 조회·생성·수정·**본문 편집** | 참여자면 누구나. `validateParticipant(workspaceId, memberId)` |
| 삭제 | **ADMIN 이상.** `validateAtLeast(workspaceId, memberId, Permission.ADMIN)` |
| 비참여자·삭제된 워크스페이스 | `404 WORKSPACE_NOT_FOUND` — 존재를 숨긴다 |
| 다른 워크스페이스의 문서 식별자 | `404 DOCUMENT_NOT_FOUND` |

권한 부족에 document 전용 코드를 두지 않고 `WorkspaceErrorCode`를 그대로 쓴다 — 검증 주체가 `WorkspaceAccessValidator`이므로 같은 뜻의 코드를 도메인마다 늘리지 않는다(as-built 결정).

**본문 편집을 참여자에게 여는 근거** — `DOMAIN.md` «워크스페이스 · 권한»이 「Regular는 문서 작성·교정·리뷰까지만 가능하다」로 두었고 편집은 작성에 해당한다. 삭제만 ADMIN 이상이다.

---

## 8. ErrorCode

`document/exception/DocumentErrorCode`, `document/exception/LabelErrorCode`. 접두사 하나당 enum 하나다 — `EXCEPTION.md`의 `{DOMAIN}_{REASON}` 규칙 때문에 한 enum에 두 접두사를 섞으면 enum 이름과 코드가 어긋난다.

| 상수 | status | message | 상태 | 태스크 |
| --- | --- | --- | --- | --- |
| `DOCUMENT_NOT_FOUND` | 404 | 문서를 찾을 수 없습니다. | as-built | — |
| `DOCUMENT_INVALID_TITLE` | 400 | 문서 제목은 1자 이상 200자 이하여야 합니다. | as-built | — |
| `DOCUMENT_INVALID_CONTENT` | 400 | 문서 본문은 1자 이상 10,000자 이하여야 합니다. | as-built | — |
| `DOCUMENT_INVALID_VERSION` | 400 | 문서 버전 정보가 올바르지 않습니다. | as-built | — |
| `DOCUMENT_VERSION_NOT_FOUND` | 404 | 문서 버전을 찾을 수 없습니다. | as-built | — |
| `DOCUMENT_LABEL_LIMIT_EXCEEDED` | 400 | 문서에 붙일 수 있는 라벨은 최대 5개입니다. | as-built | — |
| `DOCUMENT_DRAFT_IN_PROGRESS` | 409 | 진행 중인 초안이 있어 본문을 편집할 수 없습니다. | **추가** | `DOC-5` |
| `DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT` | 409 | 사전 초안의 원천 문서라 본문을 편집할 수 없습니다. | **추가** | `DOC-5` |
| `LABEL_INVALID_NAME` | 400 | 라벨 이름은 1자 이상 20자 이하여야 합니다. | as-built | — |

**두 차단 코드를 나누는 이유** — 사용자가 무엇을 끝내야 편집할 수 있는지가 다르다(자기 문서의 갱신을 끝내는 것 vs 남이 진행하는 사전 개정을 기다리는 것).

**추출 대상이 아님에 대한 에러 코드는 두지 않는다.** `aligned`는 조회 응답의 필드이고, 추출 요청을 거절하는 것은 `draftdictionary` 도메인의 책임이다.

---

## 9. 크로스 도메인 계약

### 접근 검증 — 포트를 만들지 않는다 (`D-19`)

**`WorkspaceAccessValidator`를 직접 주입한다.** `ARCHITECTURE.md` «크로스 도메인 조회 — 포트와 어댑터»의 명문 예외이며 `T-DOC-1`이 그 조항을 추가한다. as-built가 이미 이 방식이므로 **`DocumentService`의 주입부를 고치지 않는다.**

근거 셋 — ① 「비참여자에게 404」와 `Permission` 서열 판정이 한 곳에 남아야 한다 ② `validateAtLeast`는 예외를 던지고 값을 돌려주지 않아 `boolean` 포트에 그대로 쓸 수 없다 ③ 6개 도메인이 같은 방식이 된다.

### 소비 포트 (이 도메인이 정의하고 스텁까지 제공)

| 포트 | 시그니처 | 제공 도메인 | 스텁 동작 | 태스크 |
| --- | --- | --- | --- | --- |
| `DictionaryQueryPort` | `Optional<Integer> activeVersionNo(Long workspaceId)` | Dictionary(**머지됨**) | `Optional.empty()` | `DOC-1` |
| `DraftDocumentQueryPort` | `boolean hasOngoingDraft(Long documentId)` | DraftDocument(미착수) | `false` | `DOC-5` |
| `DraftDictionaryQueryPort` | `boolean isSourceOfOngoingDraft(Long documentId)` | DraftDictionary(미착수) | `false` | `DOC-5` |

**`DictionaryQueryPort`가 `Y-02`를 해소한다.** 현재 `DocumentService.activeDictionaryVersionNo()`가 `TODO`로 `null`을 반환해 판정이 항상 거짓인데, dictionary는 이미 구현돼 있고 `DictionaryRepository.findByWorkspaceIdAndStatus(workspaceId, ACTIVE)`가 필요한 값을 갖고 있다.

`Optional<Integer>`를 쓰는 이유는 **「사전집이 없다」와 「버전 0」을 구분해야** 하기 때문이다. 사전집이 없는 기간은 정상 상태다.

### 어댑터 배치

**조회 포트는 우리가 정의하고 어댑터도 우리가 구현한다.** 어댑터는 `document/infra/adapter/`에 두고 **제공 도메인의 `infra`(Repository)만 참조한다** — 같은 레이어끼리라 방향 위반이 아니다.

> **발행 위임 포트는 반대다**(`D-33`, 2026-09-12). `DOC-6`이 구현할 `DocumentVersionPublishPort`의 어댑터는 **우리가 제공 측으로서** `document/infra/adapter/`에 두고, 이때는 우리 `implement`·`service`를 쓴다. 발행이 우리 도메인 로직이기 때문이다. 소비 도메인(`reviewrequest`)은 스텁만 갖는다. 아래 문단의 「제공 도메인 infra만 참조」는 **조회 어댑터에 대한 규정**이다. 제공 도메인의 `implement`(`DictionaryReader` 등)를 참조하면 `infra -> implement`가 되어 `ARCHITECTURE.md`의 역방향 참조 금지를 어긴다. 이 규약 덕분에 **`dictionary` 패키지의 파일을 한 줄도 고치지 않는다.**

어댑터 선택은 프로퍼티로 한다 — `app.crossdomain.{name}.mode=stub|real`(기본 `stub`, `matchIfMissing = true`). `InMemoryEventPublisher`의 `@ConditionalOnProperty` 패턴을 따르고 `@ConditionalOnMissingBean`은 쓰지 않는다.

### 제공 포트 (다른 도메인이 우리를 볼 때 — 우리가 어댑터를 구현한다)

| 소비자가 정의한 포트 | 시그니처 | 근거 메서드 | 태스크 |
| --- | --- | --- | --- |
| `draftdictionary/infra/port/DocumentQueryPort` | `boolean isExtractable(Long documentId)` | `DocumentVersion.isAligned(...)`를 최신 확정 버전에 적용 | ~~`DOC-4`~~ **선행 PR 완료** |
| `draftdocument/infra/port/DocumentQueryPort` | `Optional<DocumentSnapshot> read(Long documentId)` | 최신 확정 버전의 `body`·`versionNo`를 담은 스냅샷 | ~~`DOC-4`~~ **선행 PR 완료** |
| `reviewrequest/infra/port/DocumentVersionPublishPort` | `int publish(Long documentId, int baseVersionNo, String body, int dictionaryVersionNo)` | `Document.publishNext` + `DocumentVersion.publishRevised` | `DOC-6` |

스냅샷 record — `DocumentSnapshot(Long documentId, Long workspaceId, int currentVersionNo, String body)`. **엔티티를 포트 시그니처에 노출하지 않는다.**

> **`isExtractable`이 `D-15`의 `isOutdated`를 대체한다**(`R-9`·`O-4`). 기존 계획 문서는 스텁이 `false`를 반환하고 정책이 유예된다고 확정했지만, `G-12`로 추출 대상 필터가 MVP1 필수가 됐다. **이름과 의미가 함께 바뀐다** — `isOutdated`는 참일 때 제외였고 `isExtractable`은 참일 때 포함이다. 소비 도메인이 논리를 반대로 쓰지 않도록 `CONFLICTS.md`가 이 전환을 기록한다.

> **`DocumentVersionPublishPort`에 `dictionaryVersionNo`를 추가한다**(`R-13`). 기존 시그니처로는 `G-7`(발행 시점 최신 사전집 버전)을 넣을 수 없다. 값을 ReviewRequest가 조회해 넘기는 이유는, 발행 시점의 활성 버전을 판단하는 것이 발행 트랜잭션의 책임이기 때문이다.

### 발행 이벤트

| 이벤트 | 페이로드 | 수신자 | 태스크 |
| --- | --- | --- | --- |
| `DocumentDeletedEvent` | `(Long documentId, Long workspaceId, OffsetDateTime occurredAt)` | 초안 도메인(파생 데이터 정리), Notification(미정) | `DOC-10` |
| `DocumentEditedEvent` | `(Long documentId, Long workspaceId, int versionNo, OffsetDateTime occurredAt)` | Notification(미정) | `DOC-10` |

**불변 record, 식별자·원시값·시각만 담는다.** 도메인 모델이 곧 JPA 엔티티이므로 엔티티·지연 로딩 프록시·연관 컬렉션을 담으면 안 된다. 인메모리 어댑터는 참조를 그대로 넘겨 로컬에서 위반이 드러나지 않으므로 **리뷰에서 확인한다.**

`implement`는 `common/infra/event/EventPublisher` 포트만 주입받는다. `ApplicationEventPublisher`를 직접 주입하지 않는다.

`DocumentRemover` javadoc이 「이벤트 발행은 구독자가 생기는 draftdocument 도메인 작업에서 함께 넣는다」고 미뤄 두었으나, **`G-14`의 상호 배타 검증이 초안 쪽 조회를 필요로 하므로 발행부를 `DOC-10`에서 먼저 만든다.** 구독자가 없어도 발행은 성립한다.

### 수신 이벤트

| 이벤트 | 발행자 | 처리 | 태스크 |
| --- | --- | --- | --- |
| `WorkspaceDeletedEvent` | Workspace | **처리하지 않는다** — 모든 조회가 `WorkspaceAccessValidator`에서 먼저 막히므로 문서 행을 지울 필요가 없다 | — |
| `DictionaryRevisedEvent` | Dictionary | **처리하지 않는다** — `aligned`는 파생 판정이라 사전집이 올라가면 저장 없이 자동으로 거짓이 된다 | — |

**수신 핸들러를 두지 않는 것이 이 도메인의 특징이다.** 두 이벤트 모두 파생 판정이나 조회 차단으로 이미 반영되므로, 핸들러를 만들면 같은 규칙이 두 벌이 된다.

---

## 10. 공유 파일 규약

### 공유 자산과 주인

| 자산 | 주인 | 규약 |
| --- | --- | --- |
| `common/presentation/PageResponse`, `common/service/PageResult` | **`T-CMN-1`(선행 공통 태스크)** | 규격은 `docs/API.md` «페이징·정렬 규격»을 그대로 따른다. 원래 `DraftDocument (DD-2)` 소유였으나 **기구현 3개의 페이징이 더 먼저 필요해** 선행 태스크로 분리했다(`Y-05`). `DOC-7`은 만들지 않고 **쓴다** |
| `common/domain/TextRange` | DraftDocument (`DD-2`) | 이 도메인은 쓰지 않는다 |
| `docs/API.md` Document 절 | **이 도메인** | 파일 안의 자기 절만 고친다. 공통 규칙·페이징·버저닝·에러 응답 형식 절은 건드리지 않는다 |

### 수정 금지 파일

`common/**`(위 2건 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`, 그리고 **다른 도메인의 패키지 전체**.

필요하면 통합 태스크(`T-*`)로 넘기고 PR에 이유를 적는다.

> **`dictionary` 패키지는 어댑터를 만들 때도 고치지 않는다.** 어댑터는 `document/infra/adapter/`에 두고 `dictionary/infra`의 Repository만 참조한다.

> **`docs/DOMAIN.md`·`REQUIREMENTS.md`·`ARCHITECTURE.md`·`UBIQUITOUS_LANGUAGE.md`·`TEST.md`는 `T-DOC-1`만 고친다.** 이 도메인의 `R-1`~`R-7`·`R-13`·`R-16`~`R-18`이 그 파일들을 향하지만, 직접 고치지 않고 `T-DOC-1`에 맡긴다.

### git worktree 운영

| 항목 | 규칙 | 이유 |
| --- | --- | --- |
| 브랜치명 | `feat/WLSH-{티켓번호}-document` | `.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다. 패턴이 없으면 티켓 추적이 끊긴다 |
| `bootRun` 동시 실행 금지 | 한 번에 한 worktree만 | `spring-boot-docker-compose`가 worktree마다 별도 Compose 프로젝트를 띄워 호스트 포트가 겹친다 |
| `./gradlew test` 동시 실행 주의 | 여러 worktree에서 동시에 돌리지 않는다 | Testcontainers가 worktree 수만큼 뜨고 LGTM과 겹치면 Docker 기본 메모리에서 OOM |
| develop 동기화 | `T-DOC-1`·`T-CMN-1` 머지 직후 **즉시** | 문서 기준과 `PageResponse`가 그때 바뀐다 |
| 파일 소유권 | 3절 표를 따른다 | 선착순은 곧 중복 생성이다 |

---

## 11. 테스트 계획

| 계층 | 방식 |
| --- | --- |
| Unit(domain) | 순수 JUnit, Spring 없음. `@Nested`로 케이스를 묶는다 |
| Repository(infra) | `RepositoryTestSupport` 상속 — `@DataJpaTest` + 실제 MySQL 컨테이너 + **Flyway가 만든 스키마** |
| Service | `IntegrationTestSupport` 상속 — `@SpringBootTest(webEnvironment = NONE)` + `DbCleaner` |
| Controller(presentation) | `@WebMvcTest(XxxController.class)` + `@MockitoBean` + **RestAssuredMockMvc** |

> **개별 테스트에 `@Transactional`을 붙이지 않는다.** 서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써 경계 자체를 검증하지 못하게 된다 — `IntegrationTestSupport` javadoc. `docs/TEST.md`가 롤백을 권장하는 것은 낡은 서술이며 `T-DOC-1`이 정정한다(`D-32`).

> **테스트 전용 엔티티를 만들지 않는다.** 스키마는 Flyway가 만들고 `DbCleaner`가 `information_schema`에서 실제 테이블을 읽어 비운다.

AssertJ를 쓴다(`assertThat`·`assertThatThrownBy`·`extracting`). JUnit `assertEquals`·`assertThrows`는 쓰지 않는다. `@DisplayName`은 한국어 완결 문장 + 마침표, 메서드명은 `{메서드명}_{조건}` camelCase다.

### 테스트 클래스와 필수 케이스

| 클래스 | 계층 | 필수 케이스 | 태스크 |
| --- | --- | --- | --- |
| `domain/DocumentTest` | Unit | as-built + **`publishNext_increasesVersionNo`** / `publishNext_updatesUpdaterId` | `DOC-2` |
| `domain/DocumentVersionTest` | Unit | as-built(`isOutdated_*` **삭제**) + **`isAligned_dictionaryIsAbsent`("활성 사전집이 없으면 정렬된 것으로 본다.")** / **`isAligned_edited`("직접 편집한 버전은 사전집 버전이 같아도 정렬되지 않은 것으로 본다.")** / `isAligned_dictionaryVersionDiffers` / `publishEdited_marksEdited` / `publishEdited_inheritsDictionaryVersionNo` / `publishRevised_takesActiveDictionaryVersionNo` | `DOC-2`·`DOC-3` |
| `domain/PublishedVersionTest` | Unit | as-built + `next_increasesVersionNoAndStampsNewTime` | `DOC-2` |
| `domain/LabelTest`·`DocumentLabelTest` | Unit | as-built | — |
| `implement/DocumentEditGuardTest` | Unit | **`validate_ongoingDocumentDraftExists`(409)** / **`validate_sourceOfOngoingDictionaryDraft`(409)** / `validate_noDraft` | `DOC-5` |
| `implement/DocumentAlignmentReaderTest` | Unit | `readAligned_dictionaryIsAbsent` / `readAligned_edited` | `DOC-1` |
| `infra/DocumentRepositoryTest` | Repository | as-built + **`findAll_paging`** / `findAll_sortWhitelist` | `DOC-7` |
| `infra/DocumentVersionRepositoryTest` | Repository | as-built + **`findCurrentSummaries_includesEdited`** | `DOC-3` |
| `infra/LabelRepositoryTest` | Repository | as-built | — |
| `infra/adapter/DictionaryQueryAdapterTest` | Repository | **`activeVersionNo_returnsActiveOnly`("보관 버전은 활성 버전으로 읽히지 않는다.")** / `activeVersionNo_dictionaryIsAbsent` | `DOC-1` |
| `service/DocumentServiceTest` | Service | as-built(23건) + **`editContent_publishesNewVersion`("본문을 편집하면 새 버전이 발행된다.")** / **`editContent_inheritsDictionaryVersionNo`** / **`editContent_ongoingDraftExists`(409)** / **`readAll_alignedIsFalseWhenDictionaryIsRevised`("사전집이 새 버전을 발행하면 문서가 정렬되지 않은 것으로 바뀐다.")** / `read_alignedWithActiveDictionary` / 접근 검증 3건(`_memberIsNotParticipant` 404 / `_permissionIsBelowAdmin` 403 / `_workspaceIsDeleted` 404) | `DOC-1`~`DOC-6` |
| `presentation/DocumentControllerTest` | Controller | as-built + **`editContent`** / `editContent_contentIsBlank`(400) / **`readAll_containsAlignedAndEdited`** / `readAll_doesNotContainOutdated` | `DOC-2`·`DOC-3` |
| `presentation/LabelControllerTest` | Controller | as-built | — |

**`readAll_alignedIsFalseWhenDictionaryIsRevised`가 이 도메인의 회귀 방지 핵심이다.** `Y-02`가 되살아나면(포트가 스텁으로 돌아가면) 이 케이스가 먼저 깨진다.

### Fixture

`document/fixture/`에 둔다(src/test 전용). 정적 팩토리 + 내부 `static class XxxBuilder`, 기본값 보유, 체이닝, `build()`는 항상 유효한 객체를 돌려준다. 운영 경로에 없는 상태는 `ReflectionTestUtils.setField`로 주입한다 — `WorkspaceFixture` 선례.

모든 필드를 받는 거대 fixture나 `create(id, ...)` 형태는 만들지 않는다.

| Fixture | 변경 |
| --- | --- |
| `DocumentFixture` | as-built. 경계값 상수(`MAX_LENGTH_BODY`·`TOO_LONG_BODY`·`DEFAULT_BODY`) 유지 |
| `DocumentVersionFixture` | **`edited(boolean)` 빌더 메서드 추가.** javadoc의 「v2 이상과 대조를 거친 버전은 반영으로만 생기는데 그 경로가 아직 없다」는 서술을 **`G-9`·`G-7`로 경로가 생겼음**에 맞춰 고친다 |
| `LabelFixture` | as-built |

---

## 12. Phase와 태스크

| Phase | 목표 | 명시적 제외 |
| --- | --- | --- |
| 1 | **정렬 판정을 실제로 동작하게 만든다** — 사전집 배선 + `edited` + `aligned` | 편집 차단(초안 도메인이 없다) |
| 2 | 본문 직접 편집과 차단 검증 | 교정 반영(리뷰 도메인이 없다) |
| 3 | 다른 도메인이 우리를 보는 창구 — 제공 포트·발행 어댑터 | — |
| 4 | 규약 정합 — 페이징·로그·이벤트·ID 값 객체 | — |

| ID | 태스크 | 산출물 | 일수 | 의존 |
| --- | --- | --- | --- | --- |
| **DOC-1** | 사전집 활성 버전 조회 배선 | 포트 1 + 어댑터 1 + 스텁 1 + `DocumentAlignmentReader` + 프로퍼티 + 테스트 3 | 1 | — |
| **DOC-2** | 본문 직접 편집 → 즉시 발행 | `V210` + `Document.publishNext` + `DocumentVersion.publishEdited` + `EditDocumentContentRequest`·`Command` + 서비스 1 + 엔드포인트 1 + 테스트 6 | 2 | `DOC-1` |
| **DOC-3** | `outdated` 제거·`aligned` 통합 | `isAligned` static + `DocumentVersionSummary` 수정 + result 4 + response 4 + 테스트 5 | 1 | `DOC-1`, `DOC-2` |
| ~~**DOC-4**~~ | ~~제공 포트 — 추출 대상·문서 스냅샷~~ | **선행 PR(`chore/WLSH-145-contracts`)이 끝냈다**(2026-09-12) — 산출물 없음 | — | — |
| **DOC-5** | 편집 차단 검증 | 포트 2 + 스텁 2 + 어댑터 2 + `DocumentEditGuard` + ErrorCode 2 + 테스트 3 | 1 | `DOC-2`, **`DD-1`**, **`DI-1`** |
| **DOC-6** | 교정 반영 발행 어댑터 | `DocumentVersion.publishRevised` + 어댑터 1 + 테스트 2 | 1 | `DOC-2`, **`RR-4b`** |
| **DOC-7** | 목록·버전 이력 페이징 | Repository 2 + result 2 + response 2 + 테스트 2 | 1 | **`T-CMN-1`** |
| **DOC-8** | ID 값 객체 제거 | record 4 삭제 + 테스트 1 삭제 | 0.5 | — |
| **DOC-9** | 감사 로그 | 서비스·implement 로그 + 테스트 0 | 0.5 | `DOC-2` |
| **DOC-10** | 삭제·편집 이벤트 발행 | 이벤트 record 2 + `DocumentRemover`·서비스 수정 + 테스트 2 | 1 | `DOC-2` |

**`DOC-1`~`DOC-3`을 한 덩어리로 본다.** `aligned`를 넣으려면 활성 사전집 버전을 읽을 수 있어야 하고(`DOC-1`) 조건에 `edited`가 들어가므로(`DOC-2`), 나누어 머지하면 중간 상태에서 응답 필드가 두 번 바뀐다. **커밋은 태스크별로 나누되 같은 PR로 올린다.**

`DOC-5`는 **초안 도메인의 루트 CRUD(`DD-1`·`DI-1`)가 develop에 있어야** real 어댑터를 만들 수 있다. 그 전에는 스텁이 `false`를 반환해 편집이 항상 허용된다 — 스텁 단계에서 `DOC-5`를 머지해도 동작은 as-built와 같다.

### Phase별 DoD

**공통 (모든 Phase)**

- [ ] `./gradlew spotlessApply && ./gradlew check` 초록
- [ ] 새 로직에 테스트를 함께 작성했다. 실패하는 테스트를 삭제하거나 조건을 완화하지 않았다
- [ ] `docs/API.md`의 Document 절만 갱신했다(공통 규칙 절은 건드리지 않았다)
- [ ] 엔티티를 result·response·이벤트·포트 시그니처에 담지 않았다
- [ ] `implement`에 `@Transactional`을 선언하지 않았다

**DOC-1**

- [ ] `document/infra/port/DictionaryQueryPort`가 `Optional<Integer>`를 반환한다 — 「사전집 없음」과 「버전 0」이 구분된다
- [ ] 어댑터가 `dictionary/infra/DictionaryRepository`만 참조한다. `DictionaryReader`·`DictionaryService`를 참조하지 않는다
- [ ] `app.crossdomain.dictionary.mode=stub|real`로 전환된다. `@ConditionalOnMissingBean`을 쓰지 않았다
- [ ] `DocumentService.activeDictionaryVersionNo()`의 `TODO`가 사라졌다
- [ ] 보관(`ARCHIVED`) 사전집이 활성으로 읽히지 않는다

**DOC-2**

- [ ] `V210`이 `default 0`으로 기존 행을 채운다
- [ ] `Document.publishNext`가 버전 번호를 올리는 **유일한 지점**이다
- [ ] `publishEdited`가 `edited = true`와 **이전 버전 승계**를 함께 보장한다
- [ ] `PATCH .../content`가 제목·라벨 수정 엔드포인트와 분리돼 있다
- [ ] `Document`·`DocumentService` javadoc의 「본문을 바꾸는 메서드가 없다」를 고쳤다(`R-7`)

**DOC-3**

- [ ] `isOutdated` 두 메서드와 `outdated` 응답 필드가 코드에서 사라졌다
- [ ] `aligned` 판정이 `DocumentVersion`의 static 한 곳에 있고 `DocumentVersionSummary`가 그것을 재호출한다 — **규칙이 두 벌이 아니다**
- [ ] 응답에 `aligned`·`edited`·`dictionaryVersionNo` 셋이 함께 있다
- [ ] 활성 사전집이 없을 때 `aligned`가 참이다

**~~DOC-4~~ — 선행 PR이 충족했다.** `D-33`으로 조회 어댑터의 주인이 소비 도메인이 되면서 산출물이 `draftdictionary`·`draftdocument` 패키지로 옮겨 갔다. **`document-phase-3` 세션은 이것을 다시 만들지 않는다.**

- [x] `isExtractable`이 `aligned`와 같은 static을 호출한다 — `draftdictionary/infra/adapter/DocumentQueryAdapter`
- [x] `DocumentSnapshot`에 엔티티가 없다 — `draftdocument/infra/port/DocumentSnapshot`
- [x] `CONFLICTS.md`의 `O-4`에 `isOutdated → isExtractable` 전환을 기록했다

**DOC-5**

- [ ] 두 차단 조건이 각각 다른 ErrorCode로 구분된다
- [ ] 초안 조회 하나가 초안과 개정안을 함께 덮는다(`D-22`) — `ReviewRequest`를 조회하지 않는다
- [ ] 스텁이 `false`를 반환해 초안 도메인 없이도 편집이 동작한다

**DOC-6**

- [ ] 포트 시그니처에 `dictionaryVersionNo`가 있다(`R-13`)
- [ ] 반영본이 `edited = false`다
- [ ] `resultVersionNo`를 반환해 `RevisionDocument`가 채울 수 있다

**DOC-7**

- [ ] `T-CMN-1`의 `PageResponse`·`PageResult`를 **쓴다**(만들지 않는다)
- [ ] 정렬 필드 화이트리스트 밖이면 `400`이다
- [ ] service가 Spring `Page`를 반환하지 않는다
- [ ] 라벨 목록은 배열을 유지한다

**DOC-8**

- [ ] `DocumentId`·`DocumentVersionId`·`DocumentLabelId`·`LabelId`와 `DocumentIdTest`가 사라졌다
- [ ] 다른 도메인이 이 record들을 참조하지 않는다

**DOC-9**

- [ ] 로그가 `[클래스명.메서드명] 영문 문장 key={}` 형식이다
- [ ] 본문 편집 성공·편집 차단(WARN)·문서 삭제(INFO)를 남긴다
- [ ] request body 전체나 본문 원문을 로그에 남기지 않는다

**DOC-10**

- [ ] 이벤트가 식별자·원시값·시각만 담은 불변 record다
- [ ] `implement`가 `EventPublisher` 포트만 주입받는다
- [ ] 구독자가 없어도 발행이 성립한다

### 통합 태스크 (6개 도메인 공통)

| ID | 내용 | 의존 |
| --- | --- | --- |
| `T-DOC-1` | **문서 선행 수정** — `R-1`~`R-24`, `D-19`~`D-32`. `CONFLICTS.md` 9절이 파일별 목록을 갖는다 | — (**모든 구현의 선행**) |
| `T-CMN-1` | `PageResponse`·`PageResult` 신설 | `T-DOC-1` |
| ~~`T-INT-1`~~ | ~~`spring.flyway.out-of-order=true` 정리~~ — **폐기**(2026-09-12). 개발 브랜치 DB를 항상 리셋하므로 머지 순서와 번호 순서가 어긋나도 무방하다(`Y-10`) | — |
| `T-INT-2` | 크로스 도메인 어댑터를 `real`로 전환 | 6개 도메인 Phase 3 |
| `T-INT-3` | `SecurityConfig` + 인증 주체 + 프로파일 분리 | 인증 도메인(별건) |

**`T-INT-4`는 신설하지 않는다** — `D-26`으로 `originRevisionId`를 두지 않기로 해 필요가 사라졌다.

---

## 13. 리스크와 열린 질문

| 항목 | 내용 | 대응 |
| --- | --- | --- |
| **`aligned` 규칙이 두 벌이 될 위험** | 목록은 본문을 빼고 읽으므로 엔티티와 프로젝션 두 곳에서 판정한다. as-built가 static 공유로 막았지만 `edited`가 인자로 추가되면서 시그니처가 3인자로 늘어난다 | `DOC-3` DoD에 「규칙이 두 벌이 아니다」를 넣었다. `DocumentVersionSummary.isAligned`가 자기 로직을 갖지 않는지 리뷰에서 본다 |
| **편집과 초안이 경합한다** | `DOC-5`가 머지되기 전(스텁 단계)에는 편집이 항상 허용되므로, 초안 진행 중에 편집이 들어가 `baseVersionNo`가 낡을 수 있다 | 초안 도메인(`DD-1`)보다 `DOC-5`를 늦게 두되, **초안 생성 쪽에서도 `baseVersionNo`가 현재 버전과 다르면 거절**하도록 `DD-*`에 요청한다 |
| **`G-11` 승계가 낳는 오해** | 편집본이 이전 사전집 버전을 승계하므로 `dictionaryVersionNo`만 보면 「통과했다」로 읽힌다 | `edited`를 응답에 함께 내려 구분하게 한다(`F-3`). 컬럼 주석과 `DOMAIN.md`에 승계임을 명시한다 |
| **`PATCH` 두 개의 혼동** | `.../documents/{id}`(제목·라벨)와 `.../documents/{id}/content`(본문)가 같은 동사를 쓴다 | `API.md`에 「본문은 하위 경로, 나머지는 상위 경로」를 못박고 상위 `PATCH`의 응답을 `204`로 유지해 차이를 드러낸다 |
| **`DOC-6`이 리뷰 도메인을 기다린다** | 교정 반영 경로가 `RR-4b` 이후라 그때까지 v2 이상이 **편집으로만** 생긴다 | 의도된 순서다. `DocumentVersionFixture`가 그 사이 테스트를 받친다 |
| **MongoDB 용처 미정** | 의존성·컨테이너만 있고 사용 코드가 0줄이다. `D-24`로 메시징이 SQS가 되어 후보 하나가 사라졌다 | 이 도메인은 쓰지 않는다. `CONFLICTS.md` `Y-22`로 남긴다 |

### 열린 질문

- **`aligned`의 응답 이름을 그대로 쓸지** — `UBIQUITOUS_LANGUAGE.md`에 「정렬됨/aligned」를 등재하기로 확정했으나(`D-31`), 화면 문구는 「최신 사전집 기준」처럼 다르게 갈 수 있다. **API 필드명은 `aligned`로 고정**하고 화면 문구는 프론트 몫으로 둔다.
- **편집에 낙관적 락이 필요한지** — 두 사람이 같은 문서를 동시에 편집하면 뒤에 온 요청이 앞의 버전 위에 또 버전을 쌓는다. 본문이 사라지지는 않지만 의도하지 않은 버전이 생긴다. `NFR-REV-002`는 리뷰 요청에만 낙관적 락을 요구하므로 **MVP1에서는 두지 않고**, 편집 요청에 `baseVersionNo`를 받아 현재 버전과 다르면 `409`를 주는 안을 `DOC-2`에서 검토한다.
- **제목·라벨 수정도 `updaterId`를 갱신하는데 버전은 만들지 않는다** — as-built 동작이며 `Document.touch`가 그 자리다. 편집이 들어온 뒤에도 이 비대칭을 유지한다(제목은 본문이 아니다).

### DOMAIN.md 수정 (`T-DOC-1`이 반영)

1. 문서 정책 「본문이 바뀌는 유일한 경로는 대조 → 초안 → 리뷰 → 반영이다. 업로드(v1)만 예외」 → **편집도 예외**(`R-1`)
2. 문서 정책 「대조의 진입점은 둘이고 산출물은 같다」 → **진입점은 「갱신」 하나**(`R-2`)
3. 문서 정책 「수정본을 따로 저장하는 자리는 없다」 → **편집본이 곧 새 버전**(`R-3`)
4. `DocumentVersion` 표에 **`edited` 1행 추가**(`G-10`), `originRevisionId` 행을 「두지 않는다」로(`D-26`)
5. 문서 정책의 outdated 판정 → **`aligned`**(`D-31`·`R-18`)
6. 문서 정책 「outdated 문서는 용어 추출 대상이 아니다」 → **`aligned`가 아닌 문서는 추출 대상이 아니다**(`G-12`, `edited` 조건 포함)
7. 초안 정책에 **워크스페이스 단위 상호 배타**(`G-14`)와 **편집 차단 2건**(`G-15`·`D-30`) 추가
8. 속성 표의 `DocumentId`·`DocumentVersionId` 등에 **「개념 표기」 각주**(`D-25`)

### 리뷰 체크리스트 대조

| 출처 | 항목 | 이 문서 반영 위치 |
| --- | --- | --- |
| `ARCHITECTURE.md` | service가 Repository·기술 객체를 직접 참조하지 않는가 / 레이어 건너뛰기 / 도메인 간 직접 참조 / 이벤트 페이로드 | 9절, 12절 DoD |
| `TEST.md` | 계층별 방식 / Fixture / AssertJ / `@Transactional` 금지 | 11절 |
| `LOG.md` | prefix 규약 / 민감 정보 / 레벨 | 12절 `DOC-9` DoD |
| `API.md` | 페이징·정렬 / 에러 형식 / 요청자 식별 / 404 정책 | 7절, 12절 `DOC-7` DoD |
| `EXCEPTION.md` | `{DOMAIN}_{REASON}` / 중복 코드 금지 / 던지는 위치 | 8절 |
