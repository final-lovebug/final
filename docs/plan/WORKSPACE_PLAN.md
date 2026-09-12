# Workspace 도메인 구현 계획

워크스페이스(Workspace) 도메인의 구현 계획이다. `docs/DOMAIN.md`·`docs/ARCHITECTURE.md`·`docs/TEST.md`·`docs/API.md`·`docs/LOG.md`·`docs/EXCEPTION.md`를 먼저 읽고, 이 문서는 그 규칙을 이 도메인에 적용한 결과로 읽는다.

여섯 도메인 계획 문서는 같은 목차를 쓴다 — `DOCUMENT_PLAN.md`, `DICTIONARY_PLAN.md`, `DRAFT_DOCUMENT_PLAN.md`, `DRAFT_DICTIONARY_PLAN.md`, `REVIEW_REQUEST_PLAN.md`.

**이 도메인은 절반 이상이 미구현이다.** 워크스페이스 CRUD와 Owner 자동 등록까지가 as-built이고 **참여자 관리·초대·룰셋 수정이 전부 없다.** 3·5·6·7절의 표에 `상태` 칸을 두어 구분한다.

**`docs/plan/CONFLICTS.md`를 함께 읽는다.** 큰 흐름 확정(`G-*`)·이번 세션 확정(`D-19`~`D-32`)·뒤집힌 결정(`R-*`)이 거기에 있고, 이 문서는 ID로 참조한다.

**여러 세션으로 나눠 구현한다면 `EXECUTION_ORDER.md`를 먼저 읽는다.**

---

## 1. 범위와 목표

**담당 애그리게이트**: `Workspace`(루트, `RuleSet` 값 객체 포함), `Participant`, `Invitation`

**목표**: 사전집과 문서를 공유하는 협업 단위를 만들고, **참여자와 그 권한을 관리하며 리뷰 규칙(정족수)을 정한다.** 6개 도메인 전체의 **접근 검증 단일 지점**을 제공한다.

**관련 요구사항**: `REQ-WS-001`(생성 — 완료), `REQ-WS-002`(목록·상세 — 완료), `REQ-WS-003`(멤버 초대 — **대기**), `REQ-WS-004`(멤버 역할 관리 — **진행중**), `REQ-WS-005`(멤버 목록·제외 — **대기**), `REQ-WS-006`(설정 — 완료), `REQ-WS-007`(삭제 — 완료), `NFR-WS-001`(데이터 격리 — 진행중), `NFR-USR-006`(API 인가 규칙)

**이 도메인이 6개 전체의 크리티컬 패스인 이유**

`G-4`(승인 집계는 룰셋 정족수 기준)가 확정되면서 **`WS-4`(룰셋 수정)가 리뷰 도메인의 선행이 됐다.** 현재 `RuleSet`은 `initial()`만 있고 변경 메서드가 없어 **정족수가 영구히 0**이다(`Y-14`). 정족수가 0이면 `REVIEW_REQUEST_PLAN.md` 2-1절의 발행 조건 표에서 「승인·변경요청 존재 여부와 무관하게 Admin 이상이 언제든 발행할 수 있다」 경로만 타므로, **「리뷰어들이 승인하면」이라는 큰 흐름을 실제로 검증할 수 없다.**

**접근 검증의 단일 지점 제공**(`D-19`) — 다른 5개 도메인이 `WorkspaceAccessValidator`를 직접 주입한다. 이 도메인은 **포트를 제공하지 않는다**(9절).

### MVP1 범위 밖

| 대상 | 사유 | 대체 |
| --- | --- | --- |
| `Workspace.description` | `REQ-WS-001`·`REQ-WS-006` 비고가 「모델 미정의 — 필요해지면 `DOMAIN.md`에 먼저 추가」로 이미 처리했다(`X-10`) | 모델에 넣지 않고 근거만 기록한다 |
| 태그·공개여부 | 같은 이유 | — |
| `Participant.leftAt` | `DOMAIN.md` «모델 반영 필요(미확정)»의 「참여자 삭제 방식」(`X-11`). **`WS-2`에서 확정한다** | 그때까지 소프트 삭제(`BaseEntity.deletedAt`)를 쓴다 |
| 알림 설정 모델 | Notification 도메인 몫이고 6개 범위 밖(`X-12`) | 이벤트만 발행하고 소비자가 없어도 된다 |
| 이메일 발송 초대 | `REQ-WS-003` 비고 「이메일 발송은 후순위, 링크 복사 방식으로 시작」 | 토큰 링크 발급까지 |
| 워크스페이스 간 사전집 참조(`REQ-MV2-*`) | MVP2 | — |

**`Member`는 이 도메인이 아니다.** `Participant.memberId`는 논리 FK이며 `member` 테이블에 DB FK를 걸지 않는다 — as-built(`V2`에 `fk_participant_workspace`만 있다).

---

## 2. 결정 대기표

### 2-1. 확정된 결정

| ID | 확정 내용 | 문서 수정 |
| --- | --- | --- |
| **D-19** | **접근 검증은 `WorkspaceAccessValidator` 직접 주입을 규약의 명문 예외로 둔다.** 이 도메인은 **포트를 제공하지 않는다.** 근거 셋 — ① 「비참여자에게 404」와 `Permission` 서열 판정이 한 곳에 남아야 한다(`WorkspaceAccessValidator` javadoc) ② `validateAtLeast`는 예외를 던지고 값을 돌려주지 않아 `boolean` 포트에 그대로 쓸 수 없다 ③ 기구현 2개와 신규 4개가 같은 방식이 된다 | `ARCHITECTURE.md` «크로스 도메인 조회»에 예외 조항 추가 |
| **R-19** | **`WorkspacePolicyPort`를 룰셋 조회 하나로 축소한다.** 기존 3개 메서드 중 `isParticipant`·`hasAdminPermission`은 `D-19`로 대체되고 **`requiredReviewerCount`만 남는다** — 정족수는 `Workspace.ruleSet`에 있고 `WorkspaceAccessValidator`가 노출하지 않기 때문이다 | `REVIEW_REQUEST_PLAN.md` 9절(보존 — `CONFLICTS.md`가 지목) |
| **G-2** | 사전 초안 생성·교정 주체는 **ADMIN 이상**. 이 도메인은 그 판정 수단(`Permission.isAtLeast`)을 제공한다 | — |
| **G-4** | 승인 집계는 **룰셋 정족수** 기준이다. 리뷰어 지정은 알림·필터 용도다. **`WS-4`가 룰셋을 바꿀 수단을 만들어야 이것이 성립한다** | — |
| **O-2** | **참여자 이탈 이벤트를 이 도메인이 발행한다** — `ParticipantRemovedEvent`. `REVIEW_REQUEST_PLAN.md` 9절 수신 이벤트 표가 `**미정**`으로 기다리고 있다 | — |
| **D-25** | **`WorkspaceId`·`ParticipantId`와 두 테스트를 제거하고 `Long`으로 통일한다** | `DOMAIN.md` 속성 표 각주 |
| **Y-12** | **하위 디렉터리 규약을 맞춘다** — `presentation/dto/`·`service/model/`로 옮기고 `Request.toCommand()` 패턴을 쓴다. 현재 DTO·command가 패키지 루트에 있고 컨트롤러가 `new CreateWorkspaceCommand(...)`를 직접 조립한다. `member`·`document`·`dictionary`는 모두 규약을 따른다 | — |
| **Owner는 정확히 1명** | 워크스페이스와 Owner 참여자를 **한 트랜잭션에서** 만든다. `WorkspaceService.create`가 그 경계를 보장한다 — as-built | — |
| **Owner와 Admin은 동급** | Owner는 생성자에게 붙는 명칭이고 할 수 있는 일은 Admin과 같다. **삭제와 소유권 이전만 Owner 전용**이다. 권한 검사는 `validateAtLeast(..., ADMIN)` 한 줄로 하고 Owner 전용 분기를 만들지 않는다 — as-built | — |
| **비참여자에게 404** | 참여자가 아닌 워크스페이스에도 `WORKSPACE_NOT_FOUND`를 준다. `403`을 주면 존재가 드러난다(`NFR-WS-001`) — as-built | — |
| **검증 순서** | 참여 여부를 먼저 보고(404) 그다음 서열을 본다(403). 뒤집으면 비참여자에게 403이 나가 존재가 드러난다 — as-built이며 `validateAtLeast` javadoc이 근거를 갖고 있다 | — |
| **워크스페이스 생존까지 검증** | 소프트 삭제 시 참여자 행이 남으므로, 참여 여부만 보면 삭제된 워크스페이스에 딸린 자원이 새어 나간다. **`WorkspaceAccessValidator`가 `workspaceReader.read`로 생존을 함께 확인한다** — as-built | — |
| **소프트 삭제, 참여자 행은 유지** | 모든 조회가 워크스페이스에서 먼저 막히므로 참여자 행이 남아도 새어 나가지 않는다 — as-built | — |
| **서열은 level 값으로** | `Permission{OWNER(3), ADMIN(2), REGULAR(1)}`. ordinal이 아니라 level로 판단해 사이에 상수를 끼워 넣어도 서열이 뒤집히지 않는다 — as-built | — |
| **룰셋은 값 객체** | 식별자도 생명주기도 두지 않고 `workspace` 테이블 컬럼 2개로 저장한다. **엔티티로 승격하지 않는다** — 정족수는 스냅샷 없이 발행 시점 값을 실시간 조회한다(`D-12`·`D-13`) | — |
| **룰셋 상한 검증은 이 도메인** | `requiredDocumentReviewerCount`·`requiredDictionaryReviewerCount`는 **0 이상, 참여자 수 이하**다. 상한은 참여자 수를 알아야 하므로 `RuleSet` 생성자가 아니라 **implement 레이어에서 검증한다** — `RuleSet` javadoc이 이것을 예고했다. 리뷰 도메인은 룰셋 값을 그대로 신뢰하고 참여자 수를 조회하지 않는다 | — |
| **D-18** | 하위 디렉터리는 `presentation/dto/`·`service/model/`, ErrorCode는 `{domain}/exception/` — ErrorCode는 이미 준수, 나머지는 `WS-6` | — |

