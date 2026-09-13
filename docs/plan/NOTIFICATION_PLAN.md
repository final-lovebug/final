# Notification 구현 계획

2026-09-13 작성. 다른 6개 계획 문서와 같은 13절 목차를 쓴다. **이 문서는 큰 흐름 확정(2026-09-10) 이후에 쓰였으므로 `R-*`에 뒤집힌 서술이 없다.**

결정 원장은 `docs/plan/CONFLICTS.md`이고 이 도메인의 결정은 `D-47`~`D-52`다. 실행 순서 규약은 `docs/plan/EXECUTION_ORDER.md`를 따른다.

---

## 1. 범위와 목표

리뷰어들이 리뷰를 마쳐 새 문서·사전집 개정안이 반영되면 관계자에게 알린다. 6개 도메인이 `DomainEvent` record 15종을 계약으로만 만들어 뒀고 소비자가 하나도 없는 상태(`X-12`)에서, **Notification이 그 첫 소비자가 된다.**

동시에 `D-24`가 정해 둔 「로컬·테스트 인메모리 / AWS 배포 SQS」 2-어댑터 구조를 처음으로 실제 코드로 완성한다. 그전까지 `app.messaging.mode`를 읽는 코드가 0줄이었다(`Y-28`).

충족하는 요구사항 — `REQ-NTF-004`(목록·읽음) · `REQ-NTF-008`(리뷰 진행 알림) · `REQ-NTF-009`(수신 설정) · `NFR-NTF-001`(채널 추상화) · `NFR-NTF-002`(중복 방지) · `NFR-NTF-003`(비동기 생성).

### MVP1 범위 밖

- **발행부.** `RR-4a`~`4d`(리뷰 판정 제출·정족수 집계·반영·이벤트 발행)는 이 도메인이 하지 않는다. **`RR-4d`가 머지돼 발행부가 이미 붙었으므로 알림은 지금 실제로 흐른다.** 이 도메인은 소비 측만 맡는다
- **`EMAIL`·`SLACK` 채널 발송.** `REQ-NTF-005`·`006`이 MVP2다. 채널 어댑터 **인터페이스는 지금 넣고**(`NFR-NTF-001`) 구현체는 `IN_APP` 하나다
- **추출·대조 완료 알림**(`REQ-NTF-001`). `ExtractionJob`·`CheckJob`이 폴링이라(`D-34`) 발행할 이벤트가 없다
- **코멘트 등록 알림.** `Comment` 엔티티는 `RR-4a`로 들어왔지만 등록을 알리는 이벤트가 없다
- **프론트엔드 알림 패널.** `frontend/docs/ARCHITECTURE.md`가 Phase 5~6으로 미뤄 뒀다
- **LocalStack.** 브로커를 띄운 왕복 검증은 하지 않는다. 대신 **발행 측이 만든 그대로의 문자열을 수신 측에 넣는** 테스트로 우리 코드의 경계까지 덮는다 — 큐는 문자열을 옮길 뿐이다. 실제 AWS 동작(가시성 타임아웃·DLQ)은 배포 준비 시점에 검증한다. **표준 큐를 쓰므로 순서 보장은 애초에 없다**(`D-53`)
- **Outbox**(`NFR-MSG-002`). `@TransactionalEventListener(AFTER_COMMIT)`이 로컬 원자성을 대신한다는 기존 판단 그대로

---

## 2. 결정 대기표

### 2-1. 확정된 결정

`CONFLICTS.md` 3-2절의 `D-47`~`D-52`를 그대로 따른다. ID로만 참조한다.

| ID | 한 줄 요약 |
| --- | --- |
| `D-47` | Notification을 7번째 도메인으로 신설. MVP1 유형은 리뷰 5종 |
| `D-48` | 태스크 접두사 `NT-`, Flyway 대역 700~799 |
| `D-49` | `title`·`read` 신설. CTA 문구와 이동 경로는 저장하지 않는다 |
| `D-50` | 알림 설정은 워크스페이스 단위 |
| `D-51` | 멱등은 DB 유니크 키 |
| `D-52` | SQS 발행 어댑터는 `common/infra/event/sqs/` |

#### 수신자 규칙 — 이 도메인의 핵심 정책

