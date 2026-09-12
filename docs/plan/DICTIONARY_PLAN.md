# Dictionary 도메인 구현 계획

사전집(Dictionary) 도메인의 구현 계획이다. `docs/DOMAIN.md`·`docs/ARCHITECTURE.md`·`docs/TEST.md`·`docs/API.md`·`docs/LOG.md`·`docs/EXCEPTION.md`를 먼저 읽고, 이 문서는 그 규칙을 이 도메인에 적용한 결과로 읽는다.

여섯 도메인 계획 문서는 같은 목차를 쓴다 — `WORKSPACE_PLAN.md`, `DOCUMENT_PLAN.md`, `DRAFT_DOCUMENT_PLAN.md`, `DRAFT_DICTIONARY_PLAN.md`, `REVIEW_REQUEST_PLAN.md`.

**이 도메인은 이미 상당 부분 구현돼 있다.** 3·5·6·7절의 표에 `상태` 칸을 두어 `as-built` / `추가` / `변경` / `제거` / `대기`를 구분한다.

**`docs/plan/CONFLICTS.md`를 함께 읽는다.** 큰 흐름 확정(`G-*`)·이번 세션 확정(`D-19`~`D-32`)·뒤집힌 결정(`R-*`)이 거기에 있고, 이 문서는 ID로 참조한다. **자기 서술과 `R-*`가 어긋나면 `R-*`를 따른다.**

**여러 세션으로 나눠 구현한다면 `EXECUTION_ORDER.md`를 먼저 읽는다.**

---

## 1. 범위와 목표

**담당 애그리게이트**: `Dictionary`(루트, `DictionaryVersion` 값 객체 포함), `Term`

**목표**: 워크스페이스의 **확정된 표준 용어 집합**을 버전 단위로 보관하고, 리뷰 승인이 위임한 발행을 수행한다. 대조·추출·정렬 판정이 기준으로 삼는 「활성 사전집」을 제공한다.

**관련 요구사항**: `REQ-DIC-001`(1차 사전집 확정 — 진행중), `REQ-DIC-002`(조회·검색 — 대기), `REQ-DIC-005`(버전 발행 — 진행중), `REQ-DIC-006`(버전 이력 — 완료), `REQ-DIC-009`(사전집 조회 — 완료), `REQ-DIC-007`(버전 간 비교 — 대기), `NFR-DIC-002`(대표어 유일성)

**큰 흐름이 이 도메인에 준 변경 셋**

1. **사전 초안이 통합 결과가 된다**(`G-1`) — 초안이 「이전 사전집 + 추출 용어」의 전체 목록을 들고, 발행은 그 목록을 그대로 받는다. **현재 `revise`가 이미 그 형태여서 코드 변경이 거의 없다.**
2. **발행은 리뷰 승인 뒤 ADMIN 수동**(`G-3`) — 지금 열려 있는 직접 반영 엔드포인트는 임시 API로 태그하고 리뷰 도메인 완성 시 제거한다(`D-27`).
3. **발행 위임 포트의 시그니처가 값으로 바뀐다**(`R-12`) — 후보어 ID 목록이 아니라 용어 스냅샷 목록을 받는다.

### MVP1 범위 밖 · 다른 도메인 소관

| 대상 | 사유 | 대체 |
| --- | --- | --- |
| **`REQ-DIC-004` 용어 수동 추가·수정·삭제** | **「MVP1 범위 밖」이 아니라 「다른 도메인 소관」이다**(`R-11`). 요구사항은 유효하고, `DOMAIN.md` «초안 공통» 표가 `교정중` 상태에서 「항목 추가·수정·삭제」를 이미 허용한다 | **`draftdictionary`의 초안 교정.** 확정 사전집의 `Term`을 직접 고치는 경로는 이 도메인에 만들지 않는다 |
| 용어 추출(`REQ-EXT-*`) | 산출물이 초안이다 | `draftdictionary` + AI 연동 도메인 |
| 후보어 판정·교정 | 같은 이유 | `draftdictionary` |
| `REQ-DIC-003`의 동의어·비권장어 표시 | **`D-28`로 내렸다.** 대조가 저장된 표기 목록을 훑는 방식이 아니라 LLM이 맥락을 파악하는 방식이라는 제품 설계와 일관된다 | 최근 변경 이력은 `REQ-DIC-007`(버전 간 비교)이 대신한다 |
| 유래 리비전(`originRevisionId`) | `D-26` — `RevisionDictionary.resultVersionNo`가 같은 관계의 반대 방향이라 정보가 중복이다 | 컬럼을 만들지 않는다 |
| `REQ-DIC-008` 내보내기(CSV/MD/JSON) | 우선순위 「하」 | MVP2 |
| 역인덱스(`REQ-IDX-*`) | `D-29` — MVP2로 내렸다 | — |
| 벡터·임베딩 | `D-23` — MVP1에서 배제, MySQL 유지 | — |

**사전집을 사람이 빈 껍데기로 만드는 경로는 없다.** 첫 버전조차 「문서 → 추출 → 초안 → 리뷰 → 승인」을 거쳐 태어난다(`DOMAIN.md` «사전집 생성 주기»).

---

## 2. 결정 대기표

### 2-1. 확정된 결정

| ID | 확정 내용 | 문서 수정 |
| --- | --- | --- |
| **G-1** | **사전 초안은 통합 결과다.** 발행은 초안이 만든 **차기 버전의 전체 용어 목록**을 받아 그대로 새 버전으로 만든다. **as-built가 이미 이 형태다** — `TermAppender.appendAll` javadoc이 "이전 버전에서 복사하지 않고 넘어온 목록만 그 버전의 내용이 된다"고 못박고 있다 | — |
| **G-2** | 발행 요청 주체는 **ADMIN 이상**. as-built가 `validateAtLeast(..., Permission.ADMIN)`으로 이미 검증한다 | — |
| **G-3** | 발행은 **리뷰 승인 뒤 ADMIN 수동**이다. 이 도메인은 발행을 **위임받아 수행**할 뿐 승인 여부를 판단하지 않는다 | — |
| **R-12** | **`DictionaryVersionPublishPort`가 통합 용어 목록을 받는다.** 기존 시그니처(`List<Long> approvedCandidateTermIds`)는 Dictionary가 후보어를 조회해야 해 `dictionary -> draftdictionary` 역방향을 만들고, 이 도메인에는 이전 버전 복사 로직이 아예 없다 | `REVIEW_REQUEST_PLAN.md` 9절(보존 — `CONFLICTS.md`가 지목) |
| **D-27** | **`POST /dictionary/versions`를 임시 API로 태그해 유지하고 `DIC-7`에서 제거한다.** 지금 잠그면 사전집을 만들 방법이 없어 `DOC-1`(활성 버전 조회)과 `aligned` 판정을 검증할 수 없다. `REVIEW_REQUEST_PLAN.md` 7절의 `INTERNALIZE` 태그 체계를 재사용한다 | `API.md`에 「리뷰 도메인 완성 시 제거」 명시 |
| **D-28** | `REQ-DIC-003`의 동의어·비권장어 표시를 내린다 | `R-24` |
| **D-26** | 유래 리비전을 두지 않는다 | `DOMAIN.md` `DictionaryVersion` 각주 |
| **D-19** | **접근 검증은 포트를 만들지 않고 `WorkspaceAccessValidator`를 직접 주입한다.** as-built가 이미 그 방식이므로 **`DictionaryService`를 고치지 않는다** | `ARCHITECTURE.md` 예외 조항 |
| **D-25** | **`DictionaryId`·`TermId`와 `DictionaryIdTest`·`TermIdTest`를 제거하고 `Long`으로 통일한다** | `DOMAIN.md` 속성 표 각주 |
| **Y-07** | **감사 상위 클래스를 `BaseEntity` 하나로 통일한다.** `AuditableEntity`는 제거하고 삭제 경로가 없는 엔티티도 nullable `deletedAt`을 공통 규약으로 가진다 | — |
| **Y-08** | **`Term`이 `BaseEntity`를 상속하도록 바꾼다.** 지금은 `@CreationTimestamp`/`@UpdateTimestamp`를 직접 선언하므로 공통 감사 규약을 타지 않는다 | — |
| **활성 유일성은 DB가 지킨다** | `active_flag` 생성 컬럼(`case when status = 'ACTIVE' then 1 else null end`) + `uk_dictionary_workspace_active`. MySQL이 UNIQUE에서 NULL을 서로 다른 값으로 보므로 보관 행은 몇 개든 쌓이고 활성 행은 두 개째가 막힌다 — as-built | — |
| **보관 전환은 명시적 flush** | `DictionaryUpdater.archive`가 `saveAndFlush`를 쓴다. Hibernate가 INSERT를 UPDATE보다 먼저 실행하므로 보관 전환을 미루면 활성 사전집이 순간 둘이 되어 유니크 제약에 걸린다 — as-built이며 근거를 javadoc이 갖고 있다 | — |
| **용어 0개 버전 금지** | `TermAppender.appendAll`이 빈 목록을 `DICTIONARY_EMPTY_TERMS`로 거절한다 — as-built | — |
| **표준어 유일성 이중 방어** | `TermFormValidator`가 요청 안의 중복을 저장 전에 잡고(`NFR-DIC-002`), `uk_term_dictionary_preferred_form`이 최후 방어선이다. DB 제약에 닿으면 `DataIntegrityViolationException`이 되어 사용자에게 줄 메시지를 만들 수 없다 — as-built | — |
| **사전집은 삭제하지 않는다** | 모든 행이 보존해야 할 버전 이력이다. `DictionaryRepository`가 조회 조건에 `deletedAt`을 넣지 않는다 — as-built | — |
| **경로는 단수 `/dictionary`** | 워크스페이스에 활성 1개 + 보관 N개로 존재하고 「이름」을 두지 않으므로, 리소스 컬렉션이 아니라 워크스페이스의 단일 속성처럼 읽힌다. 버전 목록은 `/dictionary/versions` 하위에 둔다 — as-built | `API.md`에 근거 명시 |
| **D-18** | 하위 디렉터리는 `presentation/dto/`·`service/model/`, ErrorCode는 `{domain}/exception/` — as-built가 이미 준수. **이 도메인이 최신 패턴의 기준이다** | — |