### 2-2. 남은 결정 대기

**없다. 2026-09-10에 전건 확정했다.** 모든 Phase에 착수할 수 있다.

`docs/DOMAIN.md` «모델 반영 필요(미확정)» 블록 중 이 도메인에 걸리는 것이 둘 있고 **둘 다 태스크에서 형태를 정한다** — 결정을 기다리는 것이 아니다.

- **참여자 권한 변경 주체**(`X-11`) → `WS-1`. `DOMAIN.md` «권한 설정»이 「소유권 이전은 기존 Owner가」와 「Admin은 Regular만 내보낼 수 있다」를 이미 규정했으므로, **같은 서열 규칙을 권한 변경에 적용한다**(Admin은 Regular↔Admin 승격·강등 불가, Owner만 가능).
- **참여자 삭제 방식**(`X-11`) → `WS-2`. `REVIEW_REQUEST_PLAN.md` 2-2절이 "우리 설계는 어느 쪽이 되어도 동작한다"고 적었으므로 **소프트 삭제로 간다** — `Participant`가 이미 `BaseEntity`를 상속하고 `ParticipantReader`가 `...AndDeletedAtIsNull`로 조회한다.

---

## 3. 도메인 모델

### Workspace (애그리게이트 루트)

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | `@GeneratedValue(IDENTITY)` |
| `name` | `String` | `name` | X | as-built | `varchar(50)`. 1~50자 |
| `ruleSet` | `RuleSet` | `required_document_reviewer_count`, `required_dictionary_reviewer_count` | X | as-built | `@Embedded` 값 객체. 생성 시 `0/0` |
| `createdBy` | `Long` | `created_by` | X | as-built | `updatable = false` |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | as-built | `BaseEntity` 상속 |
| ~~`description`~~ | — | — | — | **대기** | `X-10` — 모델에 넣지 않는다 |

**룰셋만 따로 만들거나 지우는 경로는 없다.** 워크스페이스와 함께 생기고 함께 사라지며 항상 정확히 1개다.

### Participant

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | as-built | |
| `workspaceId` | `Long` | `workspace_id` | X | as-built | `updatable = false`. **같은 도메인이라 DB FK를 건다** |
| `memberId` | `Long` | `member_id` | X | as-built | `updatable = false`. **논리 FK** — `member`에 DB FK를 걸지 않는다 |
| `permission` | `Permission` | `permission` | X | as-built | `@Enumerated(STRING)`, `varchar(20)`. **`updatable = false`를 떼야 한다**(`WS-1`) |
| `joinedAt` | `OffsetDateTime` | `joined_at` | X | as-built | `updatable = false` |
| `createdBy` | `Long` | `created_by` | X | as-built | `updatable = false`. 초대한 사람(Owner는 자기 자신) |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | as-built | `BaseEntity` 상속. 내보내기는 소프트 삭제 |
| ~~`leftAt`~~ | — | — | — | **대기** | `X-11` — `deletedAt`으로 대신한다 |

> **`permission`에 `updatable = false`가 없는 것이 맞다.** 현재 코드는 이 필드에 `updatable = false`를 붙이지 않았으므로 `WS-1`이 변경 메서드만 추가하면 된다. `workspaceId`·`memberId`·`joinedAt`은 불변으로 유지한다.

워크스페이스를 `@ManyToOne`으로 참조하지 않고 식별자로만 가리킨다. 애그리게이트 경계를 식별자로 넘어 지연 로딩 프록시가 상위 레이어로 새는 경로를 막는다 — DB에는 FK가 그대로 있다.

### Invitation (신규)

| 필드 | 타입 | 컬럼 | nullable | 상태 | 비고 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | **추가** | |
| `workspaceId` | `Long` | `workspace_id` | X | **추가** | DB FK |
| `inviteeEmail` | `String` | `invitee_email` | O | **추가** | `varchar(320)`. **링크 복사 방식이면 `null`** |
| `token` | `String` | `token` | X | **추가** | `varchar(64)`. **전역 유일.** URL-safe 랜덤 |
| `permission` | `Permission` | `permission` | X | **추가** | 수락 시 부여할 권한. **`ADMIN` / `REGULAR`만** — Owner는 초대로 부여하지 않는다 |
| `status` | `InvitationStatus` | `status` | X | **추가** | `@Enumerated(STRING)`, `varchar(20)` |
| `expiresAt` | `OffsetDateTime` | `expires_at` | X | **추가** | |
| `acceptedAt` | `OffsetDateTime` | `accepted_at` | O | **추가** | |
| `acceptedParticipantId` | `Long` | `accepted_participant_id` | O | **추가** | 수락 후 만들어진 참여자 |
| `createdBy` | `Long` | `created_by` | X | **추가** | 초대한 사람 |
| `createdAt`/`updatedAt`/`deletedAt` | `OffsetDateTime` | — | — | **추가** | `BaseEntity` 상속 — 취소가 상태 전이이므로 `deletedAt`은 쓰지 않지만, 만료 정리 배치가 생길 여지를 남긴다 |

`DOMAIN.md` «Invitation (초대)» 표를 그대로 옮긴 것이다. `token`을 전역 유일로 두는 이유는 **초대 링크가 워크스페이스를 모른 채 들어오기 때문**이다 — 수락 요청이 토큰만 갖고 온다.

### 애그리게이트 경계

`Workspace`·`Participant`·`Invitation`은 **같은 애그리게이트**지만 JPA 컬렉션으로 묶지 않는다.

- 「Owner는 정확히 1명」은 **`WorkspaceService.create`의 트랜잭션 경계**가 보장한다.
- 「참여자는 최대 5명」은 **애플리케이션 검증**이다. `ParticipantRepository.countByWorkspaceIdAndDeletedAtIsNull`이 그 수단이며 **정원 검사는 수락 시점**에 한다(`DOMAIN.md` «초대»).
- 「룰셋은 참여자 수 이하」도 애플리케이션 검증이고 **룰셋을 수정하는 쪽**에서 한다(2-1절).
- 워크스페이스를 소프트 삭제해도 참여자·초대 행은 지우지 않는다.

### 값 객체

`RuleSet`(`@Embeddable record`) — `requiredDocumentReviewerCount` + `requiredDictionaryReviewerCount`. compact constructor가 **0 이상**만 검증하고 **상한은 검증하지 않는다** — 참여자 수를 알아야 하기 때문이다(javadoc이 근거를 갖고 있다). `initial()`이 `0/0`을 만든다.

### enum

```
Permission { OWNER(3), ADMIN(2), REGULAR(1) }          // as-built. isAtLeast(required)
InvitationStatus { PENDING, ACCEPTED, EXPIRED, CANCELED }   // 추가 — 대기 / 수락 / 만료 / 취소
```

`Permission`의 level 값은 **선언 순서(ordinal)가 아니라 명시적 숫자**다. 사이에 상수를 끼워 넣어도 서열이 뒤집히지 않는다.

`InvitationStatus`는 `DOMAIN.md`의 한국어 값을 그대로 옮긴 것이다.

---

## 4. 상태 전이와 도메인 메서드

### InvitationStatus

| from | to | 트리거 | 조건 | 상태 |
| --- | --- | --- | --- | --- |
| (신규) | `PENDING` | `Invitation.issue(...)` | Admin 이상. **이미 참여자인 회원에게는 발급하지 않는다.** 같은 워크스페이스·같은 대상에 `PENDING`은 1개만 | **추가** |
| `PENDING` | `ACCEPTED` | `accept(participantId, at)` | 만료 전 + **정원 미달**. 위반 시 `WORKSPACE_PARTICIPANT_LIMIT_EXCEEDED`(409) | **추가** |
| `PENDING` | `EXPIRED` | `expire()` | `expiresAt` 경과. **조회 시점에 판정하고 저장한다** | **추가** |
| `PENDING` | `CANCELED` | `cancel(actorId)` | 발급자 본인 또는 Admin 이상 | **추가** |
| `ACCEPTED`/`EXPIRED`/`CANCELED` | — | — | 종단 | **추가** |