| 유형 | 수신자 |
| --- | --- |
| `REVIEW_REQUEST_RECEIVED` | **지정된 리뷰어.** 지정된 리뷰어가 없으면 알림을 만들지 않는다 |
| `APPROVED` | 요청자 |
| `CHANGES_REQUESTED` | 요청자 |
| `REVISED` | **워크스페이스 참여자 전원** |
| `CANCELED` | 요청자 |

**모든 유형에 자기 알림 억제를 건다** — 수신자와 행위자가 같으면 행을 만들지 않는다. `ReviewRequest.cancel`이 현재 요청자 본인만 허용하므로 `CANCELED`는 지금 0건이 나오고, `RR-4c`가 Admin 취소를 열면 의미가 생긴다.

`G-4`가 「리뷰어 지정은 알림·필터 용도이고 미지정 참여자도 정족수에 산입된다」고 한 것과 모순되지 않는다 — **지정은 알림을 누구에게 보낼지를 정할 뿐 리뷰 자격을 제한하지 않는다.** 지정되지 않은 참여자는 알림 없이도 목록에서 요청을 보고 리뷰할 수 있다.

### 2-2. 남은 결정 대기

- **`REVISED`의 「전원」 범위.** 워크스페이스 참여자 전원으로 해석했다. 「요청자 + 리뷰어 전원」이 의도였다면 `NotificationRecipientResolver` 한 메서드만 바뀐다
- **알림 보존 기간.** 지금은 무한 보관이고 소프트 삭제 경로도 열어 두지 않았다. 행이 쌓이면 정리 정책이 필요하다
- **`EMAIL`·`SLACK` 어댑터 구현 시점.** MVP2. 워크스페이스별 웹훅 URL을 어디에 둘지가 함께 정해져야 한다

---

## 3. 도메인 모델

`DOMAIN.md` «Notification» 절의 속성 표가 기준이다. 아래는 구현 수준 표이며 **`nullable` 칸은 `X`가 NOT NULL, `O`가 nullable**이다(`DOMAIN.md`의 `필수` 칸과 반대).

### Notification

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | IDENTITY |
| `recipientId` | `Long` | `recipient_id` | X | `updatable = false` |
| `workspaceId` | `Long` | `workspace_id` | X | `updatable = false` |
| `type` | `NotificationType` | `type` | X | `varchar(30)`, STRING |
| `targetType` | `NotificationTargetType` | `target_type` | X | `varchar(20)`, STRING |
| `targetId` | `Long` | `target_id` | X | |
| `title` | `String` | `title` | X | `varchar(255)` |
| `message` | `String` | `message` | X | `varchar(500)` |
| `channels` | `Set<NotificationChannel>` | 별도 테이블 | X | `@ElementCollection` + `@BatchSize(100)` |
| `dedupeKey` | `String` | `dedupe_key` | X | `varchar(200)`, `updatable = false` |
| `read` | `boolean` | `is_read` | X | **`read`는 MySQL 예약어라 컬럼명이 다르다** |
| `readAt` | `OffsetDateTime` | `read_at` | O | `read == (readAt != null)` |
| `createdBy` | `Long` | `created_by` | **O** | 행위자. 시스템 발행이면 null |

### NotificationSetting

| 필드 | 타입 | 컬럼 | nullable | 비고 |
| --- | --- | --- | --- | --- |
| `id` | `Long` | `id` | X | |
| `workspaceId` | `Long` | `workspace_id` | X | `updatable = false` |
| `type` | `NotificationType` | `type` | X | `updatable = false` |
| `channels` | `Set<NotificationChannel>` | 별도 테이블 | X | 비면 그 유형은 알림을 만들지 않는다 |
| `createdBy` | `Long` | `created_by` | X | |

### 애그리게이트 경계

`Notification`과 `NotificationSetting`은 **서로 참조하지 않는다.** 설정은 알림을 만들지 말지를 가르는 입력이고, 만들어진 알림은 그 시점의 채널 목록을 자기 안에 복사해 갖는다 — 나중에 설정이 바뀌어도 과거 알림의 채널은 변하지 않는다.

다른 도메인과는 **연관 매핑 없이 FK id 필드**로만 잇는다(기존 6개 도메인과 같다). `recipientId`는 `member`를, `workspaceId`는 `workspace`를 가리키지만 DB FK도 걸지 않는다 — 워크스페이스가 소프트 삭제돼도 알림 이력은 남는다.

### enum