**`R-12`가 이 도메인의 가장 중요한 결정이다.** `G-1`(통합 모델)과 `TermAppender`의 기존 규약이 맞물려, 발행 포트를 값 기반으로 바꾸면 **`dictionary`가 다른 도메인을 조회할 필요가 아예 없어진다.** 이 도메인은 소비 포트를 하나도 갖지 않는 유일한 도메인이 된다.

### 2-2. 남은 결정 대기

**없다. 2026-09-10에 전건 확정했다.** 모든 Phase에 착수할 수 있다.

`docs/DOMAIN.md` «모델 반영 필요(미확정)» 블록 중 이 도메인에 걸리는 것은 없다. `DRAFT_DICTIONARY_PLAN.md` 13절이 남긴 **`O-1`(`Term` 생성 주체)은 `G-1`·`G-3`으로 해소됐다** — 초안이 최종 목록을 제공하고 이 도메인이 발행한다.

---

## 3. 도메인 모델

### Dictionary (애그리게이트 루트)

**사전집 행 하나가 확정된 버전 하나다.** 워크스페이스에 행이 쌓이고 활성중인 행 하나가 가장 최근 확정본이자 대조·추출의 기준이다.

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | `@GeneratedValue(IDENTITY)` |
| `workspaceId` | `Long` | `workspace_id` | X | as-built | `updatable = false`. DB FK 있음 |
| `version` | `DictionaryVersion` | `version_no`, `published_at` | X | as-built | `@Embedded` 값 객체 |
| `status` | `DictionaryStatus` | `status` | X | as-built | `@Enumerated(STRING)`, `varchar(20)` |
| `createdBy` | `Long` | `created_by` | X | as-built | `updatable = false`. 반영을 수행한 사람 |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | as-built | `BaseEntity` 상속. 삭제 유스케이스는 제공하지 않는다 |

`Term` 컬렉션을 매달지 않는다 — 용어 수백 개를 통째로 끌고 다니지 않기 위해 `TermReader`가 따로 읽는다. 「이름(`name`)」도 두지 않는다 — 워크스페이스에 활성 사전집이 정확히 1개이므로 구분해 부를 이름이 필요 없다.

### Term

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | |
| `dictionaryId` | `Long` | `dictionary_id` | X | as-built | **같은 도메인이라 DB FK를 건다** |
| `preferredForm` | `String` | `preferred_form` | X | as-built | `varchar(100)`. **사전집 내 유일.** 앞뒤 공백 제거 후 대소문자 구분 |
| `englishName` | `String` | `english_name` | O | as-built | `varchar(100)`. 코드·DB 네이밍 기준. 빈 문자열은 `null`로 모은다 |
| `definition` | `String` | `definition` | X | as-built | `@Lob`, `text`. **대조 시 LLM의 판단 근거** |
| `createdBy` | `Long` | `created_by` | X | as-built | |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | **변경** | **직접 선언 → `BaseEntity` 상속**(`Y-08`). 삭제 유스케이스는 제공하지 않는다 |

전 필드가 `updatable = false`다. 개별 용어를 고치거나 지우는 경로가 없고, 용어를 바꾸려면 **새 버전을 발행**해야 한다.

**동의어·비권장어를 두지 않는다.** 대조는 저장된 표기 목록을 훑는 방식이 아니라 LLM이 문맥을 파악해 표준어·정의와 비교하는 방식이므로, `definition`이 판단 근거를 대신한다(`D-28`).

### 애그리게이트 경계

`Dictionary`와 `Term`은 **같은 애그리게이트**지만 JPA 컬렉션으로 묶지 않는다.

- 연관관계 매핑을 쓰지 않는다. 애그리게이트 경계를 식별자로 넘어 지연 로딩 프록시가 상위 레이어로 새는 경로를 막는다 — DB에는 FK가 그대로 있다.
- **버전마다 사전집 행이 복제되면서 `Term`도 함께 복제되므로, 각 버전에 매달린 `Term`이 곧 그 버전의 스냅샷이다.** 아무도 편집하지 않으므로 저절로 불변이 된다 — 별도 스냅샷 엔티티(`TermVersion`)를 두지 않는 근거다.
- 「활성 사전집은 워크스페이스당 1개」는 **DB가 지킨다**(`active_flag` + 유니크). 애플리케이션 검증에 기대지 않는다.

### 값 객체

`DictionaryVersion`(`@Embeddable record`) — `versionNo`(1부터, 워크스페이스 안에서 유일) + `publishedAt`. **사전집 행 하나가 곧 확정된 버전 하나이므로 미확정 상태가 존재하지 않고**, 행이 만들어지는 순간이 확정 순간이라 `publishedAt`은 항상 채워진다. `initial()`·`next()` 정적 팩토리를 갖는다.

`NewTerm`(`record`) — **저장 전 입력 모델.** 식별자가 없고, 만들어질 때 앞뒤 공백을 제거해 표기 비교가 일관되게 한다. `toTerm(dictionaryId, createdBy)`로 엔티티가 된다. **`R-12`의 포트 시그니처가 이 record와 짝을 맞춘다.**

### enum

```
DictionaryStatus { ACTIVE, ARCHIVED }   // 활성중 / 보관중
```

**`ARCHIVED`는 「삭제됨」이 아니라 「지나간 버전」이다.** 사전집은 삭제하지 않으며, 새 버전이 반영될 때 기존 `ACTIVE`가 이 상태로 내려간다.

상태 축이 이 둘뿐인 이유는 **미확정 상태가 존재하지 않기 때문**이다. 교정·리뷰 중인 것은 `DraftDictionary`·`RevisionDictionary`이고, 이 도메인에는 확정본만 들어온다.

---

## 4. 상태 전이와 도메인 메서드

### DictionaryStatus