**정원 검사가 수락 시점인 이유** — 발급 시점에 검사하면 그 사이에 정원이 차도 수락이 통과한다(`DOMAIN.md` «초대»가 명시).

**만료를 배치로 처리하지 않는다.** 수락·조회가 `expiresAt`을 보고 판정하면 충분하고, 배치는 인프라를 늘린다. `status`에 `EXPIRED`를 두는 이유는 **만료된 토큰으로 수락을 시도한 흔적을 남기기** 위해서다.

### Participant 권한 전이

| from | to | 트리거 | 누가 | 상태 |
| --- | --- | --- | --- | --- |
| `REGULAR` | `ADMIN` | `changePermission(ADMIN, actorId)` | **Owner만** | **추가** |
| `ADMIN` | `REGULAR` | `changePermission(REGULAR, actorId)` | **Owner만** | **추가** |
| `ADMIN` | `OWNER` | `transferOwnership(...)` | **기존 Owner만.** 기존 Owner는 `ADMIN`으로 내려간다 | **추가** |
| `REGULAR` | `OWNER` | `transferOwnership(...)` | 같음 | **추가** |
| 어느 것이든 | (소프트 삭제) | `remove(actorId)` | **Admin은 `REGULAR`만, `ADMIN`을 내보내는 것은 Owner만.** Owner는 삭제 대상이 아니다 | **추가** |

**권한 변경을 Owner 전용으로 두는 근거**(`X-11` 확정) — `DOMAIN.md` «삭제»가 「Admin은 Regular만 내보낼 수 있고, Admin을 내보내는 것은 Owner만 가능하다」로 **같은 서열의 상호 조작을 금지**했다. 권한 변경에 같은 규칙을 적용하면 Admin이 다른 Admin을 강등하는 경로가 막힌다.

**소유권 이전은 두 참여자를 한 트랜잭션에서 바꾼다.** 「Owner가 정확히 1명」이 그 경계 안에서만 성립한다.

**권한을 낮춰도 그 참여자가 Admin 권한으로 이미 한 작업은 되돌리지 않는다**(`DOMAIN.md` «권한 설정»).

### 도메인 메서드 시그니처

```java
// Workspace
static Workspace create(String name, Long createdBy);        // as-built
void rename(String name);                                    // as-built
void changeRuleSet(RuleSet ruleSet);                         // 추가 WS-4 — 상한 검증은 implement가 먼저 한다

// RuleSet (@Embeddable record)
static RuleSet initial();                                    // as-built
boolean exceedsParticipantCount(long participantCount);      // 추가 WS-4 — 판정만, 예외는 implement가 던진다

// Participant
static Participant owner(Long workspaceId, Long memberId);           // as-built
static Participant join(Long workspaceId, Long memberId,
        Permission permission, Long invitedBy);                       // 추가 WS-3
void changePermission(Permission permission);                         // 추가 WS-1
void demoteToAdmin();                                                 // 추가 WS-2 — 소유권 이전 시 기존 Owner
void promoteToOwner();                                                // 추가 WS-2
boolean isOwner();                                                    // 추가 WS-2
boolean canBeManagedBy(Permission actorPermission);                   // 추가 WS-1/WS-2 — 같은 서열 상호 조작 금지

// Permission
boolean isAtLeast(Permission required);                               // as-built

// Invitation
static Invitation issue(Long workspaceId, String inviteeEmail,
        Permission permission, OffsetDateTime expiresAt, Long createdBy);   // 추가 WS-3
void accept(Long participantId, OffsetDateTime at);                   // 추가 WS-3
void expire();                                                        // 추가 WS-3
void cancel();                                                        // 추가 WS-3
boolean isAcceptable(OffsetDateTime now);                             // 추가 WS-3
```

**`canBeManagedBy`를 도메인에 두는 이유** — 「Admin은 Regular만」이라는 규칙이 내보내기와 권한 변경 두 유스케이스에 걸린다. implement 두 곳에 복제하면 규칙이 두 벌이 된다.

**`exceedsParticipantCount`가 판정만 하고 예외를 던지지 않는 이유** — `RuleSet`은 `record`이고 `domain`에 있어 `BusinessException`을 던질 수는 있지만, **참여자 수를 인자로 받아야 하므로 검증 주체가 implement**다. 판정 규칙만 값 객체에 두어 두 벌이 되지 않게 한다.

---

## 5. 스키마와 Flyway