```java
NotificationType    { REVIEW_REQUEST_RECEIVED, APPROVED, CHANGES_REQUESTED, REVISED, CANCELED }
NotificationTargetType { DOCUMENT, DICTIONARY, REVIEW_REQUEST }
NotificationChannel { IN_APP, EMAIL, SLACK }
```

`NotificationChannel`은 프론트 `shared/types/common.ts`의 `NotificationChannel`과 값이 정확히 같다. 프론트 설정 화면의 네 번째 항목(`KAKAO`/「웹 push」)은 **화면 전용 키이고 서버 enum에 없다.**

---

## 4. 상태 전이와 도메인 메서드

알림은 상태 기계가 아니다. 축은 읽음 하나뿐이다.

| from | to | 트리거 | 조건 |
| --- | --- | --- | --- |
| (신규) | 안 읽음 | `create(...)` | `read = false`, `readAt = null` |
| 안 읽음 | 읽음 | `markRead()` | — |
| 읽음 | 읽음 | `markRead()` | **no-op.** `readAt`이 처음 읽은 시각 그대로 유지된다 |

### 도메인 메서드 시그니처

```java
// Notification
static Notification create(Long recipientId, Long workspaceId, NotificationType type,
                           NotificationTargetType targetType, Long targetId,
                           String title, String message, Set<NotificationChannel> channels,
                           String dedupeKey, Long createdBy);
void markRead();          // 멱등. read와 readAt을 함께 바꾼다
boolean isRead();

// NotificationSetting
static NotificationSetting createDefault(Long workspaceId, NotificationType type, Long createdBy);
void changeChannels(Set<NotificationChannel> channels);
boolean isEnabledFor(NotificationChannel channel);
boolean isMuted();        // channels가 비었음
```

**`read`와 `readAt`을 따로 세팅하는 경로를 만들지 않는다.** 둘이 어긋나면 목록의 읽음 표시와 읽은 시각이 서로 다른 말을 하게 된다. `markRead()` 하나만 둔다.

---

## 5. 스키마와 Flyway

대역 700~799(`D-48`). 도메인 내부는 10 단위로 증가시킨다.

| 파일 | 내용 |
| --- | --- |
| `V700__create_notification.sql` | `notification` + `notification_channel` |
| `V710__create_notification_setting.sql` | `notification_setting` + `notification_setting_channel` |

핵심 제약 둘.

- `uq_notification_recipient_dedupe (recipient_id, dedupe_key)` — **멱등의 근거**(`D-51`). at-least-once 재수신이 두 번째 insert에서 유니크 위반으로 튕긴다
- `uq_notification_setting_workspace_type (workspace_id, type)` — 워크스페이스당 유형별 1행(`D-50`)

조회 인덱스는 `idx_notification_recipient (recipient_id, workspace_id, is_read, deleted_at)` 하나다. 목록(최신순 페이징)과 미읽음 수가 모두 이 앞쪽 컬럼을 탄다.

**다른 도메인 테이블에 FK를 걸지 않는다.** 워크스페이스·회원이 소프트 삭제돼도 알림 이력은 남아야 하고, FK를 걸면 대역을 넘나드는 마이그레이션 순서 의존이 생긴다.

---

## 6. 패키지와 파일

```
notification
├── domain
│   ├── Notification · NotificationSetting
│   └── NotificationType · NotificationTargetType · NotificationChannel
├── exception
│   └── NotificationErrorCode
├── implement
│   ├── NotificationReader · NotificationAppender · NotificationUpdater
│   ├── NotificationSettingReader · NotificationSettingWriter
│   ├── NotificationRecipientResolver      ← 수신자 규칙이 사는 한 곳
│   ├── NotificationDedupeKeyFactory
│   ├── NotificationMessageFactory
│   └── NotificationChannelDispatcher      ← NFR-NTF-001
├── infra
│   ├── NotificationRepository · NotificationSettingRepository
│   ├── port
│   │   ├── ReviewRequestQueryPort · ReviewRequestSnapshot
│   │   ├── WorkspaceQueryPort
│   │   └── NotificationSender             ← 채널 어댑터 인터페이스
│   ├── adapter
│   │   ├── ReviewRequestQueryAdapter · ReviewRequestQueryStub
│   │   ├── WorkspaceQueryAdapter · WorkspaceQueryStub
│   │   └── InAppNotificationSender
│   └── event
│       ├── InMemoryNotificationEventListener   ← @TransactionalEventListener + @Async
│       └── SqsNotificationEventListener        ← @SqsListener
├── service
│   ├── NotificationService · NotificationSettingService
│   ├── NotificationEventHandler           ← 기술 무관 공용 핸들러
│   └── model/*Command · *Result · *Query
└── presentation
    ├── NotificationController · NotificationSettingController
    └── dto/*Request · *Response
```