| from | to | 트리거 | 조건 | 상태 |
| --- | --- | --- | --- | --- |
| (신규) | `ACTIVE` | `Dictionary.createFirst(...)` | 워크스페이스에 사전집이 없다 | as-built |
| (신규) | `ACTIVE` | `Dictionary.nextVersion(previous, ...)` | **이전 버전이 이미 `ARCHIVED`로 flush됐다** | as-built |
| `ACTIVE` | `ARCHIVED` | `archive()` | — | as-built |
| `ARCHIVED` | — | — | 종단. 되돌리지 않는다 | as-built |

`archive()`는 **이미 내려간 버전이면 아무 일도 하지 않는다**(멱등). 새 버전이 발행될 때 이벤트가 두 번 도착해도 상태가 어긋나지 않는다.

### 발행 순서 — 뒤집으면 유니크 제약에 걸린다

```
1. 활성 사전집을 읽는다 (없으면 3으로)
2. archive() + saveAndFlush          ← 여기서 flush하지 않으면 4의 INSERT가 먼저 나간다
3. Dictionary.nextVersion(previous)  (첫 버전이면 createFirst)
4. TermAppender.appendAll(nextId, terms)
5. TermReader.readAll(nextId)로 정렬된 목록을 읽어 응답을 만든다
```

2번의 명시적 flush가 필요한 이유는 **Hibernate가 한 트랜잭션의 쓰기를 모을 때 INSERT를 UPDATE보다 먼저 실행**하기 때문이다. 보관 전환을 미뤄 두면 새 버전 INSERT가 먼저 나가면서 활성 사전집이 순간 둘이 되어 `uk_dictionary_workspace_active`에 걸린다 — `DictionaryUpdater.archive` javadoc이 이 근거를 갖고 있다.

5번에서 저장 결과를 그대로 쓰지 않고 다시 읽는 이유는 **정렬 기준을 조회 쿼리 한 곳에만 두기 위해서**다(`TermAppender.appendAll` javadoc).

### 동시 발행

**따로 막지 않는다.** `G-14`(워크스페이스 단위 상호 배타)와 `DOMAIN.md` «초안 사전» 정책이 사전집당 진행 중인 등재 흐름을 1개로 제한하므로, 같은 버전을 기준으로 편집하는 주체가 둘이 될 수 없다. 검사가 필요해지면 그 자리는 **리뷰 승인 시점**이고 이 도메인 밖이다.

`D-27`로 임시 API가 열려 있는 동안은 이 전제가 성립하지 않지만, **활성 유일성을 DB가 지키므로 최악의 결과는 두 번째 요청의 실패**다(데이터 손상이 아니다).

### 도메인 메서드 시그니처

```java
// Dictionary
static Dictionary createFirst(Long workspaceId, Long createdBy);                 // as-built
static Dictionary nextVersion(Dictionary previous, Long createdBy);              // as-built
void archive();                                                                  // as-built (멱등)
boolean isActive();                                                              // as-built
int versionNo();                                                                 // as-built — 값 객체 위임
OffsetDateTime publishedAt();                                                    // 추가 — DocumentVersion 선례와 대칭

// DictionaryVersion (@Embeddable record)
static DictionaryVersion initial();                                              // as-built
DictionaryVersion next();                                                        // as-built

// Term
static Term create(Long dictionaryId, String preferredForm, String englishName,
        String definition, Long createdBy);                                      // as-built
static String normalize(String form);                                            // as-built

// NewTerm (record)
Term toTerm(Long dictionaryId, Long createdBy);                                   // as-built
```

`nextVersion`은 **인자로 받은 이전 버전을 건드리지 않는다** — 보관 처리는 호출자가 `archive()`로 따로 한다. 두 일을 한 메서드에 넣으면 위 발행 순서의 flush 지점을 표현할 수 없다.

`publishedAt()` 위임을 추가하는 이유는 **호출자가 `getVersion().publishedAt()`으로 체이닝하지 않게** 하기 위해서다 — `DocumentVersion`이 같은 이유로 두 위임을 갖고 있다.

---

## 5. 스키마와 Flyway