**대역: 100–199**(`backend/CLAUDE.md`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 | 상태 | 태스크 |
| --- | --- | --- | --- |
| `V2__create_workspace_and_participant.sql` | `workspace`, `participant` | as-built | — |
| `V100__create_invitation.sql` | `invitation` | **추가** | `WS-3` |

> **`V2`가 대역 밖인 것은 고치지 않는다.** 규칙 제정 전에 만들어졌고 이미 머지됐다. 파일명을 바꾸면 Flyway 체크섬이 어긋난다. **신규만 `V100`부터** 쓰고 `spring.flyway.out-of-order=true` 설정은 `T-INT-1`이 처리한다(`Y-10`).

```sql
-- V100
-- 초대 링크(토큰)로 참여자를 등록한다. 이메일 발송은 후순위이고 링크 복사 방식으로 시작한다.
-- 토큰을 전역 유일로 두는 이유는 수락 요청이 워크스페이스를 모른 채 토큰만 갖고 오기 때문이다.
create table invitation (
    id                       bigint       not null auto_increment,
    workspace_id             bigint       not null,
    -- 링크 복사 방식이면 null이다. 이메일 발송이 붙으면 채워진다.
    invitee_email            varchar(320),
    token                    varchar(64)  not null,
    -- 수락 시 부여할 권한. ADMIN / REGULAR만 온다 — Owner는 초대로 부여하지 않는다.
    permission               varchar(20)  not null,
    status                   varchar(20)  not null,
    expires_at               datetime(6)  not null,
    accepted_at              datetime(6),
    accepted_participant_id  bigint,
    created_by               bigint       not null,
    created_at               datetime(6)  not null,
    updated_at               datetime(6)  not null,
    deleted_at               datetime(6),
    primary key (id),
    constraint fk_invitation_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_invitation_token unique (token)
);

-- 「같은 워크스페이스·같은 대상에 대기 상태 초대는 1개」는 부분 유니크가 필요해
-- MySQL 8.4에서 DB로 지킬 수 없다(D-10과 같은 사정). 애플리케이션에서 검증한다.
create index idx_invitation_workspace_status on invitation (workspace_id, status);
```

**대기 상태 초대 1개 제약을 DB로 걸지 않는 이유** — `status = 'PENDING'`인 행만 유니크로 묶으려면 부분 유니크 인덱스가 필요하고 MySQL 8.4에 없다. `dictionary`의 `active_flag` 생성 컬럼 기법을 쓸 수도 있지만 **`inviteeEmail`이 `null`일 수 있어**(링크 복사 방식) 유니크 키가 성립하지 않는다. `D-10`이 같은 사정으로 애플리케이션 검증을 택했다.

**`participant.permission`에 스키마 변경이 없다.** `V2`가 이미 `varchar(20) not null`이고 엔티티에 `updatable = false`가 없어, `WS-1`은 도메인 메서드만 추가하면 된다.

---

## 6. 패키지와 파일

```
com.ubidict.backend.workspace
├── presentation
│   ├── WorkspaceController                      as-built
│   ├── ParticipantController                    WS-1
│   ├── InvitationController                     WS-3
│   └── dto                                      ← WS-6에서 신설, 루트에서 이동
│       ├── CreateWorkspaceRequest               이동 WS-6 (+ toCommand)
│       ├── UpdateWorkspaceRequest               이동 WS-6 (+ toCommand)
│       ├── UpdateRuleSetRequest                 WS-4
│       ├── ChangePermissionRequest              WS-1
│       ├── IssueInvitationRequest               WS-3
│       ├── WorkspaceResponse                    이동 WS-6
│       ├── ParticipantResponse                  WS-1
│       └── InvitationResponse                   WS-3
├── service
│   ├── WorkspaceService                         as-built (+ changeRuleSet WS-4)
│   ├── ParticipantService                       WS-1
│   ├── InvitationService                        WS-3
│   └── model                                    ← WS-6에서 신설, 루트에서 이동
│       ├── CreateWorkspaceCommand               이동 WS-6
│       ├── RenameWorkspaceCommand               이동 WS-6
│       ├── UpdateRuleSetCommand                 WS-4
│       ├── ChangePermissionCommand              WS-1
│       ├── IssueInvitationCommand               WS-3
│       ├── WorkspaceResult                      이동 WS-6
│       ├── ParticipantResult                    WS-1
│       └── InvitationResult                     WS-3
├── implement
│   ├── WorkspaceReader                          as-built
│   ├── WorkspaceAppender                        as-built
│   ├── WorkspaceUpdater                         as-built (+ changeRuleSet WS-4)
│   ├── WorkspaceRemover                         as-built (+ 이벤트 발행 WS-5)
│   ├── WorkspaceAccessValidator                 as-built (+ 감사 로그 WS-1)
│   ├── ParticipantReader                        as-built (+ readAll·count WS-1)
│   ├── ParticipantAppender                      as-built (+ appendMember WS-3)
│   ├── ParticipantUpdater                       WS-1   — 권한 변경·소유권 이전
│   ├── ParticipantRemover                       WS-2   — 내보내기(소프트 삭제) + 이벤트
│   ├── ParticipantPolicyValidator               WS-1/WS-2 — 서열 상호 조작 금지
│   ├── RuleSetValidator                         WS-4   — 상한(참여자 수 이하) 검증
│   ├── InvitationReader                         WS-3
│   ├── InvitationAppender                       WS-3
│   ├── InvitationUpdater                        WS-3   — 수락·만료·취소
│   └── InvitationTokenGenerator                 WS-3   — URL-safe 랜덤
├── infra
│   ├── WorkspaceRepository                      as-built
│   ├── ParticipantRepository                    as-built (+ findAll WS-1)
│   ├── InvitationRepository                     WS-3
│   └── adapter
│       └── WorkspacePolicyAdapter               WS-4   — reviewrequest의 룰셋 조회 구현(R-19)
├── domain
│   ├── Workspace                                as-built (+ changeRuleSet WS-4)
│   ├── RuleSet                                  as-built (+ exceedsParticipantCount WS-4)
│   ├── Participant                              as-built (+ 권한 전이 메서드 WS-1/WS-2)
│   ├── Permission                               as-built
│   ├── Invitation                               WS-3
│   ├── InvitationStatus                         WS-3
│   ├── ~~WorkspaceId~~                          제거 D-25
│   ├── ~~ParticipantId~~                        제거 D-25
│   └── event
│       ├── WorkspaceDeletedEvent                WS-5
│       └── ParticipantRemovedEvent              WS-5 (O-2)
└── exception
    ├── WorkspaceErrorCode                       as-built (+ 4건)
    └── InvitationErrorCode                      WS-3
```

**`infra/port/`가 없다.** 이 도메인은 다른 도메인을 조회하지 않는다 — `Participant.memberId`가 논리 FK이지만 회원 정보를 읽을 유스케이스가 MVP1에 없다(참여자 목록은 `memberId`만 내린다).

**`adapter/`에 하나만 있다.** `D-19`로 접근 검증 포트가 사라지고 **룰셋 조회만 남았다**(`R-19`).

---

## 7. API 명세

접두사 `/api`(버저닝 없음). 컨트롤러는 **`ResponseEntity<T>`를 반환**하고 `@ResponseStatus`를 쓰지 않는다. **Swagger 애노테이션은 쓰지 않는다**(명세는 `docs/API.md`가 담당).

요청자 식별은 **`@RequestParam Long memberId`**다. 인증 계층(`NFR-USR-001`)이 없어 생긴 임시 방식이며 인증 도입 시 전부 사라진다. 컨트롤러 클래스 주석에 `TODO(NFR-USR-001)`과 **"인증 도입 전까지 운영 배포 대상이 아니다"**를 적는다 — `WorkspaceController`가 이 선례를 만들었다.

### 워크스페이스

| 상태 | Method | Path | 요청 | 응답 | 성공 | 권한 |
| --- | --- | --- | --- | --- | --- | --- |
| as-built | POST | `/api/workspaces` | `CreateWorkspaceRequest{name}` | `WorkspaceResponse` | `201` | 로그인 |
| as-built | GET | `/api/workspaces` | — | `WorkspaceResponse[]` | `200` | — |
| as-built | GET | `/api/workspaces/{workspaceId}` | — | `WorkspaceResponse` | `200` | 참여자 |
| as-built | PATCH | `/api/workspaces/{workspaceId}` | `UpdateWorkspaceRequest{name}` | — | `204` | ADMIN 이상 |
| **추가** | **PATCH** | **`/api/workspaces/{workspaceId}/rule-set`** | **`UpdateRuleSetRequest{requiredDocumentReviewerCount, requiredDictionaryReviewerCount}`** | **`WorkspaceResponse`** | **`200`** | **ADMIN 이상** |
| as-built | DELETE | `/api/workspaces/{workspaceId}` | — | — | `204` | **OWNER** |

**목록은 배열을 유지한다** — `API.md`가 「한 회원이 참여하는 워크스페이스 수가 구조적으로 작다」는 이유로 이미 페이징 예외로 명시했다.

**룰셋 수정 응답이 `200`인 이유** — 상한 검증에 걸리지 않고 저장된 실제 값을 클라이언트가 확인해야 한다. 이름 수정(`204`)과 다른 이유다.

### 참여자

| 상태 | Method | Path | 요청 | 응답 | 성공 | 권한 |
| --- | --- | --- | --- | --- | --- | --- |
| **추가** | GET | `/api/workspaces/{workspaceId}/participants` | — | `ParticipantResponse[]` | `200` | 참여자 |
| **추가** | PATCH | `.../participants/{participantId}/permission` | `ChangePermissionRequest{permission}` | — | `204` | **OWNER** |
| **추가** | PATCH | `.../participants/{participantId}/ownership` | — | — | `204` | **OWNER** |
| **추가** | DELETE | `.../participants/{participantId}` | — | — | `204` | ADMIN 이상(서열 규칙) |

**참여자 목록도 배열이다** — 정원이 5명이라 구조적으로 작다.

**소유권 이전을 별 경로로 두는 이유** — `permission = OWNER`로 바꾸는 것이 아니라 **두 참여자를 함께 바꾸는 일**이다(기존 Owner가 Admin으로 내려간다). 같은 엔드포인트에 섞으면 요청 하나가 두 의미를 갖는다.

**내보내기 권한이 「ADMIN 이상(서열 규칙)」인 이유** — Admin은 `REGULAR`만, `ADMIN`을 내보내는 것은 Owner만 가능하다. 대상의 권한에 따라 필요 권한이 달라지므로 컨트롤러가 아니라 `ParticipantPolicyValidator`가 판정한다.

### 초대

| 상태 | Method | Path | 요청 | 응답 | 성공 | 권한 |
| --- | --- | --- | --- | --- | --- | --- |
| **추가** | POST | `/api/workspaces/{workspaceId}/invitations` | `IssueInvitationRequest{inviteeEmail?, permission}` | `InvitationResponse` | `201` | ADMIN 이상 |
| **추가** | GET | `/api/workspaces/{workspaceId}/invitations` | `status?` | `InvitationResponse[]` | `200` | ADMIN 이상 |
| **추가** | DELETE | `.../invitations/{invitationId}` | — | — | `204` | 발급자 또는 ADMIN 이상 |
| **추가** | POST | `/api/invitations/{token}/accept` | — | `WorkspaceResponse` | `201` | **로그인** |

**수락 경로가 워크스페이스 하위가 아닌 이유** — 초대 링크는 **워크스페이스를 모른 채** 들어온다. 토큰이 전역 유일이므로 그것만으로 워크스페이스를 찾는다. 이것이 이 도메인에서 `workspaceId`가 URL에 없는 유일한 엔드포인트다.

**수락 응답이 `WorkspaceResponse`인 이유** — 수락 직후 화면이 그 워크스페이스로 진입하므로 `myPermission`을 포함한 상세가 필요하다.

**`InvitationResponse`에 `token`을 담는다** — 링크 복사 방식이므로 발급 응답에서 토큰을 받아야 한다. 목록 조회에서는 **`token`을 내리지 않는다**(이미 발급된 링크를 다시 노출할 이유가 없다).

### 권한

| 대상 | 규칙 |
| --- | --- |
| 워크스페이스 생성 | 로그인만. 검증 없음 |
| 참여 중인 목록 조회 | 참여한 것만 내려가므로 별도 검증이 필요 없다 — as-built |
| 상세·참여자 목록 | `validateParticipant(workspaceId, memberId)` |
| 이름·룰셋 수정, 초대 발급·목록·취소, 문서 삭제 | `validateAtLeast(..., Permission.ADMIN)` |
| 삭제, 권한 변경, 소유권 이전 | `validateAtLeast(..., Permission.OWNER)` |
| 내보내기 | `validateAtLeast(..., ADMIN)` + **대상 권한에 따른 서열 규칙**(`ParticipantPolicyValidator`) |
| 비참여자·삭제된 워크스페이스 | `404 WORKSPACE_NOT_FOUND` — 존재를 숨긴다 |
| 참여자지만 서열 부족 | `403` + **WARN 감사 로그**(`NFR-REV-001`·`API.md` «데이터 격리») |

**403에 WARN 감사 로그를 남기는 것이 `Y-06`의 해소 지점이다.** `API.md`가 이미 「참여자이지만 서열이 모자란 경우에만 403이고, 이때는 WARN 감사 로그를 남긴다」를 규정했는데 `WorkspaceAccessValidator`가 로그를 남기지 않는다. **`WS-1`에서 넣는다.**

---

## 8. ErrorCode

`workspace/exception/WorkspaceErrorCode`, `workspace/exception/InvitationErrorCode`. 접두사 하나당 enum 하나다.

| 상수 | status | message | 상태 | 태스크 |
| --- | --- | --- | --- | --- |
| `WORKSPACE_NOT_FOUND` | 404 | 워크스페이스를 찾을 수 없습니다. | as-built | — |
| `WORKSPACE_INVALID_NAME` | 400 | 워크스페이스 이름은 1자 이상 50자 이하여야 합니다. | as-built | — |
| `WORKSPACE_INVALID_REVIEWER_COUNT` | 400 | 필수 리뷰어 수는 0 이상이어야 합니다. | as-built | — |
| `WORKSPACE_ADMIN_REQUIRED` | 403 | 워크스페이스 관리자 이상만 수행할 수 있습니다. | as-built | — |
| `WORKSPACE_OWNER_REQUIRED` | 403 | 워크스페이스 소유자만 수행할 수 있습니다. | as-built | — |
| `WORKSPACE_REVIEWER_COUNT_EXCEEDS_PARTICIPANTS` | 400 | 필수 리뷰어 수는 참여자 수를 넘을 수 없습니다. | **추가** | `WS-4` |
| `WORKSPACE_PARTICIPANT_NOT_FOUND` | 404 | 참여자를 찾을 수 없습니다. | **추가** | `WS-1` |
| `WORKSPACE_PARTICIPANT_LIMIT_EXCEEDED` | 409 | 워크스페이스 참여자는 최대 5명입니다. | **추가** | `WS-3` |
| `WORKSPACE_OWNER_CANNOT_BE_REMOVED` | 409 | 소유자는 내보낼 수 없습니다. | **추가** | `WS-2` |
| `INVITATION_NOT_FOUND` | 404 | 초대를 찾을 수 없습니다. | **추가** | `WS-3` |
| `INVITATION_NOT_ACCEPTABLE` | 409 | 만료되거나 이미 처리된 초대입니다. | **추가** | `WS-3` |
| `INVITATION_ALREADY_PARTICIPANT` | 409 | 이미 참여 중인 회원입니다. | **추가** | `WS-3` |
| `INVITATION_DUPLICATE_PENDING` | 409 | 같은 대상에 대기 중인 초대가 이미 있습니다. | **추가** | `WS-3` |
| `INVITATION_OWNER_NOT_ALLOWED` | 400 | 초대로는 소유자 권한을 부여할 수 없습니다. | **추가** | `WS-3` |

**`WORKSPACE_ADMIN_REQUIRED`·`WORKSPACE_OWNER_REQUIRED`를 다른 도메인이 그대로 쓴다.** 권한 부족은 워크스페이스 권한이 모자란 것이므로 도메인마다 같은 뜻의 코드를 늘리지 않는다 — `DocumentErrorCode`·`DictionaryErrorCode` javadoc이 이 근거를 갖고 있다.

**내보내기 서열 위반에 새 코드를 두지 않는다.** Admin이 Admin을 내보내려 하면 `WORKSPACE_OWNER_REQUIRED`(403)다 — 「Owner만 할 수 있다」가 정확한 설명이다.

---

## 9. 크로스 도메인 계약

### 이 도메인은 접근 검증 포트를 제공하지 않는다 (`D-19`)

다른 5개 도메인이 **`WorkspaceAccessValidator`를 직접 주입한다.** `ARCHITECTURE.md` «크로스 도메인 조회 — 포트와 어댑터»의 명문 예외이며 `T-DOC-1`이 그 조항을 추가한다.

**우리가 제공하는 계약**

```java
// workspace/implement/WorkspaceAccessValidator — 다른 도메인이 직접 주입한다
Permission validateParticipant(Long workspaceId, Long memberId);   // 비참여자·삭제된 워크스페이스 → 404
void validateAtLeast(Long workspaceId, Long memberId, Permission required);   // 서열 부족 → 403 + WARN
```

**이 클래스가 지키는 세 가지**

1. **비참여자에게 404** — `403`을 주면 워크스페이스의 존재가 드러난다(`NFR-WS-001`).
2. **검증 순서** — 참여 여부를 먼저(404), 그다음 서열을(403). 뒤집으면 비참여자에게 403이 나가 존재가 드러난다.
3. **워크스페이스 생존까지 확인** — 소프트 삭제 시 참여자 행이 남으므로 참여 여부만 보면 삭제된 워크스페이스에 딸린 자원이 새어 나간다. 사전집·문서처럼 워크스페이스 자체를 읽을 이유가 없는 도메인도 같은 검증에 올라탄다.

**세 규칙이 한 곳에 있어야 하는 것이 `D-19`의 근거다.** 소비 도메인마다 포트·어댑터를 두면 `Permission` 서열 판정과 404 정책이 5벌로 복제되고, 한 곳을 고칠 때 나머지가 남는다.

> **이 클래스의 시그니처를 바꾸면 5개 도메인이 깨진다.** 변경이 필요하면 통합 태스크로 넘기고 PR에 영향 범위를 적는다.

### 제공 포트 (다른 도메인이 우리를 볼 때 — 우리가 어댑터를 구현한다)

| 소비자가 정의한 포트 | 시그니처 | 근거 메서드 | 태스크 |
| --- | --- | --- | --- |
| `reviewrequest/infra/port/WorkspacePolicyPort` | `int requiredReviewerCount(Long workspaceId, ReviewRequestType type)` | `WorkspaceRepository.findByIdAndDeletedAtIsNull` → `RuleSet` | `WS-4` |

**`R-19`로 3개 메서드에서 하나로 줄었다.** `isParticipant`·`hasAdminPermission`은 `D-19`의 직접 주입으로 대체된다. `REVIEW_REQUEST_PLAN.md` 9절이 이미 "`validateAtLeast`는 예외를 던지고 값을 돌려주지 않으므로 `boolean`을 반환하는 포트에 그대로 쓸 수 없다 — 어댑터가 `Participant.permission`을 읽어 `Permission.isAtLeast(...)`로 판단한다"고 적어 **불일치를 자각하고 있었다.** 직접 주입이 그 우회를 없앤다.

**룰셋 조회만 포트로 남는 이유** — 정족수는 `Workspace.ruleSet`에 있고 `WorkspaceAccessValidator`가 노출하지 않는다. 값을 읽는 순수 조회이므로 `ARCHITECTURE.md`의 포트 용도(「조회와 발행 위임」)에 정확히 들어맞는다.

~~**어댑터는 `workspace/infra/adapter/`에 두고 자기 `infra`(Repository)를 참조한다.**~~ **정정(2026-09-12, `D-33`)** — `requiredReviewerCount`는 **조회 포트**이므로 어댑터도 소비 도메인이 갖는다. `ARCHITECTURE.md` «크로스 도메인 **조회** — 포트와 어댑터»가 「어댑터도 소비 도메인이 구현한다」로 못박은 대로다(`X-21`).

**`reviewrequest/infra/adapter/WorkspacePolicyAdapter`로 옮겼고 짝 스텁(`WorkspacePolicyStub`, 정족수 0)도 그 옆에 있다.** 어댑터 선택은 `app.crossdomain.workspace.mode`가 한다. 이 도메인은 `Workspace`·`RuleSet`·`WorkspaceRepository`를 **읽히기만** 하고 어댑터 파일을 갖지 않는다.

**실효 정족수는 `ruleSet` 값 그대로다.** `min(ruleSet, 참여자 수)`를 계산하지 않는다 — **상한 검증은 룰셋을 설정·수정하는 이 도메인의 책임**이고(`WS-4`), 참여자 이탈로 정족수를 채울 수 없게 된 교착은 Admin 이상의 발행으로 푼다. 그래서 포트에 `participantCount`를 두지 않는다.

### 소비 포트

**없다.** `Participant.memberId`가 논리 FK이지만 회원 정보를 읽을 유스케이스가 MVP1에 없다 — 참여자 목록은 `memberId`만 내리고 화면이 회원 API로 이름을 채운다.

### 발행 이벤트

| 이벤트 | 페이로드 | 수신자 | 태스크 |
| --- | --- | --- | --- |
| `WorkspaceDeletedEvent` | `(Long workspaceId, OffsetDateTime occurredAt)` | Notification(미정) | `WS-5` |
| `ParticipantRemovedEvent` | `(Long workspaceId, Long memberId, OffsetDateTime occurredAt)` | **ReviewRequest**(지정 리뷰어에서 제외), Notification(미정) | `WS-5` |

**불변 record, 식별자·원시값·시각만 담는다.** 도메인 모델이 곧 JPA 엔티티이므로 엔티티·지연 로딩 프록시·연관 컬렉션을 담으면 안 된다. `ARCHITECTURE.md`가 이 도메인을 예로 들어 금지 형태(`WorkspaceCreatedEvent(Workspace workspace)`)와 허용 형태를 보여 준다.

`implement`는 `common/infra/event/EventPublisher` 포트만 주입받는다.

> **`ParticipantRemovedEvent`가 `O-2`를 해소한다.** `REVIEW_REQUEST_PLAN.md` 9절 수신 이벤트 표가 「참여자 이탈 이벤트 | Workspace | 지정 리뷰어에서 제외 | **미정**」으로 남겨 둔 항목이다. **이 도메인이 발행하기로 확정**했다.
>
> `memberId`를 담는 이유는 수신 측이 `Reviewer.memberId`로 매칭하기 때문이다. `participantId`는 리뷰 도메인이 모른다.

**`WorkspaceDeletedEvent`를 다른 도메인이 구독하지 않는다.** 모든 조회가 `WorkspaceAccessValidator`에서 먼저 막히므로 문서·사전집 행을 지울 필요가 없고, **사전집은 애초에 삭제하지 않는다.** 발행은 Notification과 감사 목적이다.

### 수신 이벤트

**없다.** 이 도메인은 다른 도메인의 상태에 의존하지 않는다 — 워크스페이스가 모든 것의 상위 개념이다.

---

## 10. 공유 파일 규약

### 공유 자산과 주인

| 자산 | 주인 | 규약 |
| --- | --- | --- |
| `workspace/implement/WorkspaceAccessValidator` | **이 도메인** | 5개 도메인이 직접 주입한다(`D-19`). **시그니처를 바꾸면 전부 깨진다** — 변경은 통합 태스크로 넘기고 PR에 영향 범위를 적는다 |
| `workspace/domain/Permission` | **이 도메인** | 다른 도메인이 `Permission.ADMIN`을 인자로 넘긴다. **상수를 지우거나 이름을 바꾸지 않는다.** 사이에 끼워 넣는 것은 level 값 덕분에 안전하다 |
| `common/presentation/PageResponse`, `common/service/PageResult` | `T-CMN-1` | 이 도메인의 목록은 모두 구조적으로 작아 **배열을 유지한다.** 쓰지 않는다 |
| `common/domain/TextRange` | DraftDocument (`DD-2`) | 쓰지 않는다 |
| `docs/API.md` Workspace 절 | **이 도메인** | 파일 안의 자기 절만 고친다. 공통 규칙·페이징·버저닝·에러 응답 형식 절은 건드리지 않는다 |

### 수정 금지 파일

`common/**`, `common/domain/BaseEntity`, `common/domain/AuditableEntity`, `backend/src/test/java/.../support/**`, `application.properties`, `build.gradle`, 그리고 **다른 도메인의 패키지 전체**.

> **`V2__create_workspace_and_participant.sql`을 고치지 않는다.** 이미 머지됐고 파일명·내용을 바꾸면 Flyway 체크섬이 어긋난다. 대역 밖인 것은 `T-INT-1`이 `out-of-order`로 처리한다.

> **`docs/DOMAIN.md`·`REQUIREMENTS.md`·`ARCHITECTURE.md`는 `T-DOC-1`만 고친다.** 이 도메인의 `D-19`(`ARCHITECTURE.md` 예외 조항)와 `X-10`·`X-11`이 그 파일들을 향하지만 직접 고치지 않는다.

### git worktree 운영

| 항목 | 규칙 | 이유 |
| --- | --- | --- |
| 브랜치명 | `feat/WLSH-{티켓번호}-workspace` | `.githooks/prepare-commit-msg`가 브랜치명에서 `[A-Z]+-[0-9]+`를 뽑아 커밋 메시지에 삽입한다 |
| `bootRun` 동시 실행 금지 | 한 번에 한 worktree만 | Compose 프로젝트가 worktree마다 떠 호스트 포트가 겹친다 |
| `./gradlew test` 동시 실행 주의 | 여러 worktree에서 동시에 돌리지 않는다 | Testcontainers + LGTM이 Docker 기본 메모리에서 OOM |
| **머지 순서** | **`WS-4`를 가장 먼저 develop에 올린다** | 리뷰 도메인의 발행 판정이 이것을 기다린다(`G-4`) |
| develop 동기화 | `T-DOC-1` 머지 직후 **즉시** | `ARCHITECTURE.md` 예외 조항이 그때 들어온다 |

---

## 11. 테스트 계획

| 계층 | 방식 |
| --- | --- |
| Unit(domain) | 순수 JUnit, Spring 없음. `@Nested`로 케이스를 묶는다 |
| Unit(implement) | 순수 로직 검증기만 별도로 만든다 — `WorkspaceAccessValidatorTest` 선례 |
| Repository(infra) | `RepositoryTestSupport` 상속 — `@DataJpaTest` + 실제 MySQL 컨테이너 + **Flyway가 만든 스키마** |
| Service | `IntegrationTestSupport` 상속 — `@SpringBootTest(webEnvironment = NONE)` + `DbCleaner` |
| Controller(presentation) | `@WebMvcTest(XxxController.class)` + `@MockitoBean` + **RestAssuredMockMvc** |

> **개별 테스트에 `@Transactional`을 붙이지 않는다.** 서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써 경계 자체를 검증하지 못하게 된다. **이 도메인에서 특히 중요하다** — 「Owner는 정확히 1명」과 소유권 이전이 트랜잭션 경계로만 보장된다. `docs/TEST.md`가 롤백을 권장하는 것은 낡은 서술이며 `T-DOC-1`이 정정한다(`D-32`).

> **테스트 전용 엔티티를 만들지 않는다.** 스키마는 Flyway가 만들고 `DbCleaner`가 `information_schema`에서 실제 테이블을 읽어 비운다.

AssertJ를 쓴다. `@DisplayName`은 한국어 완결 문장 + 마침표, 메서드명은 `{메서드명}_{조건}` camelCase다.

### 테스트 클래스와 필수 케이스

| 클래스 | 계층 | 필수 케이스 | 태스크 |
| --- | --- | --- | --- |
| `domain/WorkspaceTest` | Unit | as-built + **`changeRuleSet_replacesValues`** | `WS-4` |
| `domain/RuleSetTest` | Unit | as-built + **`exceedsParticipantCount_boundary`("참여자 수와 같은 값은 넘지 않은 것으로 본다.")** | `WS-4` |
| `domain/ParticipantTest` | Unit | as-built + **`changePermission_changesPermission`** / **`canBeManagedBy_sameRankIsRejected`("같은 서열은 서로를 관리할 수 없다.")** / `canBeManagedBy_ownerCanManageAdmin` / `demoteToAdmin`·`promoteToOwner` | `WS-1`·`WS-2` |
| `domain/PermissionTest` | Unit | as-built(`isAtLeast` 서열 3건) | — |
| `domain/InvitationTest` | Unit | **`issue_ownerPermissionIsRejected`(400)** / **`accept_expired`(409)** / `accept_alreadyAccepted`(409) / `cancel_pendingOnly` / `isAcceptable_boundary` | `WS-3` |
| `implement/WorkspaceAccessValidatorTest` | Unit | as-built + **`validateAtLeast_logsWarnOnInsufficientPermission`("서열이 모자라면 감사 로그를 남긴다.")** | `WS-1` |
| `implement/ParticipantPolicyValidatorTest` | Unit | **`validateRemovable_adminCannotRemoveAdmin`(403)** / **`validateRemovable_ownerIsNotRemovable`(409)** / `validateRemovable_adminCanRemoveRegular` | `WS-2` |
| `implement/RuleSetValidatorTest` | Unit | **`validate_exceedsParticipantCount`(400)** / `validate_equalsParticipantCount` | `WS-4` |
| `infra/WorkspaceRepositoryTest` | Repository | as-built | — |
| `infra/ParticipantRepositoryTest` | Repository | as-built + **`countByWorkspaceIdAndDeletedAtIsNull_excludesRemoved`("내보낸 참여자는 정원에 세지 않는다.")** / `findAllByWorkspaceId` | `WS-1`·`WS-3` |
| `infra/InvitationRepositoryTest` | Repository | **`save_tokenIsUnique`** / `findByToken` / `findAllByWorkspaceIdAndStatus` | `WS-3` |
| `infra/adapter/WorkspacePolicyAdapterTest` | Repository | **`requiredReviewerCount_readsRuleSetByType`** / `requiredReviewerCount_workspaceIsDeleted` | `WS-4` |
| `service/WorkspaceServiceTest` | Service | as-built + **`changeRuleSet_exceedsParticipantCount`(400)** / **`changeRuleSet_isReflectedImmediately`("룰셋을 올리면 진행 중인 요청도 새 정족수를 따른다.")** | `WS-4` |
| `service/ParticipantServiceTest` | Service | **`changePermission_actorIsNotOwner`(403)** / **`transferOwnership_demotesPreviousOwner`("소유권을 넘기면 기존 소유자가 관리자로 내려간다.")** / **`transferOwnership_keepsExactlyOneOwner`** / **`remove_publishesEvent`** / `remove_ownerIsNotRemovable`(409) / 접근 검증 3건 | `WS-1`·`WS-2`·`WS-5` |
| `service/InvitationServiceTest` | Service | **`accept_participantLimitExceeded`(409 — "정원 검사는 수락 시점에 한다.")** / **`issue_alreadyParticipant`(409)** / **`issue_duplicatePending`(409)** / `accept_createsParticipantWithGrantedPermission` / `accept_tokenIsUnknown`(404) / 접근 검증 3건 | `WS-3` |
| `presentation/WorkspaceControllerTest` | Controller | as-built + **`changeRuleSet`** / `changeRuleSet_negativeCount`(400) | `WS-4` |
| `presentation/ParticipantControllerTest` | Controller | `readAll` / `changePermission` / `transferOwnership` / `remove` | `WS-1`·`WS-2` |
| `presentation/InvitationControllerTest` | Controller | `issue` / `readAll` / `cancel` / **`accept`(워크스페이스 하위가 아닌 경로)** | `WS-3` |

**`transferOwnership_keepsExactlyOneOwner`와 `accept_participantLimitExceeded`가 이 도메인의 회귀 방지 핵심이다.** 전자는 트랜잭션 경계가 살아 있는지를, 후자는 정원 검사 시점이 발급이 아니라 수락인지를 확인한다.

### Fixture

`workspace/fixture/`에 둔다(src/test 전용). 정적 팩토리 + 내부 `static class XxxBuilder`, 기본값 보유, 체이닝, `build()`는 항상 유효한 객체를 돌려준다.

| Fixture | 변경 |
| --- | --- |
| `WorkspaceFixture` | as-built + **`ruleSet(int, int)` 빌더 메서드 추가**(`WS-4`) |
| `ParticipantFixture` | as-built. **javadoc의 「소유자 외의 권한은 운영 코드에 등록 경로가 아직 없으므로(REQ-WS-003·004) 리플렉션으로 주입한다」를 `WS-1`·`WS-3`으로 경로가 생겼음에 맞춰 고친다** |
| `InvitationFixture` | **신설**(`WS-3`). `status`·`expiresAt`·`inviteeEmail(null)` 주입을 지원한다 |

---

## 12. Phase와 태스크

| Phase | 목표 | 명시적 제외 |
| --- | --- | --- |
| 1 | **룰셋을 바꿀 수단을 만든다** — 리뷰 도메인의 선행(`G-4`) | 참여자 관리 |
| 2 | 참여자 목록·권한 변경·내보내기·소유권 이전 | 초대 |
| 3 | 초대 발급·수락·취소 | — |
| 4 | 규약 정합 — 패키지 구조·이벤트·ID 값 객체 | — |

| ID | 태스크 | 산출물 | 일수 | 의존 |
| --- | --- | --- | --- | --- |
| **WS-4** | **룰셋 수정 + 상한 검증 + 제공 어댑터** | `Workspace.changeRuleSet` + `RuleSet.exceedsParticipantCount` + `RuleSetValidator` + `WorkspaceUpdater` 확장 + 엔드포인트 1 + dto 2 + `WorkspacePolicyAdapter` + ErrorCode 1 + 테스트 6 | 2 | — |
| **WS-1** | 참여자 목록·권한 변경 + 감사 로그 | `ParticipantController`·`ParticipantService` + `ParticipantUpdater` + `ParticipantPolicyValidator` + `Participant` 메서드 3 + `ParticipantReader` 확장 + ErrorCode 1 + `WorkspaceAccessValidator` 로그 + 테스트 7 | 2 | — |
| **WS-2** | 내보내기 + 소유권 이전 | `ParticipantRemover` + `Participant` 메서드 3 + 엔드포인트 2 + ErrorCode 1 + 테스트 5 | 2 | `WS-1` |
| **WS-3** | 초대 발급·수락·취소 | `Invitation`·`InvitationStatus` + `V100` + Repository 1 + implement 4 + `InvitationController`·`InvitationService` + dto 2 + ErrorCode 5 + 테스트 10 | 3 | `WS-1` |
| **WS-5** | 삭제·이탈 이벤트 발행 | 이벤트 record 2 + `WorkspaceRemover`·`ParticipantRemover` 수정 + 테스트 2 | 1 | `WS-2` |
| **WS-6** | 패키지 규약 정리 + ID 값 객체 제거 | dto·model 디렉터리 이동 + `toCommand()` 4 + record 2 삭제 + 테스트 2 삭제 | 1 | `WS-3` |

**`WS-4`를 가장 먼저 한다.** `G-4`(승인 집계는 룰셋 정족수)가 확정되면서 리뷰 도메인의 `RR-3c`·`RR-4b`가 이것을 기다리게 됐다. 룰셋을 바꿀 수단이 없으면 정족수가 영구히 0이라 발행 판정의 「1 이상」 경로를 한 번도 타지 못한다.

**`WS-6`을 마지막에 두는 이유** — 디렉터리 이동이 앞선 태스크의 파일을 전부 건드리므로, 먼저 하면 이후 태스크마다 충돌이 난다.

### Phase별 DoD

**공통 (모든 Phase)**

- [ ] `./gradlew spotlessApply && ./gradlew check` 초록
- [ ] 새 로직에 테스트를 함께 작성했다. 실패하는 테스트를 삭제하거나 조건을 완화하지 않았다
- [ ] `docs/API.md`의 Workspace 절만 갱신했다(공통 규칙 절은 건드리지 않았다)
- [ ] 엔티티를 result·response·이벤트·포트 시그니처에 담지 않았다
- [ ] `implement`에 `@Transactional`을 선언하지 않았다
- [ ] `WorkspaceAccessValidator`의 시그니처를 바꾸지 않았다(5개 도메인이 직접 주입한다)

**WS-4**

- [ ] `RuleSet` compact constructor는 **0 이상만** 검증한다. 상한은 `RuleSetValidator`가 참여자 수를 조회해 검증한다
- [ ] 참여자 수와 **같은 값은 통과**한다(「참여자 수 이하」)
- [ ] `WorkspacePolicyPort` 어댑터가 **`requiredReviewerCount` 하나만** 구현한다(`R-19`). `isParticipant`·`hasAdminPermission`을 만들지 않는다
- [ ] 어댑터가 `participantCount`를 노출하지 않는다 — 실효 정족수는 `ruleSet` 값 그대로다
- [ ] 응답이 `200`이고 저장된 실제 값을 담는다
- [ ] 룰셋 변경이 **스냅샷 없이 즉시 반영**된다(`D-12`·`D-13`)

**WS-1**

- [ ] `canBeManagedBy`가 **같은 서열의 상호 조작을 막는다** — 규칙이 내보내기와 권한 변경 두 곳에 복제되지 않았다
- [ ] 권한 변경이 **Owner 전용**이다(`X-11` 확정)
- [ ] `WorkspaceAccessValidator`가 **403에 WARN 감사 로그를 남긴다**(`Y-06`·`NFR-REV-001`). `[클래스명.메서드명] 영문 문장 key={}` 형식이다
- [ ] 참여자 목록이 배열이다(정원 5명이라 구조적으로 작다)
- [ ] `Participant.permission`에 `updatable = false`를 붙이지 않았다

**WS-2**

- [ ] **Owner는 내보낼 수 없다**(409)
- [ ] Admin은 `REGULAR`만, `ADMIN`을 내보내는 것은 Owner만이다
- [ ] 소유권 이전이 **한 트랜잭션에서 두 참여자를 바꾼다** — `transferOwnership_keepsExactlyOneOwner`가 통과한다
- [ ] 내보내기가 **소프트 삭제**다. 남긴 문서·리뷰·코멘트는 그대로 유지된다
- [ ] 소유권 이전 경로가 권한 변경과 분리돼 있다

**WS-3**

- [ ] **정원 검사가 수락 시점**이다(발급 시점이 아니다) — `accept_participantLimitExceeded`가 통과한다
- [ ] 이미 참여자인 회원에게 발급하지 않는다(409)
- [ ] 같은 워크스페이스·같은 대상에 `PENDING`은 1개다 — **애플리케이션 검증**이고 DB 부분 유니크에 기대지 않는다
- [ ] 초대로 `OWNER`를 부여할 수 없다(400)
- [ ] `token`이 전역 유일이고 URL-safe다
- [ ] **수락 경로가 `/api/invitations/{token}/accept`**다 — 워크스페이스 하위가 아니다
- [ ] 목록 응답에 `token`을 담지 않는다(발급 응답에만 담는다)
- [ ] 만료를 배치가 아니라 조회·수락 시점에 판정한다

**WS-5**

- [ ] 이벤트가 식별자·원시값·시각만 담은 불변 record다
- [ ] `ParticipantRemovedEvent`가 **`memberId`를 담는다**(`participantId`가 아니다) — 리뷰 도메인이 `Reviewer.memberId`로 매칭한다(`O-2`)
- [ ] `implement`가 `EventPublisher` 포트만 주입받는다
- [ ] 구독자가 없어도 발행이 성립한다
- [ ] `CONFLICTS.md`의 `O-2`를 「해소」로 갱신했다

**WS-6**

- [ ] dto가 `presentation/dto/`, command·result가 `service/model/`에 있다
- [ ] `Request.toCommand()` 패턴을 쓴다 — 컨트롤러가 `new XxxCommand(...)`를 조립하지 않는다
- [ ] `WorkspaceId`·`ParticipantId`와 두 테스트가 사라졌다
- [ ] 다른 도메인이 이 record들을 참조하지 않는다

### 통합 태스크 (6개 도메인 공통)

| ID | 내용 | 의존 |
| --- | --- | --- |
| `T-DOC-1` | **문서 선행 수정** — `CONFLICTS.md` 9절이 파일별 목록을 갖는다. **이 도메인은 `ARCHITECTURE.md` 예외 조항(`D-19`)이 여기 들어간다** | — (**모든 구현의 선행**) |
| `T-CMN-1` | `PageResponse`·`PageResult` 신설 | `T-DOC-1` |
| ~~`T-INT-1`~~ | ~~`spring.flyway.out-of-order=true` 정리~~ — **폐기**(2026-09-12). 개발 브랜치 DB를 항상 리셋하므로 이력이 비어 있고 전체가 버전 순서대로 한 번에 적용된다. **`V2`가 대역 밖이어도, `WS-3`이 `V100`을 나중에 머지해도 거부되지 않는다**(`Y-10`) | — |
| `T-INT-2` | 크로스 도메인 어댑터를 `real`로 전환 | 6개 도메인 Phase 3 |
| `T-INT-3` | `SecurityConfig` + 인증 주체 + 프로파일 분리 | 인증 도메인(별건) |

**`T-INT-3`이 이 도메인에 미치는 영향이 가장 크다.** 컨트롤러 5곳의 `memberId` 파라미터가 사라지고 인증 주체에서 해석된다 — `WorkspaceController` javadoc이 "바꿀 지점은 이 클래스의 파라미터 5곳뿐이고 service 이하는 손대지 않는다"고 이미 적었다. `WS-1`~`WS-3`이 컨트롤러를 늘리므로 **그 수가 함께 늘어난다.**

---

## 13. 리스크와 열린 질문

| 항목 | 내용 | 대응 |
| --- | --- | --- |
| **`WorkspaceAccessValidator`가 5개 도메인의 단일 실패점이다** | `D-19`로 포트를 거치지 않으므로 시그니처 변경이 곧 5개 도메인 컴파일 오류다. 반대로 **컴파일이 그것을 즉시 알려주는 것이 이 선택의 이득**이기도 하다 | 10절 「공유 자산과 주인」에 못박고 12절 공통 DoD에 「시그니처를 바꾸지 않았다」를 넣었다. 변경이 필요하면 통합 태스크로 넘긴다 |
| **`WS-4`가 늦으면 리뷰 도메인이 검증 불가 상태로 진행된다** | 정족수가 0인 채로 `RR-3c`·`RR-4b`를 만들면 발행 조건의 「1 이상」 경로가 테스트되지 않고, 나중에 룰셋이 들어올 때 드러난다 | `EXECUTION_ORDER.md` 순차 순서에서 `WS-4`를 `RR-1`보다 앞에 두었다. 12절이 이 도메인의 1번 태스크로 고정했다 |
| **정원 5명이 룰셋 상한과 얽혀 있다** | 정원이 바뀌면 룰셋 상한도 바뀐다. 두 숫자가 `DOMAIN.md`에 하드코딩돼 있다 | 정원을 `Participant`의 `public static final MAX_PARTICIPANTS`로 두고 `RuleSetValidator`가 참여자 **실제 수**를 조회하게 한다 — 정원이 바뀌어도 상한 검증은 그대로 동작한다 |
| **소유권 이전 중 동시 요청** | 두 Owner 후보에게 동시에 이전하면 Owner가 둘이 될 수 있다. `participant`에 「Owner는 1명」을 DB로 지킬 유니크가 없다 | 트랜잭션 경계가 1차 방어다. `dictionary`의 `active_flag` 기법(생성 컬럼 + 유니크)을 `permission = 'OWNER'`에 적용할 수 있으므로, **`WS-2`에서 `V110`으로 추가할지 검토한다** |
| **초대 토큰이 URL에 노출된다** | 링크 복사 방식이므로 토큰이 브라우저 히스토리·로그에 남는다 | `expiresAt`으로 수명을 제한하고, **`LOG.md` «민감 정보 규칙»에 따라 토큰을 로그에 남기지 않는다**(인증 토큰과 같은 취급). 12절 `WS-3` DoD에 넣지 않았으므로 리뷰에서 확인한다 |
| **`Participant.leftAt`을 두지 않아 재초대 이력이 섞인다** | 내보낸 뒤 다시 초대하면 소프트 삭제된 행과 새 행이 함께 남는다. `uk_participant_workspace_member`가 `(workspace_id, member_id)`라 **두 번째 참여가 유니크에 걸린다** | **`WS-3`에서 재초대 경로를 정한다** — 소프트 삭제된 행을 되살리는(`deletedAt = null` + 새 `joinedAt`) 방식이 유니크와 맞는다. 13절 열린 질문으로 남긴다 |
| **`X-08` traceId가 이 도메인 응답에도 걸린다** | `NFR-CMN-003`이 `code·message·traceId`를 요구하는데 `ErrorResponse`가 2필드다 | `T-INT-3`에서 공통으로 처리한다. 이 도메인은 `ErrorResponse`를 고치지 않는다 |

### 열린 질문

- **재초대 시 소프트 삭제된 참여자 행을 되살릴지 새로 만들지** — `uk_participant_workspace_member`가 `(workspace_id, member_id)`라 새로 만들면 유니크에 걸린다. **되살리는 방식**(`deletedAt = null`, `permission`·`joinedAt`·`createdBy` 갱신)이 스키마와 맞고 `V2`를 고치지 않아도 된다. `WS-3`에서 확정한다.
- **「Owner는 정확히 1명」을 DB로 지킬지** — `dictionary`의 생성 컬럼 기법(`case when permission = 'OWNER' then 1 else null end`)을 쓰면 소유권 이전의 동시성까지 막힌다. 다만 **소프트 삭제된 Owner 행이 있으면 유니크가 걸린다** — Owner는 내보낼 수 없으므로 실제로는 발생하지 않지만, 워크스페이스를 삭제해도 참여자 행이 남는 것과 겹친다. `WS-2`에서 판단한다.
- **참여자 목록에 회원 이름을 담을지** — 담으려면 `member` 도메인 조회 포트가 필요하고, 그러면 「소비 포트 없음」이 깨진다. **MVP1은 `memberId`만 내리고 화면이 회원 API로 채운다**로 두되, 화면 요구가 확정되면 재검토한다.

### DOMAIN.md 수정 (`T-DOC-1`이 반영)

1. `Participant` 표의 `permission`에 **변경 가능함**을 명시(`WS-1`). 「권한 변경 주체는 Owner 전용」을 «권한 설정» 정책에 확정으로 적는다(`X-11`)
2. «모델 반영 필요(미확정)»에서 **「참여자 권한 변경 주체」와 「참여자 삭제 방식」을 해소로 옮긴다** — 전자는 Owner 전용, 후자는 소프트 삭제(`deletedAt`)
3. `Invitation` 표에 **`token` 길이·`inviteeEmail` null 조건**을 구체화(`varchar(64)`, `varchar(320)`)
4. «리뷰 규칙» 정책에 **상한 검증 주체가 이 도메인임**을 이미 적었으므로 유지. `RuleSetValidator`가 그 자리임을 각주로 덧붙인다
5. 속성 표의 `WorkspaceId`·`ParticipantId`·`InvitationId`에 **「개념 표기」 각주**(`D-25`)

### ARCHITECTURE.md 수정 (`T-DOC-1`이 반영)

«크로스 도메인 조회 — 포트와 어댑터»에 **접근 검증 예외 조항**을 추가한다(`D-19`).

> **워크스페이스 접근 검증은 예외다.** 다른 도메인은 `workspace/implement/WorkspaceAccessValidator`를 **직접 주입한다.** 「비참여자에게 404」 정책과 `Permission` 서열 판정이 한 곳에 남아야 하고, `validateAtLeast`가 예외를 던지는 방식이 `boolean`을 반환하는 포트와 맞지 않기 때문이다. 룰셋 값처럼 **상태를 읽는 조회는 포트를 쓴다.**

### 리뷰 체크리스트 대조

| 출처 | 항목 | 이 문서 반영 위치 |
| --- | --- | --- |
| `ARCHITECTURE.md` | service가 Repository·기술 객체를 직접 참조하지 않는가 / 레이어 건너뛰기 / 도메인 간 직접 참조 / 이벤트 페이로드 | 9절, 12절 DoD |
| `TEST.md` | 계층별 방식 / Fixture / AssertJ / `@Transactional` 금지 | 11절 |
| `LOG.md` | prefix 규약 / 민감 정보(초대 토큰) / 레벨 | 12절 `WS-1` DoD, 13절 리스크 |
| `API.md` | 페이징 예외 / 에러 형식 / 요청자 식별 / 404 정책 | 7절 |
| `EXCEPTION.md` | `{DOMAIN}_{REASON}` / 중복 코드 금지 / 던지는 위치 | 8절 |