`common/infra/event/sqs/`에 `SqsEventPublisher`·`EventEnvelope`가 더해진다(`D-52`).

---

## 7. API 명세

`docs/API.md`의 `# **Notification API**` 절이 정본이다. 요약만 옮긴다.

| Method | Path | 권한 |
| --- | --- | --- |
| GET | `/api/workspaces/{workspaceId}/notifications` | 참여자 |
| GET | `/api/workspaces/{workspaceId}/notifications/unread-count` | 참여자 |
| PATCH | `/api/workspaces/{workspaceId}/notifications/{notificationId}/read` | 수신자 본인 |
| PATCH | `/api/workspaces/{workspaceId}/notifications/read-all` | 참여자 |
| GET | `/api/workspaces/{workspaceId}/notification-settings` | 참여자 |
| PATCH | `/api/workspaces/{workspaceId}/notification-settings` | ADMIN 이상 |

**`POST`가 없다.** 알림은 이벤트로만 만들어진다.

### 권한

참여 여부와 서열은 `WorkspaceAccessValidator`를 직접 주입해 검사한다(`D-19` 예외). 비참여 워크스페이스는 `404`다.

**수신자 본인 확인은 워크스페이스 검증과 별개다.** 같은 워크스페이스 참여자라도 남의 알림은 볼 수 없고, 이때도 `403`이 아니라 `404`를 준다 — `403`을 주면 그 알림의 존재가 드러난다.

### 요청자 식별 — 임시 방식

컨트롤러가 `@RequestParam Long memberId`를 받고 클래스 javadoc에 `TODO(NFR-USR-001)`을 단다. `T-INT-3`이 전부 한 번에 걷어낸다.

---

## 8. ErrorCode

| 코드 | 상태 | 메시지 |
| --- | --- | --- |
| `NOTIFICATION_NOT_FOUND` | 404 | 알림을 찾을 수 없습니다. |
| `NOTIFICATION_INVALID_CHANNEL` | 400 | 지원하지 않는 알림 채널입니다. |

**「내 알림이 아님」에 별도 코드를 두지 않는다.** `NOTIFICATION_NOT_FOUND`를 그대로 쓴다 — 존재를 숨기는 것이 목적이므로 코드가 갈리면 그 자체로 정보가 샌다.

권한 부족은 `WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED`를 재사용한다. 검증 주체가 `WorkspaceAccessValidator`이므로 같은 뜻의 코드를 도메인마다 늘리지 않는다(`document`·`dictionary`와 같은 판단).

---

## 9. 크로스 도메인 계약

### 소비 포트

어댑터는 **소비 도메인**인 `notification/infra/adapter/`에 둔다(`D-33`). 제공 도메인의 `infra`(Repository)만 참조한다.

| 포트 | 메서드 | 프로퍼티 |
| --- | --- | --- |
| `ReviewRequestQueryPort` | `findSnapshot(Long)` · `reviewerMemberIds(Long)` | `app.crossdomain.review-request.mode` (신규) |
| `WorkspaceQueryPort` | `participantMemberIds(Long)` | `app.crossdomain.workspace.mode` (기존 재사용) |

`ReviewRequestSnapshot`은 `(Long workspaceId, ReviewRequestType type, String title, Long requesterId)` record다. **포트 시그니처에 엔티티를 넣지 않는다.**

### 제공 포트

없다. 다른 도메인이 알림을 조회할 일이 없다.

### 수신 이벤트

| 이벤트 | 만드는 알림 |
| --- | --- |
| `ReviewRequestCreatedEvent` | `REVIEW_REQUEST_RECEIVED` |
| `ReviewSubmittedEvent` (verdict=APPROVED) | `APPROVED` |
| `ReviewRequestChangesRequestedEvent` | `CHANGES_REQUESTED` |
| `ReviewRequestRevisedEvent` | `REVISED` |
| `ReviewRequestCanceledEvent` | `CANCELED` |