**대역: 300–399**(`backend/CLAUDE.md`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 | 상태 | 태스크 |
| --- | --- | --- | --- |
| `V300__create_dictionary_and_term.sql` | `dictionary`, `term` | as-built | — |
| `V310__add_term_deleted_at.sql` | `term.deleted_at` 추가 | **추가** | `DIC-2` |

```sql
-- V310
-- Term이 공통 BaseEntity 규약을 따르도록 삭제 시각 컬럼을 추가한다.
-- 현재 삭제 경로는 없으므로 기존 행과 신규 행의 기본값은 null이다.
alter table term
    add column deleted_at datetime(6);
```

**`V300`은 고치지 않는다.** `active_flag` 생성 컬럼 기법과 그 주석은 이 스키마의 핵심 근거이므로 원문을 보존한다.

```sql
-- V300에서 이어받는 활성 유일성 기법 (참고 — 재작성하지 않는다)
active_flag tinyint generated always as (case when status = 'ACTIVE' then 1 else null end) stored,
constraint uk_dictionary_workspace_active unique (workspace_id, active_flag)
```

`status`를 그대로 유니크에 넣으면 보관 행끼리 충돌한다. MySQL은 UNIQUE에서 NULL을 서로 다른 값으로 보므로 **보관 행은 NULL이라 몇 개든 쌓이고 활성 행은 1이라 두 개째가 막힌다.**

`term.deleted_at`은 공통 `BaseEntity` 규약을 위해 `V310`에서 추가하지만, 개별 삭제 유스케이스는 제공하지 않는다.

**검색·정렬(`DIC-6`)에 인덱스가 필요해지면 `V320`으로 추가한다.** `uk_term_dictionary_preferred_form`이 `(dictionary_id, preferred_form)`을 덮으므로 표준어 접두 검색은 그 인덱스를 탄다. 정의 본문 검색은 `text` 컬럼이라 별도 판단이 필요하다 — `DIC-6`에서 결정한다.

---

## 6. 패키지와 파일

```
com.ubidict.backend.dictionary
├── presentation
│   ├── DictionaryController                     as-built (POST /versions → INTERNALIZE 태그 DIC-1)
│   └── dto
│       ├── ReviseDictionaryRequest              as-built
│       ├── TermRequest                          as-built
│       ├── DictionaryResponse                   as-built (+ 페이징 DIC-6)
│       ├── DictionaryVersionResponse            as-built
│       └── TermResponse                         as-built
├── service
│   ├── DictionaryService                        as-built (+ 검색 DIC-6)
│   └── model
│       ├── ReviseDictionaryCommand              as-built
│       ├── TermCommand                          as-built
│       ├── DictionaryResult                     as-built
│       ├── DictionaryVersionResult              as-built
│       └── TermResult                           as-built
├── implement
│   ├── DictionaryReader                         as-built
│   ├── DictionaryAppender                       as-built
│   ├── DictionaryUpdater                        as-built (saveAndFlush 유지)
│   ├── TermReader                               as-built (+ 검색·정렬 DIC-6)
│   ├── TermAppender                             as-built
│   └── TermFormValidator                        as-built
├── infra
│   ├── DictionaryRepository                     as-built (+ 페이징 DIC-6)
│   ├── TermRepository                           as-built (+ 검색 DIC-6)
│   ├── TermSummary                              DIC-6   — 정의 본문을 뺀 프로젝션
│   └── adapter
│       ├── DocumentDictionaryQueryAdapter       DIC-5   — document의 DictionaryQueryPort 구현
│       ├── DraftDictionaryTermQueryAdapter      DIC-4   — 표준어·정의 스냅샷 제공
│       ├── DraftDocumentTermQueryAdapter        DIC-4   — 같은 스냅샷, 소비자만 다르다
│       └── DictionaryVersionPublishAdapter      DIC-3   — reviewrequest의 발행 위임 구현
├── domain
│   ├── Dictionary                               as-built (BaseEntity 유지: DIC-2)
│   ├── DictionaryVersion                        as-built
│   ├── DictionaryStatus                         as-built
│   ├── Term                                     as-built (감사 필드 → BaseEntity: DIC-2)
│   ├── NewTerm                                  as-built
│   ├── ~~DictionaryId~~                         제거 D-25
│   ├── ~~TermId~~                               제거 D-25
│   └── event
│       └── DictionaryRevisedEvent               DIC-8
└── exception
    ├── DictionaryErrorCode                      as-built
    └── TermErrorCode                            as-built
```

**`infra/port/`가 없다.** `R-12`로 발행 포트가 값 기반이 되면서 **이 도메인은 다른 도메인을 조회할 필요가 없어졌다** — 6개 도메인 중 소비 포트가 하나도 없는 유일한 도메인이다. `adapter/`만 있고 모두 **다른 도메인이 정의한 포트의 구현**이다.

**`DictionaryService`는 접근 검증에 `WorkspaceAccessValidator`를 계속 직접 주입한다**(`D-19`).

---

## 7. API 명세

접두사 `/api`(버저닝 없음). 컨트롤러는 **`ResponseEntity<T>`를 반환**하고 `@ResponseStatus`를 쓰지 않는다. **Swagger 애노테이션은 쓰지 않는다**(명세는 `docs/API.md`가 담당).

요청자 식별은 **`@RequestParam Long memberId`**다. 인증 계층(`NFR-USR-001`)이 없어 생긴 임시 방식이며 인증 도입 시 전부 사라진다. 컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 **"인증 도입 전까지 운영 배포 대상이 아니다"**를 적는다.

**경로가 단수 `/dictionary`인 이유** — 워크스페이스에 활성 1개 + 보관 N개로 존재하고 「이름」을 두지 않으므로, 리소스 컬렉션이 아니라 워크스페이스의 단일 속성처럼 읽힌다. 버전 목록은 그 하위에 둔다.

| 상태 | Method | Path | 요청 | 응답 | 성공 | 권한 | 태그 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| as-built | POST | `/api/workspaces/{workspaceId}/dictionary/versions` | `ReviseDictionaryRequest{terms[]}` | `DictionaryResponse` | `201` | **ADMIN 이상** | **INTERNALIZE** |
| as-built | GET | `/api/workspaces/{workspaceId}/dictionary` | `page?`, `size?`, `sort?`, `keyword?` | `DictionaryResponse` | `200` | 참여자 | KEEP |
| as-built | GET | `.../dictionary/versions` | `page?`, `size?` | `PageResponse<DictionaryVersionResponse>` | `200` | 참여자 | KEEP |
| as-built | GET | `.../dictionary/versions/{versionNo}` | `page?`, `size?` | `DictionaryResponse` | `200` | 참여자 | KEEP |

### 임시 API 태그

| 태그 | 뜻 |
| --- | --- |
| **KEEP** | 리뷰 도메인이 붙어도 그대로 남는다 |
| **INTERNALIZE** | **HTTP 노출을 걷어내고 서비스 메서드만 남긴다.** 리뷰 승인이 `DictionaryVersionPublishPort`로 호출하는 진입점이 되고, 엔드포인트는 사라진다 |

**`POST /versions`가 INTERNALIZE다**(`D-27`). `DOMAIN.md` «사전집»의 「새 버전은 리뷰 승인(Revise)의 반영으로만 생긴다」와 `NFR-UPD-001`(Human-in-the-Loop)에 어긋나므로 최종 형태가 아니다. 지금 유지하는 이유는 **이것이 사전집을 만드는 유일한 경로**여서, 잠그면 `DOC-1`(활성 버전 조회)·`DOC-3`(`aligned` 판정)·`DIC-5`를 검증할 수 없기 때문이다.

`docs/API.md`와 컨트롤러 javadoc에 **「리뷰 도메인 완성 시 제거 — `DIC-7`」**을 적는다. 코드에 이미 `TODO(REQ-REV-005)`가 있으므로 태스크 ID를 덧붙인다.

### 조회 응답의 페이징 (`DIC-6`)

사전집 조회는 **용어 목록을 담으므로** 페이징이 필요하다 — 용어가 수백 건까지 늘 수 있다.

- `GET /dictionary`와 `GET /dictionary/versions/{versionNo}`는 **`DictionaryResponse` 안의 `terms`를 `PageResponse`로 감싼다.** 사전집 메타(버전·확정일시·상태)는 페이징 밖에 있어야 하므로 응답 전체를 `PageResponse`로 만들지 않는다.
- `GET /dictionary/versions`(버전 이력)는 **응답 전체가 `PageResponse`**다.
- `sort` 화이트리스트는 `preferredForm`(기본, 오름차순)과 `createdAt`이다. 임의 필드 정렬을 허용하면 인덱스 없는 컬럼으로 전체 스캔이 난다.
- `keyword`는 `preferredForm`·`englishName` 접두 검색이다(`REQ-DIC-002`). `uk_term_dictionary_preferred_form`을 탄다.

### 권한

| 대상 | 규칙 |
| --- | --- |
| 발행(반영) | **ADMIN 이상.** `validateAtLeast(workspaceId, memberId, Permission.ADMIN)` — `G-2`·`DOMAIN.md` 「생성·반영은 Admin 이상」 |
| 조회 | 참여자면 누구나. `validateParticipant(workspaceId, memberId)` |
| 비참여자·삭제된 워크스페이스 | `404 WORKSPACE_NOT_FOUND` |

권한 부족에 dictionary 전용 코드를 두지 않고 `WorkspaceErrorCode`를 그대로 쓴다 — as-built 결정이며 `DictionaryErrorCode` javadoc이 근거를 갖고 있다.

**워크스페이스에 사전집이 없는 기간은 정상이다.** 첫 발행 전까지가 그 상태이며 오류 상황이 아니다 — 조회는 `404 DICTIONARY_NOT_FOUND`를 주지만 이는 「아직 없음」이고, 호출자가 그 경우를 다뤄야 하면 `readActiveOptional`을 쓴다.

---

## 8. ErrorCode

`dictionary/exception/DictionaryErrorCode`, `dictionary/exception/TermErrorCode`. 접두사 하나당 enum 하나다 — `EXCEPTION.md`의 `{DOMAIN}_{REASON}` 규칙 때문에 한 enum에 두 접두사를 섞으면 enum 이름과 코드가 어긋난다.

| 상수 | status | message | 상태 | 태스크 |
| --- | --- | --- | --- | --- |
| `DICTIONARY_NOT_FOUND` | 404 | 사전집을 찾을 수 없습니다. | as-built | — |
| `DICTIONARY_EMPTY_TERMS` | 400 | 용어가 없는 사전집 버전은 만들 수 없습니다. | as-built | — |
| `DICTIONARY_INVALID_VERSION` | 400 | 사전집 버전 정보가 올바르지 않습니다. | as-built | — |
| `TERM_INVALID_PREFERRED_FORM` | 400 | 표준어는 1자 이상 100자 이하여야 합니다. | as-built | — |
| `TERM_INVALID_ENGLISH_NAME` | 400 | 영문명은 100자 이하여야 합니다. | as-built | — |
| `TERM_INVALID_DEFINITION` | 400 | 정의는 비어 있을 수 없습니다. | as-built | — |
| `TERM_DUPLICATE_PREFERRED_FORM` | 409 | 같은 표준어를 두 번 등재할 수 없습니다. | as-built | — |

**추가할 코드가 없다.** 큰 흐름이 이 도메인에 준 변경(통합 발행·포트 시그니처·감사 상위 클래스)은 모두 새 실패 경로를 만들지 않는다.

`DICTIONARY_NOT_FOUND`가 「아직 없음」과 「접근 불가」를 겸하는 것은 의도다 — 워크스페이스에 사전집이 없는 기간이 정상 상태이고, 비참여자에게는 그보다 앞서 `WORKSPACE_NOT_FOUND`가 나간다.

---

## 9. 크로스 도메인 계약

### 접근 검증 — 포트를 만들지 않는다 (`D-19`)

**`WorkspaceAccessValidator`를 직접 주입한다.** `ARCHITECTURE.md` «크로스 도메인 조회 — 포트와 어댑터»의 명문 예외이며 `T-DOC-1`이 그 조항을 추가한다. as-built가 이미 이 방식이므로 **`DictionaryService`의 주입부를 고치지 않는다** — javadoc의 "워크스페이스 접근 검증은 workspace 도메인의 `WorkspaceAccessValidator` 하나만 참조한다. 리포지토리나 다른 implement를 직접 건드리지 않는다"가 그대로 유효하다.

### 소비 포트

**없다.** `R-12`로 발행 포트가 값 기반이 되면서 이 도메인이 다른 도메인을 조회할 필요가 사라졌다 — 6개 도메인 중 유일하다.

이것이 `R-12`의 핵심 이득이다. 기존 시그니처(`List<Long> approvedCandidateTermIds`)를 유지하면 **이 도메인이 `draftdictionary`를 향하는 소비 포트를 정의해야 하고**, 사전집이 초안의 결과물인데 결과물이 원인을 조회하는 모양이 된다.

### 제공 포트 (다른 도메인이 우리를 볼 때 — 우리가 어댑터를 구현한다)

| 소비자가 정의한 포트 | 시그니처 | 근거 메서드 | 태스크 |
| --- | --- | --- | --- |
| `document/infra/port/DictionaryQueryPort` | `Optional<Integer> activeVersionNo(Long workspaceId)` | `DictionaryRepository.findByWorkspaceIdAndStatus(workspaceId, ACTIVE)` | `DIC-5` — **`DOC-1`이 이미 구현했다**(아래) |
| `draftdictionary/infra/port/DictionaryTermQueryPort` | `List<TermSnapshot> readActiveTerms(Long workspaceId)`, `Optional<Integer> activeVersionNo(Long workspaceId)` | 위 + `TermRepository.findAllByDictionaryIdOrderByPreferredFormAsc` | `DIC-4` |
| `draftdocument/infra/port/DictionaryTermQueryPort` | `List<TermSnapshot> readActiveTerms(Long workspaceId)` | 같은 조회 | `DIC-4` |
| `reviewrequest/infra/port/DictionaryVersionPublishPort` | `int publish(Long workspaceId, int baseVersionNo, List<NewTermSnapshot> terms)` → 새 `versionNo` | `DictionaryService.revise`와 같은 흐름 | `DIC-3` |

스냅샷 record — `TermSnapshot(Long termId, String preferredForm, String englishName, String definition)`, `NewTermSnapshot(String preferredForm, String englishName, String definition)`. **엔티티를 포트 시그니처에 노출하지 않는다.**

> **`DIC-5`는 `DOC-1`이 이미 끝냈다.** `document/infra/adapter/DictionaryQueryAdapter`와 그 스텁이 그 산출물이다. `DOCUMENT_PLAN.md` 6절이 같은 어댑터를 `DOC-1`의 산출물(「포트 1 + 어댑터 1 + 스텁 1」)로 적어 두어 **두 문서가 한 파일을 각자 자기 태스크로 청구한 상태**였다. `D-33`으로 조회 어댑터의 주인이 소비 도메인으로 확정됐으므로 **`DOC-1`의 배치가 맞고 `DIC-5`는 별도 산출물을 갖지 않는다.**

**포트 시그니처는 소비자가 정의하고 우리는 구현만 한다.** 시그니처가 위와 다르면 소비 도메인의 정의를 따른다.

> **`readActiveTerms`가 `O-3`을 해소한다.** `DRAFT_DOCUMENT_PLAN.md` 13절이 "`suggestionTerm` 값의 출처 — Dictionary 도메인이 생기면 `DictionaryQueryPort`로 승격 제안"으로 남겨 둔 항목이다. 이미 생겼으므로 `DIC-4`가 제공한다. **`G-1`의 통합 입력도 같은 조회를 쓴다** — 초안이 「이전 사전집 + 추출 용어」를 합칠 때 앞쪽이 이것이다.

> **`publish`가 `workspaceId`를 받는다**(`dictionaryId`가 아니다). 발행은 「이 워크스페이스의 다음 버전」을 만드는 것이고, 활성 사전집을 찾는 것은 이 도메인의 책임이다. 첫 버전이면 활성 사전집이 없으므로 `dictionaryId`를 받을 수가 없다 — `DictionaryService.appendNextVersion`이 이미 두 경우를 하나의 경로로 다룬다.

> **`baseVersionNo`는 낙관적 검증용이다.** 넘어온 값이 현재 활성 버전과 다르면 그 사이에 다른 반영이 있었다는 뜻이므로 `409`로 거절한다. `G-14`가 이 경합을 막지만 `D-27`의 임시 API가 열려 있는 동안은 성립하지 않으므로 방어를 둔다.

### 어댑터 배치

**포트는 소비자가 정의한다.** 어댑터를 어디에 두는지는 **포트의 종류로 갈린다**(`D-33`, 2026-09-12 확정).

| 포트 종류 | 어댑터 위치 | 이 도메인에 해당하는 것 |
| --- | --- | --- |
| **조회 포트** | **소비 도메인** `{소비}/infra/adapter/` | `DIC-4`·`DIC-5` — `draftdictionary`·`draftdocument`·`document`가 각자 자기 패키지에 두고 우리 `infra`(Repository)만 참조한다 |
| **발행 위임 포트** | **제공 도메인** `dictionary/infra/adapter/` | `DIC-3` — 발행은 우리 도메인 로직이라 어댑터가 `DictionaryService`·`implement`를 써야 한다 |

> **이 절의 이전 판은 「어댑터는 `dictionary/infra/adapter/`에 두고 스텁은 소비 도메인이 갖는다」였다.** `ARCHITECTURE.md` «크로스 도메인 **조회** — 포트와 어댑터»가 "어댑터도 소비 도메인이 구현한다"로 못박고 있어 **조회에 대해서는 틀린 서술이었고**, 실제로 `DIC-4`가 그 서술을 따라 제공 측에 놓이면서 `DOC-1`의 소비 측 배치와 어긋났다(`X-21`). `D-33`으로 정정한다.

**스텁도 소비 도메인이 갖는다.** 조회 포트는 어댑터와 스텁이 같은 패키지에 짝으로 있고, 발행 위임 포트는 `RR-4b`가 스텁을, 우리가 real을 만든다.

어댑터 선택은 소비 도메인의 프로퍼티가 결정한다 — 조회는 `app.crossdomain.dictionary.mode`, 발행 위임은 `app.crossdomain.dictionary-publish.mode`다. 두 키 모두 **선행 PR이 `application.yml`에 미리 선언해 둔다.**

### 발행 이벤트

| 이벤트 | 페이로드 | 수신자 | 태스크 |
| --- | --- | --- | --- |
| `DictionaryRevisedEvent` | `(Long workspaceId, Long dictionaryId, int versionNo, OffsetDateTime occurredAt)` | Notification(미정) | `DIC-8` |

**불변 record, 식별자·원시값·시각만 담는다.** 용어 목록을 담지 않는다 — 수백 건이 될 수 있고, `Term`은 엔티티다. 상태가 필요한 컨슈머는 식별자로 다시 조회한다.

`implement`는 `common/infra/event/EventPublisher` 포트만 주입받는다.

**문서 도메인은 이 이벤트를 구독하지 않는다.** `aligned`는 파생 판정이라 사전집이 올라가면 저장 없이 자동으로 거짓이 된다 — 핸들러를 만들면 같은 규칙이 두 벌이 된다(`DOCUMENT_PLAN.md` 9절).

### 수신 이벤트

**없다.** 이 도메인은 발행 위임을 포트로 받고(`DIC-3`) 상태를 스스로 바꾼다. 다른 도메인의 이벤트를 기다리는 지점이 없다.

`WorkspaceDeletedEvent`도 처리하지 않는다 — 모든 조회가 `WorkspaceAccessValidator`에서 먼저 막히고, **사전집은 삭제하지 않는다**는 정책이 워크스페이스 삭제보다 우선한다(버전 이력은 보존한다).

---

## 10. 공유 파일 규약

### 공유 자산과 주인

| 자산 | 주인 | 규약 |
| --- | --- | --- |
| `common/presentation/PageResponse`, `common/service/PageResult` | **`T-CMN-1`(선행 공통 태스크)** | 규격은 `docs/API.md` «페이징·정렬 규격»을 그대로 따른다. `DIC-6`은 만들지 않고 **쓴다** |
| `common/domain/TextRange` | DraftDocument (`DD-2`) | 이 도메인은 쓰지 않는다 |
| `docs/API.md` Dictionary 절 | **이 도메인** | **파일 끝에 자기 `# **Dictionary API**` 절을 신설한다**(`Y-04`). Workspace 절의 골격(도입 문단 → 요약 표 → 엔드포인트 절 → 에러 표)을 따른다. 공통 규칙·페이징·버저닝·에러 응답 형식 절은 건드리지 않는다 |

### 수정 금지 파일

`common/**`(위 2건 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`, 그리고 **다른 도메인의 패키지 전체**.

> **`V300`은 고치지 않는다.** `active_flag` 생성 컬럼과 그 주석이 이 스키마의 핵심 근거다. 변경은 `V310` 이후로 쌓는다.

> **`docs/DOMAIN.md`·`REQUIREMENTS.md`·`UBIQUITOUS_LANGUAGE.md`는 `T-DOC-1`만 고친다.** 이 도메인의 `R-11`·`R-12`·`R-24`가 그 파일들을 향하지만 직접 고치지 않는다.

### git worktree 운영

| 항목 | 규칙 | 이유 |
| --- | --- | --- |
| 브랜치명 | `feat/WLSH-{티켓번호}-dictionary` | `.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다 |
| `bootRun` 동시 실행 금지 | 한 번에 한 worktree만 | Compose 프로젝트가 worktree마다 떠 호스트 포트가 겹친다 |
| `./gradlew test` 동시 실행 주의 | 여러 worktree에서 동시에 돌리지 않는다 | Testcontainers + LGTM이 Docker 기본 메모리에서 OOM |
| develop 동기화 | `T-DOC-1`·`T-CMN-1` 머지 직후 **즉시** | 문서 기준과 `PageResponse`가 그때 바뀐다 |
| 파일 소유권 | 위 표를 따른다 | 선착순은 곧 중복 생성이다 |

---

## 11. 테스트 계획

| 계층 | 방식 |
| --- | --- |
| Unit(domain) | 순수 JUnit, Spring 없음. `@Nested`로 케이스를 묶는다 |
| Repository(infra) | `RepositoryTestSupport` 상속 — `@DataJpaTest` + 실제 MySQL 컨테이너 + **Flyway가 만든 스키마** |
| Service | `IntegrationTestSupport` 상속 — `@SpringBootTest(webEnvironment = NONE)` + `DbCleaner` |
| Controller(presentation) | `@WebMvcTest(DictionaryController.class)` + `@MockitoBean` + **RestAssuredMockMvc** |

> **개별 테스트에 `@Transactional`을 붙이지 않는다.** 서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써 경계 자체를 검증하지 못하게 된다. **이 도메인에서는 특히 중요하다** — `DictionaryUpdater.archive`의 `saveAndFlush`가 트랜잭션 경계 안에서 순서를 만드는 것이 핵심 동작이다. `docs/TEST.md`가 롤백을 권장하는 것은 낡은 서술이며 `T-DOC-1`이 정정한다(`D-32`).

> **테스트 전용 엔티티를 만들지 않는다.** 스키마는 Flyway가 만들고 `DbCleaner`가 `information_schema`에서 실제 테이블을 읽어 비운다.

AssertJ를 쓴다(`assertThat`·`assertThatThrownBy`·`extracting`). `@DisplayName`은 한국어 완결 문장 + 마침표, 메서드명은 `{메서드명}_{조건}` camelCase다.

### 테스트 클래스와 필수 케이스

| 클래스 | 계층 | 필수 케이스 | 태스크 |
| --- | --- | --- | --- |
| `domain/DictionaryTest` | Unit | as-built + **`archive_isIdempotent`("이미 보관된 사전집을 다시 보관해도 상태가 바뀌지 않는다.")** / `nextVersion_doesNotTouchPrevious` / `publishedAt_delegatesToVersion` | `DIC-2` |
| `domain/DictionaryVersionTest` | Unit | as-built(`initial`·`next`·검증) | — |
| `domain/TermTest` | Unit | as-built(정규화·`blankToNull`·검증 3건) | — |
| `implement/TermFormValidatorTest` | Unit | as-built + `validateUnique_blankIsSkipped` | — |
| `infra/DictionaryRepositoryTest` | Repository | as-built + **`save_activeDictionaryIsDuplicated`(유지 — `active_flag` 회귀 방지)** / **`findAllVersions_paging`** | `DIC-2`·`DIC-6` |
| `infra/TermRepositoryTest` | Repository | as-built + **`findByKeyword_prefixMatch`** / `findByKeyword_matchesEnglishName` | `DIC-6` |
| `infra/adapter/DictionaryVersionPublishAdapterTest` | Repository | **`publish_firstVersion`** / **`publish_nextVersionArchivesPrevious`("새 버전을 발행하면 이전 활성 사전집이 보관 상태로 내려간다.")** / **`publish_baseVersionNoMismatch`(409)** / `publish_emptyTerms`(400) | `DIC-3` |
| `infra/adapter/DraftDictionaryTermQueryAdapterTest` | Repository | **`readActiveTerms_returnsActiveVersionOnly`("보관 버전의 용어는 읽히지 않는다.")** / `readActiveTerms_dictionaryIsAbsent` | `DIC-4` |
| `infra/adapter/DocumentDictionaryQueryAdapterTest` | Repository | `activeVersionNo_returnsActiveOnly` / `activeVersionNo_dictionaryIsAbsent` | `DIC-5` |
| `service/DictionaryServiceTest` | Service | as-built + **`revise_archivesPreviousBeforeInsert`("이전 버전을 먼저 내리지 않으면 활성 사전집이 둘이 되어 실패한다.")** / **`readActive_searchByKeyword`** / `readActive_termsArePaged` / 접근 검증 3건(`_memberIsNotParticipant` 404 / `_permissionIsBelowAdmin` 403 / `_workspaceIsDeleted` 404) | `DIC-3`·`DIC-6` |
| `presentation/DictionaryControllerTest` | Controller | as-built + **`readActive_termsArePaged`** / `readActive_sortIsNotWhitelisted`(400) | `DIC-6` |

**`save_activeDictionaryIsDuplicated`와 `revise_archivesPreviousBeforeInsert`가 이 도메인의 회귀 방지 핵심이다.** 전자는 `active_flag` 생성 컬럼이 살아 있는지를, 후자는 `saveAndFlush`가 남아 있는지를 확인한다. 둘 중 하나가 사라지면 활성 사전집이 둘이 된다.

### Fixture

`dictionary/fixture/`에 둔다(src/test 전용). 정적 팩토리 + 내부 `static class XxxBuilder`, 기본값 보유, 체이닝, `build()`는 항상 유효한 객체를 돌려준다. 운영 경로에 없는 상태는 `ReflectionTestUtils.setField`로 주입한다.

모든 필드를 받는 거대 fixture나 `create(id, ...)` 형태는 만들지 않는다.

| Fixture | 변경 |
| --- | --- |
| `DictionaryFixture` | as-built. `versionNo`·`status(ARCHIVED)` 주입이 이미 있다 |
| `TermFixture` | as-built. **`NewTermSnapshot` 목록을 만드는 정적 헬퍼 추가**(`DIC-3` 어댑터 테스트용) |

---

## 12. Phase와 태스크

| Phase | 목표 | 명시적 제외 |
| --- | --- | --- |
| 1 | **문서화와 감사 엔티티 정합** — `API.md` 절 신설, `BaseEntity` 통일 | 발행 경로 변경(리뷰 도메인이 없다) |
| 2 | 다른 도메인이 우리를 보는 창구 — 제공 어댑터 3종 | — |
| | └ **`DIC-4` 완료**(`WLSH-114`), **`DIC-5`는 `DOC-1`이 흡수**, **`DIC-3`만 잔여** — `RR-4b` 대기였고 선행 PR이 포트를 만들어 풀린다. **Phase 2 PR을 새로 열지 않고 `dictionary-phase-3`에 얹는다** | |
| 3 | 조회·검색·정렬 + 페이징 | 버전 간 비교(`REQ-DIC-007`, MVP2) |
| 4 | 임시 API 제거 + 이벤트·로그 | — |

| ID | 태스크 | 산출물 | 일수 | 의존 |
| --- | --- | --- | --- | --- |
| **DIC-1** | `docs/API.md`에 Dictionary 절 신설 + `INTERNALIZE` 태그 | `API.md` 절 1 + 컨트롤러 javadoc 수정 | 0.5 | **`T-DOC-1`** |
| **DIC-2** | 감사 상위 클래스 정합 + `V310` | `Dictionary`·`Term` 수정 + `V310` + 테스트 3 | 1 | — |
| **DIC-3** | 발행 위임 어댑터 | 어댑터 1 + `NewTermSnapshot` + 테스트 4 | 1 | `DIC-2`, **`RR-4b`**(포트 정의) |
| **DIC-4** | 표준어·정의 스냅샷 조회 어댑터 | 어댑터 2 + `TermSnapshot` + 테스트 2 | 1 | `DIC-2` |
| ~~**DIC-5**~~ | ~~활성 버전 조회 어댑터~~ | **`DOC-1`이 구현 완료**(9절) — 별도 산출물 없음 | — | — |
| **DIC-6** | 조회·검색·정렬 + 페이징 | Repository 2 + `TermSummary` + `TermReader` 수정 + result·response 수정 + 테스트 5 | 2 | **`T-CMN-1`**, `DIC-2` |
| **DIC-7** | 임시 API 제거 | 컨트롤러 엔드포인트 1 삭제 + dto 2 삭제 + 테스트 수정 | 0.5 | **`RR-4b`**, `DIC-3` |
| **DIC-8** | 이벤트 발행 + 감사 로그 | 이벤트 record 1 + 서비스 수정 + 로그 + 테스트 1 | 1 | `DIC-3` |
| **DIC-9** | ID 값 객체 제거 | record 2 삭제 + 테스트 2 삭제 | 0.5 | — |

**`DIC-1`이 `T-DOC-1`에 의존하는 이유** — `API.md`의 공통 규칙 절(`X-09`의 `INVALID_INPUT` 문구)이 먼저 정리돼야 새 절의 에러 표가 그것과 어긋나지 않는다.

**`DIC-2`를 가장 먼저 하는 이유** — `BaseEntity` 통일이 엔티티 시그니처를 바꾸므로, 어댑터 3종(`DIC-3`~`DIC-5`)보다 앞서야 두 번 고치지 않는다.

**`DIC-3`이 기다린 것은 `RR-4b`의 구현이 아니라 인터페이스 파일 하나다**(2026-09-12 정정).

- `DIC-3`은 **`reviewrequest/infra/port/DictionaryVersionPublishPort`가 존재해야** 어댑터를 구현할 수 있다. 원래는 `RR-4b`가 포트와 스텁을 함께 만들어 자기 Phase를 완결하는 구조였으나(`ARCHITECTURE.md` 「제공 도메인이 아직 없으면 스텁을 함께 만든다」), **선행 PR(`chore/WLSH-145-contracts`)이 그 포트와 스텁을 먼저 만든다.** 그래서 `DIC-3`과 `RR-4b`가 **서로를 기다리지 않고 동시에** 진행된다 — `DIC-3`은 real 어댑터를, `RR-4b`는 그 포트를 주입받는 `ReviseProcessor`를 각자 만든다.
- `DIC-7`은 그보다 더 뒤다. 임시 API를 그 전에 제거하면 사전집을 만들 방법이 없어진다(`D-27`). **Phase 4지만 6개 도메인이 모두 머지된 뒤 마무리 PR에서 한다.**

### Phase별 DoD

**공통 (모든 Phase)**

- [ ] `./gradlew spotlessApply && ./gradlew check` 초록
- [ ] 새 로직에 테스트를 함께 작성했다. 실패하는 테스트를 삭제하거나 조건을 완화하지 않았다
- [ ] `docs/API.md`의 Dictionary 절만 갱신했다(공통 규칙 절은 건드리지 않았다)
- [ ] 엔티티를 result·response·이벤트·포트 시그니처에 담지 않았다
- [ ] `implement`에 `@Transactional`을 선언하지 않았다(`DictionaryUpdater.saveAndFlush`는 트랜잭션 선언이 아니다)

**DIC-1**

- [ ] Workspace 절 골격(도입 문단 → 요약 표 → 엔드포인트 절 → 에러 표)을 따랐다
- [ ] 경로가 단수 `/dictionary`인 근거를 적었다
- [ ] `POST /versions`에 **INTERNALIZE 태그와 「리뷰 도메인 완성 시 제거 — `DIC-7`」**을 적었다
- [ ] 컨트롤러의 `TODO(REQ-REV-005)`에 태스크 ID를 덧붙였다
- [ ] 사전집이 없는 기간이 정상 상태임을 에러 표에 설명했다

**DIC-2**

- [ ] `Dictionary`가 `BaseEntity`를 상속하고 기존 `deleted_at`을 유지한다
- [ ] `Term`이 `BaseEntity`를 상속하고 `@CreationTimestamp`/`@UpdateTimestamp` 직접 선언이 사라졌다
- [ ] `V310`이 `term.deleted_at`을 추가한다
- [ ] `DictionaryRepository`의 조회 조건에 `deletedAt`이 없다(as-built 유지)
- [ ] **`save_activeDictionaryIsDuplicated`가 여전히 통과한다** — `active_flag`를 건드리지 않았다

**DIC-3**

- [ ] 포트 시그니처가 **`List<NewTermSnapshot>`을 받는다**(`R-12`). `List<Long> approvedCandidateTermIds`가 아니다
- [ ] `workspaceId`를 받아 첫 버전과 다음 버전을 한 경로로 다룬다
- [ ] `baseVersionNo`가 현재 활성 버전과 다르면 `409`다
- [ ] **이전 버전을 flush로 먼저 내린다** — `revise_archivesPreviousBeforeInsert`가 통과한다
- [ ] `dictionary`가 `draftdictionary`·`reviewrequest`를 참조하지 않는다 — **소비 포트가 0개다**

**DIC-4**

- [ ] `TermSnapshot`에 엔티티가 없다
- [ ] 보관 버전의 용어가 읽히지 않는다
- [ ] 사전집이 없으면 빈 목록을 돌려준다(예외를 던지지 않는다)
- [ ] `O-3`(`suggestionTerm` 값의 출처)이 이 포트로 해소됐음을 `CONFLICTS.md`에 기록했다

**~~DIC-5~~ — `DOC-1`이 충족했다.** 아래 두 항목은 `DictionaryQueryAdapterTest`가 이미 검증한다.

- [x] `Optional<Integer>`를 반환해 「사전집 없음」과 「버전 0」이 구분된다
- [x] 보관 사전집이 활성으로 읽히지 않는다

**DIC-6**

- [ ] `T-CMN-1`의 `PageResponse`·`PageResult`를 **쓴다**(만들지 않는다)
- [ ] 사전집 메타(버전·확정일시·상태)가 페이징 밖에 있다
- [ ] `sort` 화이트리스트가 `preferredForm`·`createdAt`이고 그 밖이면 `400`이다
- [ ] service가 Spring `Page`를 반환하지 않는다
- [ ] `TermSummary`가 `definition` 본문을 빼고 읽는다(목록에 `text` 컬럼을 N배로 싣지 않는다)

**DIC-7**

- [ ] `POST /dictionary/versions` 엔드포인트와 `ReviseDictionaryRequest`·`TermRequest`가 사라졌다
- [ ] `DictionaryService.revise`는 남아 `DIC-3`의 어댑터가 호출한다
- [ ] `docs/API.md`의 해당 엔드포인트 절이 삭제됐다
- [ ] 컨트롤러 테스트의 `revise` 케이스가 어댑터 테스트로 옮겨졌다

**DIC-8**

- [ ] 이벤트가 식별자·원시값·시각만 담은 불변 record다. **용어 목록을 담지 않았다**
- [ ] `implement`가 `EventPublisher` 포트만 주입받는다
- [ ] 로그가 `[클래스명.메서드명] 영문 문장 key={}` 형식이고 발행(INFO)·권한 부족(WARN)을 남긴다
- [ ] 용어 원문·정의를 로그에 남기지 않는다

**DIC-9**

- [ ] `DictionaryId`·`TermId`와 두 테스트가 사라졌다
- [ ] 다른 도메인이 이 record들을 참조하지 않는다

### 통합 태스크 (6개 도메인 공통)

| ID | 내용 | 의존 |
| --- | --- | --- |
| `T-DOC-1` | **문서 선행 수정** — `CONFLICTS.md` 9절이 파일별 목록을 갖는다 | — (**모든 구현의 선행**) |
| `T-CMN-1` | `PageResponse`·`PageResult` 신설 | `T-DOC-1` |
| ~~`T-INT-1`~~ | ~~`spring.flyway.out-of-order=true` 정리~~ | **폐기** — 개발 브랜치 DB를 항상 리셋하므로 필요가 없다 |
| `T-INT-2` | 크로스 도메인 어댑터를 `real`로 전환 | **조회 축은 선행 PR에서 끝난다.** 발행 축(`dictionary-publish`)만 `DIC-3` 뒤 마무리 PR |
| `T-INT-3` | `SecurityConfig` + 인증 주체 + 프로파일 분리 | 인증 도메인(별건). `SecurityConfig`는 이미 있다 — 남은 것은 프로파일 분리와 `memberId` 파라미터 제거 |

**`T-INT-4`는 신설하지 않는다** — `D-26`으로 유래 리비전을 두지 않기로 해 필요가 사라졌다.

---

## 13. 리스크와 열린 질문

| 항목 | 내용 | 대응 |
| --- | --- | --- |
| **활성 유일성이 깨질 위험** | `active_flag` 생성 컬럼과 `DictionaryUpdater.saveAndFlush` 둘 중 하나가 사라지면 활성 사전집이 둘이 된다. 둘 다 「왜」가 주석에만 있고 코드로는 평범해 보인다 | 회귀 테스트 두 개(`save_activeDictionaryIsDuplicated`·`revise_archivesPreviousBeforeInsert`)를 11절 필수 케이스로 못박았다. `DIC-2` DoD에 「`active_flag`를 건드리지 않았다」를 넣었다 |
| **`D-27` 임시 API가 정책을 어기며 살아 있다** | `DOMAIN.md`·`NFR-UPD-001`에 어긋나는 경로가 리뷰 도메인 완성까지 열려 있다. 누구나 남의 `memberId`로 호출할 수 있는 상태(`API.md` «요청자 식별 — 임시 방식»)와 겹치면 사전집을 임의로 갈아치울 수 있다 | **인증 전까지 운영 배포 대상이 아니다**가 이미 프로젝트 전제다. `INTERNALIZE` 태그와 `DIC-7` 태스크로 제거 시점을 못박고, `baseVersionNo` 검증으로 의도치 않은 덮어쓰기를 막는다 |
| **통합 모델이 발행 목록을 크게 만든다** | `G-1`로 초안이 차기 버전 전체를 들므로 발행 시 넘어오는 `NewTermSnapshot`이 수백 건이 된다. 한 트랜잭션에서 `saveAll`한다 | `TermAppender.appendAll`이 이미 `saveAll` 한 번이다. 배치 크기가 문제가 되면 `hibernate.jdbc.batch_size`로 대응한다 — **코드 구조는 바꾸지 않는다** |
| **버전마다 용어가 복제되어 행이 선형 증가한다** | 용어 300개 × 버전 20개 = 6,000행. 의도된 설계(각 버전의 스냅샷)지만 무한히 늘어난다 | MVP1 규모에서는 문제가 아니다. `uk_term_dictionary_preferred_form`이 `(dictionary_id, preferred_form)`이라 조회는 항상 한 버전으로 좁혀진다 |
| **정의 본문 검색 수단이 없다** | `definition`이 `text`라 `keyword` 검색이 표준어·영문명에 한정된다(`REQ-DIC-002`) | `DIC-6`에서 표준어·영문명 접두 검색까지만 한다. 정의 검색이 필요해지면 전문 검색 인덱스를 별건으로 검토한다 |
| **`Term`의 감사 메커니즘이 바뀐다** | 직접 선언한 Hibernate 감사 애노테이션 대신 `BaseEntity`의 Spring Data Auditing을 쓴다 | `JpaAuditingConfig`의 UTC + 마이크로초 절삭 provider를 타는지 **`DIC-2` Repository 테스트로 저장 전후 값을 확인한다** |

### 열린 질문

- **`publish`가 `baseVersionNo` 불일치를 `409`로 줄 때 어느 ErrorCode를 쓸지** — `DICTIONARY_INVALID_VERSION`(400)은 「값이 잘못됨」이고 이 경우는 「그 사이에 다른 반영이 있었음」이라 의미가 다르다. `DIC-3`에서 `DICTIONARY_VERSION_CONFLICT`(409) 신설을 검토한다. **8절 표에 미리 넣지 않은 이유는 `G-14`가 이 경합을 막아 실제로 발생하지 않을 수 있기 때문**이다.
- **`DIC-6`의 `keyword` 검색을 활성 사전집에만 열지, 특정 버전에도 열지** — `GET /dictionary/versions/{versionNo}`에도 같은 파라미터를 주는 것이 대칭적이지만, 지나간 버전을 검색할 유스케이스가 `REQ-DIC-002`에 없다. **양쪽에 열되 기본 정렬만 공유**하는 안을 `DIC-6`에서 정한다.
- **`TermSummary`가 `definition`을 완전히 빼는지 앞부분만 남기는지** — 목록에서 정의 미리보기를 보여주려면 앞 N자가 필요하다. `DocumentVersionSummary`가 본문을 완전히 뺀 선례를 따라 **일단 완전히 빼고**, 화면 요구가 생기면 별도 필드로 추가한다.

### DOMAIN.md 수정 (`T-DOC-1`이 반영)

1. `Dictionary` 표의 감사 필드 설명에 **`BaseEntity` 상속 유지**를 반영(`Y-07`)
2. `DictionaryVersion` 각주의 「유래 리비전은 지금 두지 않는다 — 리뷰 도메인 작업에서 되살릴 후보」를 **「두지 않는다(`D-26`)」로 확정**
3. `Term` 표의 감사 필드 설명에 `BaseEntity` 상속 반영(`Y-08`)
4. 속성 표의 `DictionaryId`·`TermId`에 **「개념 표기」 각주**(`D-25`)
5. «사전집» 정책의 「생성·반영은 Admin 이상」에 **발행이 리뷰 승인의 위임임을 명시**(`G-3`)

### REQUIREMENTS.md 수정 (`T-DOC-1`이 반영)

1. `REQ-DIC-003`에서 **동의어·비권장어 표시를 내린다**(`R-24`·`D-28`). 최근 변경 이력은 `REQ-DIC-007`로 이관
2. `REQ-DIC-004` 비고에 **「구현 자리는 `draftdictionary`의 초안 교정」**을 명시(`R-11`) — 폐기가 아니다
3. `REQ-DIC-005` 비고의 「반영 진입점까지 구현. 리뷰 승인이 호출하도록 옮기는 것은 리뷰 도메인 작업」에 **`DIC-3`·`DIC-7` 태스크 ID를 덧붙인다**

### 리뷰 체크리스트 대조

| 출처 | 항목 | 이 문서 반영 위치 |
| --- | --- | --- |
| `ARCHITECTURE.md` | service가 Repository·기술 객체를 직접 참조하지 않는가 / 레이어 건너뛰기 / 도메인 간 직접 참조 / 이벤트 페이로드 | 9절, 12절 DoD |
| `TEST.md` | 계층별 방식 / Fixture / AssertJ / `@Transactional` 금지 | 11절 |
| `LOG.md` | prefix 규약 / 민감 정보 / 레벨 | 12절 `DIC-8` DoD |
| `API.md` | 페이징·정렬 / 에러 형식 / 요청자 식별 / 404 정책 | 7절, 12절 `DIC-1`·`DIC-6` DoD |
| `EXCEPTION.md` | `{DOMAIN}_{REASON}` / 중복 코드 금지 / 던지는 위치 | 8절 |