**이벤트 5종 중 `workspaceId`를 가진 것은 `ReviewRequestCreatedEvent` 하나뿐이다.** 나머지는 `reviewRequestId`만 담아 워크스페이스·요청자·제목을 포트로 되짚어 조회한다 — `ARCHITECTURE.md`의 「상태가 필요한 컨슈머는 식별자로 다시 조회한다」가 이것을 허용한다.

### 발행 이벤트

없다. 이 도메인은 순수 소비자다.

---

## 10. 공유 파일 규약

| 파일 | 규약 |
| --- | --- |
| `docs/API.md` | 파일 **끝**에 `# **Notification API**` 절만 추가. 공통 규칙·페이징·버저닝·에러 형식 절은 건드리지 않는다 |
| `application.yml` (main·test **양쪽**) | `app.crossdomain.review-request.mode`·`app.messaging.sqs.queue`·**`spring.cloud.aws.region.static`** 추가. **테스트 파일이 main을 대체하므로 한쪽만 고치면 컨텍스트가 깨진다** |
| `build.gradle` | `spring-cloud-aws-starter-sqs` 한 줄. BOM이 이미 있어 버전은 없다. **사전 승인 완료**. **리전 기본값을 함께 넣어야 한다** — 스타터가 spring-cloud-aws 오토컨피그를 켜면 SDK 클라이언트(`ssmClient` 포함)가 빈 생성 시점에 리전을 요구하고, 없으면 알림과 무관한 기존 `@SpringBootTest`까지 전부 깨진다 |
| `common/infra/event/sqs/**` | `D-52`의 명시적 예외. 기존 `common` 파일을 고치지 않고 새 패키지를 더한다 |

### 수정 금지 파일

`common/**`(위 예외 제외), `common/domain/BaseEntity`, `backend/src/test/java/.../support/**`, **다른 도메인의 패키지 전체**(위 예외 제외).

---

## 11. 테스트 계획

`docs/TEST.md`를 따른다 — RestAssuredMockMvc 필수, AssertJ만, `@DisplayName`은 「…한다.」로 끝나는 한글.

| 클래스 | 베이스 | 필수 케이스 |
| --- | --- | --- |
| `NotificationTest` | 순수 단위 | `markRead` 멱등, `read == (readAt != null)` 불변식 |
| `NotificationSettingTest` | 순수 단위 | 채널 교체, `isMuted` |
| `NotificationDedupeKeyFactoryTest` | 순수 단위 | 같은 이벤트 → 같은 키, 버전·회차가 다르면 다른 키 |
| `NotificationRecipientResolverTest` | 순수 단위(포트 모킹) | 유형별 수신자 5종, **리뷰어 미지정 시 빈 목록**, 자기 알림 억제 |
| `NotificationMessageFactoryTest` | 순수 단위 | 유형별 title·message |
| `NotificationRepositoryTest` | `RepositoryTestSupport` | **중복 dedupeKey insert가 거부된다**, 미읽음 수 |
| `NotificationSettingRepositoryTest` | `RepositoryTestSupport` | `(workspaceId, type)` 유니크 |
| `ReviewRequestQueryAdapterTest` · `WorkspaceQueryAdapterTest` | `RepositoryTestSupport` | 스냅샷 매핑, 소프트 삭제 제외 |
| `NotificationEventHandlerTest` | `IntegrationTestSupport` | **같은 이벤트를 두 번 처리해도 알림이 1건이다**(`NFR-NTF-002`) |
| `NotificationServiceTest` | `IntegrationTestSupport` | 이벤트 발행 → `AFTER_COMMIT` + `@Async` → 행 생성 end-to-end |
| `NotificationControllerTest` · `NotificationSettingControllerTest` | `@WebMvcTest` | 목록·읽음·모두읽음·미읽음수, **남의 알림 404** |
| `EventPublisherConditionTest` | `ApplicationContextRunner` | `in-memory`/`sqs` 중 **하나만** 뜬다 |

**`IntegrationTestSupport`를 상속한 클래스에 `@Transactional`을 붙이지 않는다.** 붙이면 테스트 트랜잭션이 롤백으로 끝나 `@TransactionalEventListener(AFTER_COMMIT)`이 영원히 실행되지 않는다 — 이 도메인은 그 리스너가 곧 본체라 테스트가 통째로 무의미해진다.

### Fixture

`notification/fixture/NotificationFixture` · `NotificationSettingFixture`. 체이닝 빌더에 기본값을 두고 `ReflectionTestUtils`로 `id`를 주입한다.

---

## 12. Phase와 태스크

| 태스크 | 티켓 | 내용 | 의존 |
| --- | --- | --- | --- |
| `NT-1` | WLSH-153 | 문서 + 도메인 엔티티 + Flyway | — |
| `NT-2` | WLSH-154 | 크로스 도메인 포트·어댑터 + 이벤트 핸들러 + 채널 디스패처 | `NT-1` |
| `NT-3` | WLSH-156 | API (목록·읽음·미읽음수·설정) | `NT-2` |
| `NT-4` | WLSH-157 | SQS 발행·수신 어댑터 | `NT-2` |

브랜치는 `feat/WLSH-{티켓}-notification-phase-{번호}`. **Phase 0 문서 커밋은 `NT-1` 브랜치의 첫 커밋으로 얹는다** — 별도 티켓이 없고, 문서가 코드보다 먼저라는 순서는 커밋 순서로 지킨다.

### Phase별 DoD

- **`NT-1`** — 문서 7개가 갱신됐고, 엔티티 2개와 enum 3개가 있고, Flyway 2개가 `RepositoryTestSupport`에서 실제로 적용되며, 도메인 단위 테스트가 통과한다
- **`NT-2`** — 이벤트 5종을 받아 알림이 생기고, **같은 이벤트를 두 번 줘도 1건이며**, 리뷰어 미지정 요청은 알림을 만들지 않는다
- **`NT-3`** — 6개 엔드포인트가 동작하고, 남의 알림이 404이며, 설정이 첫 조회에 기본값으로 만들어진다
- **`NT-4`** — `app.messaging.mode`로 두 발행 어댑터가 배타 선택되고, 두 수신 어댑터가 **같은 핸들러**에 위임한다

`NT-3`과 `NT-4`는 서로 의존하지 않아 병렬 가능하다.

---

## 13. 리스크와 열린 질문

### 리스크

- **이벤트 시그니처가 바뀌면 핸들러가 함께 바뀐다.** `RR-4d`가 머지되며 5종의 컴포넌트가 모두 달라졌고(`ReviewSubmittedEvent`는 `reviewerMemberId` → `memberId`에 `targetRound`가 붙었다) 핸들러·dedupe 키를 그에 맞춰 고쳤다. 이벤트는 도메인 간 계약이므로 같은 일이 또 생길 수 있다 — 컴파일이 잡아 주지만 **dedupe 키 파생이 바뀌면 과거 알림과 중복 판정이 어긋난다**
- **`@Async` 리스너의 실패는 호출자에게 전파되지 않는다.** `AsyncEventConfig`의 `AsyncUncaughtExceptionHandler`가 ERROR 로그로만 남긴다. 알림이 조용히 누락돼도 본 작업은 성공으로 보인다 — 이것은 **의도된 트레이드오프**다(알림 실패가 리뷰 반영을 되돌리면 안 된다)
- **SQS 경로가 실제 메시지로 검증되지 않는다.** LocalStack이 없어 직렬화 실패·`LazyInitializationException`이 배포에서 처음 드러날 수 있다. `ARCHITECTURE.md`가 「이 규약은 로컬에서 검증되지 않으므로 리뷰에서 확인한다」고 한 지점이다 — **이벤트 record에 엔티티가 섞이지 않았는지 리뷰에서 본다**
- **`REVISED`가 참여자 전원에게 가면 정원 5명 기준으로도 반영 1건에 알림 5행이 생긴다.** 지금 규모에서는 문제가 아니지만 보존 정책과 함께 다시 볼 값이다

### 열린 질문

- 알림 보존 기간과 정리 정책
- `REVISED` 수신자 범위(2-2절)
- 워크스페이스가 소프트 삭제되면 그 워크스페이스의 알림을 어떻게 할지 — 지금은 남는다. `WorkspaceDeletedEvent`를 구독해 정리할 수 있지만 이번 범위 밖이다

### DOMAIN.md 수정

`NT-1`에서 이미 반영했다 — `Notification` 표에 `title`·`read`·`dedupeKey` 추가, `NotificationSetting` 표 신설, 「모델 반영 필요(미확정)」의 **알림 설정 모델** 항목 해소, 관계 표 3행 추가.
