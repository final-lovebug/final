# **Architecture Convention**

## **목표**

프로젝트는 **도메인 중심의 레이어드 아키텍처를 사용**한다. **핵심 목표는 service가 상세 구현을 알지 않고도 비즈니스 흐름을 설명할 수 있게 만드는 것**이다. 신규 입사자, 기획자, 운영 담당자가 service 메서드를 읽었을 때 대략적인 업무 흐름을 이해할 수 있어야 한다.

이 문서는 Gemini Kim의 글 **지속 성장 가능한 소프트웨어를 만들어가는 방법**의 방향성을 프로젝트 컨벤션으로 구체화한 것이다.

## **기본 패키지 구조**

도메인을 최상위 기준으로 나누고, 도메인 내부에서 레이어를 나눈다.

```java
com.example.project
└── payment
    ├── exception
    ├── presentation
    ├── service
    ├── implement
    ├── infra
    └── domain
```

| **패키지** | **역할** |
| --- | --- |
| exception | 도메인 Exception, ErrorCode 예외 모음 |
| presentation | HTTP 요청/응답, Controller, API DTO, 인증 사용자 해석 |
| service | 비즈니스 흐름 조립, 유스케이스 단위 트랜잭션 경계 |
| implement | 비즈니스 흐름을 구성하는 상세 구현 도구 |
| infra | 저장소 접근(Repository), 외부 API·캐시·메시징 기술 격리 |
| domain | 도메인 모델(JPA 엔티티 겸용), 값 객체, 정책, 상태 전이 규칙 |

## **의존성 방향**

```java
presentation -> service -> implement -> infra
                         -> domain
```

1. 상위 레이어는 하위 레이어만 참조한다.
2. 하위 레이어는 상위 레이어를 참조하지 않는다.
3. 레이어를 건너뛰지 않는다. 예를 들어 service가 infra를 직접 참조하지 않는다.
4. 동일 레이어 간 참조는 피한다. 단, implement는 협력 도구 성격이 강하므로 필요하면 같은 도메인 안에서만 참조할 수 있다.
5. 다른 도메인을 직접 참조해야 한다면 먼저 도메인 간 의존 방향과 공개 API를 합의한다.
6. infra는 domain을 참조한다(Repository가 도메인 모델을 다루므로). 반대 방향(domain -> infra)은 금지한다. 공통 매핑 상위 클래스도 domain에 둔다.

### **크로스 도메인 조회 — 포트와 어댑터** (2026-09-10 합의)

규칙 5의 합의 결과다. 다른 도메인의 상태를 읽어야 할 때는 다음 형태로만 한다.

- **포트는 소비 도메인이 정의한다.** `{소비도메인}/infra/port/`에 인터페이스를 두고 필요한 메서드만 자기 언어로 선언한다. 소비자마다 필요한 것이 다르므로 제공 도메인이 포트를 정의하지 않는다.
- **어댑터도 소비 도메인이 구현한다.** `{소비도메인}/infra/adapter/`에 두고 **제공 도메인의 `infra`(Repository)만 참조한다.** 같은 레이어끼리라 방향 위반이 아니다. 제공 도메인의 `implement`·`service`를 참조하면 `infra -> implement`가 되어 규칙 2(역방향 금지)를 어긴다.
- **엔티티를 포트 시그니처에 노출하지 않는다.** 식별자·원시값·enum, 또는 그것들로만 구성된 스냅샷 `record`를 주고받는다. 이벤트 페이로드 규약과 같은 이유다.
- **제공 도메인이 아직 없으면 스텁을 함께 만든다.** 어댑터 선택은 프로퍼티로 한다 — `app.crossdomain.{name}.mode=stub|real`(기본 `stub`, `matchIfMissing = true`). 컴포넌트 스캔 순서에 좌우되는 `@ConditionalOnMissingBean`은 쓰지 않는다.
- **상태를 바꾸는 일은 포트로 하지 않는다.** 다른 도메인의 상태 변경은 이벤트로 알리고 그쪽이 스스로 바꾼다. 포트는 조회와 발행 위임(예: 새 버전 발행)에만 쓴다.

#### **예외 — 워크스페이스 접근 검증** (2026-09-10 확정)

**참여 여부와 권한 서열 검증은 포트를 거치지 않고 `workspace/implement/WorkspaceAccessValidator`를 직접 주입한다.** 위 규칙의 유일한 예외이며 근거가 셋이다.

- **「비참여자에게 404」 정책이 한 곳에 남아야 한다.** 참여 여부를 먼저 보고(404) 그다음 서열을 보는(403) 순서까지가 정책이며, 소비 도메인마다 어댑터를 두면 이 순서가 복제된다.
- **`validateAtLeast`는 예외를 던지고 값을 돌려주지 않는다.** `boolean` 포트로 감싸면 어댑터가 서열 판정을 다시 하게 되어 그 로직이 소비 도메인 수만큼 늘어난다.
- **워크스페이스 생존 확인이 함께 붙어 있다.** 소프트 삭제 시 참여자 행은 남으므로, 참여 여부만 보면 삭제된 워크스페이스에 딸린 자원이 새어 나간다.

**룰셋 값처럼 상태를 읽는 조회는 포트를 쓴다.** 예외는 접근 검증에 한하며, 소비 도메인은 `WorkspaceAccessValidator`의 두 메서드만 호출한다(참조일 뿐 `workspace` 패키지를 수정하는 것이 아니다).

## **Service 작성 규칙**

service는 비즈니스 로직을 "직접 구현"하는 곳이 아니라 비즈니스 흐름을 "표현"하는 곳이다.

허용한다.

- 유스케이스를 나타내는 public 메서드
- 트랜잭션 경계
- 입력 커맨드 검증 중 비즈니스 흐름에 가까운 검증
- implement 객체를 조합한 업무 흐름
- 도메인 객체의 정책 호출

금지한다.

- Repository 직접 주입
- JPA, QueryDSL, Redis, 메시징 클라이언트, HTTP Client 같은 기술 객체 직접 사용
- 요청 DTO를 그대로 서비스 인자로 받기, 응답 DTO를 서비스에서 직접 조립하기
- 복잡한 if, for, switch가 누적되어 구현 상세가 드러나는 코드
- 외부 API 응답 모델이나 DB Entity에 강하게 결합된 코드

```java
@Transactional
public PaymentResult pay(PaymentCommand command) {
    User user = userReader.read(command.userId());
    UsedPoint usedPoint = pointUseProcessor.use(user.id(), command.usePoint());
    Payment payment = paymentAppender.append(user, usedPoint, command.amount());

    return PaymentResult.from(payment);
}
```

## **Implement 작성 규칙**

implement는 서비스가 사용하는 협력 도구다. 하나의 클래스는 하나의 명확한 역할을 가지며, 이름만 봐도 서비스 흐름에서 맡는 역할이 드러나야 한다.

네이밍은 역할 중심으로 한다 — 조회 `UserReader`, 생성 `PaymentAppender`·`CouponIssuer`, 수정 `OrderUpdater`·`PointUseProcessor`, 검증 `StoreAccessValidator`, 계산 `PriceCalculator`, 외부 연동 조율 `TaxInvoiceRequester`·`MessageSender`.

- implement는 상세 구현 로직을 가지며, infra가 제공하는 인터페이스나 저장소 접근 객체를 사용할 수 있다.
- 다른 implement와 협력할 수 있지만 순환 참조는 금지하고, 재사용 가능한 단위로 작게 유지한다.

## **Infra 작성 규칙**

infra는 기술 의존성을 격리한다.

- Spring Data Repository, QueryDSL, Redis, 외부 API Client 구현체는 이 레이어에 둔다.
- **JPA 엔티티는 domain에 둔다.** 도메인 모델 클래스에 JPA 애노테이션을 붙여 쓰므로 별도 엔티티 클래스와 변환 코드를 만들지 않는다. Repository는 도메인 모델을 그대로 다룬다.
- 상위 레이어에는 기술 세부사항을 노출하지 않는다. 외부 API 응답 DTO를 그대로 올리지 않고, 필요하면 순수 인터페이스와 조회 결과 모델을 제공한다.
- 쿼리 방식 변경(Spring Data ↔ QueryDSL, 페이징·정렬 전략)은 service나 implement로 번지지 않아야 한다.

> 도메인 모델이 JPA 엔티티를 겸하므로 **JPA 자체를 벗어나는 전환은 domain 수정을 수반한다.** 변환 코드와 클래스 중복을 없애는 대신 이 비용을 받아들인 선택이다.

## **이벤트 발행 규약**

메시징 기술은 로컬과 배포 환경에서 달라진다. 상위 레이어가 그 차이를 알지 않도록 다음 규약을 따른다.

### **배치**

포트 인터페이스는 infra에 두고, 어댑터를 기술별 하위 패키지로 나눈다.

```java
common
├── domain
│   └── event
│       └── DomainEvent          // 마커 인터페이스, 프레임워크 의존 없음
└── infra
    └── event
        ├── EventPublisher       // 포트
        └── InMemoryEventPublisher
```

- implement는 포트만 참조한다. `ApplicationEventPublisher` 같은 기술 객체를 직접 주입받지 않는다.
- 배포용 어댑터는 같은 포트를 구현해 `infra` 하위에 기술별 패키지로 추가한다.
- 어댑터 선택은 프로퍼티로 제어한다. 프로파일에 묶으면 테스트에서 어댑터만 따로 켤 수 없다.

### **이벤트 페이로드**

이벤트는 **직렬화 가능한 불변 record**로 정의한다. 식별자·원시값·값 객체·시각 타입만 담는다.

금지한다.

- JPA Entity, 지연 로딩 프록시, 연관 컬렉션
- 영속성 컨텍스트나 트랜잭션이 살아 있어야 읽을 수 있는 값
- **도메인 모델.** 도메인 모델이 곧 JPA 엔티티이므로 위 금지에 포함된다. 상태가 필요한 컨슈머는 식별자로 다시 조회한다.

```java
// 금지 — 도메인 모델이 곧 엔티티다
public record WorkspaceCreatedEvent(Workspace workspace) implements DomainEvent {}

// 허용
public record WorkspaceCreatedEvent(Long workspaceId, Long ownerId, OffsetDateTime occurredAt)
        implements DomainEvent {}
```

인메모리 어댑터는 객체 참조를 그대로 전달하므로 이 규칙을 어겨도 로컬에서는 동작하고, 외부 큐 어댑터로 바꾸는 순간 직렬화 실패나 `LazyInitializationException`으로 드러난다(`AFTER_COMMIT` 리스너는 세션이 닫힌 뒤 실행되며 그 예외는 호출자에게 전파되지 않는다). **이 규약은 로컬에서 검증되지 않으므로 리뷰에서 확인한다.**

### **핸들러**

- 핸들러는 **멱등**하게 작성한다. 대부분의 메시지 큐는 at-least-once라 같은 이벤트가 두 번 도착할 수 있다.
- 이벤트 순서에 의존하지 않는다.
- 어댑터는 수신 애노테이션만 담당하고 처리 로직은 공용 핸들러에 위임한다. 인메모리와 외부 큐가 같은 핸들러를 호출해야 비즈니스 로직이 한 벌로 유지된다.

### **실패 처리**

`@TransactionalEventListener(AFTER_COMMIT)`에서 발생한 예외는 호출자에게 전파되지 않고 사라지며, `@Async`가 붙으면 더 확실히 묻힌다. 외부 큐라면 재시도 후 DLQ로 갔을 실패가 로컬에서는 아무 흔적 없이 지나간다. 인메모리 경로의 비동기 수신 실패는 `AsyncEventConfig`의 `AsyncUncaughtExceptionHandler`가 ERROR 로그로 남긴다. **리스너에서 예외를 삼키지 않는다.**

## **Domain 작성 규칙**

domain은 프로젝트의 핵심 개념과 정책을 담는다.

- 값 객체는 불변으로 설계하고, 상태 전이 규칙은 도메인 객체 내부에 둔다. 단순 데이터 컨테이너가 아니라 의미 있는 행위를 제공한다.
- **JPA 매핑 애노테이션은 허용한다.** `@Entity`, `@Table`, `@Id`, `@Column`, `@Embedded`, `@Embeddable`, `@Enumerated`, `@MappedSuperclass`, 연관 매핑, 그리고 Lombok `@Getter`·`@NoArgsConstructor(access = PROTECTED)`까지다.
- **Spring과 Web 의존은 두지 않는다.** `@Component`, `@Transactional`, `ResponseEntity`, `HttpStatus`는 domain에 들어오지 않는다. 도메인 에러 코드는 `{domain}/exception`에 둔다.
- `@Entity` 클래스는 record로 만들 수 없다. 기본 생성자와 가변 필드가 필요하므로 `@NoArgsConstructor(access = PROTECTED)` + setter 없는 일반 클래스로 둔다.

## **트랜잭션 경계**

- 기본 트랜잭션 경계는 service public 메서드에 둔다.
- 조회 전용 유스케이스는 `@Transactional(readOnly = true)`를 사용한다.
- implement에는 원칙적으로 트랜잭션을 선언하지 않는다.
- 하위 도구 클래스에서 독립 트랜잭션이 필요하면 이유를 PR에 명시한다.

## **리뷰 체크리스트**

- service 메서드가 비즈니스 흐름으로 읽히며, Repository나 외부 기술 객체를 직접 참조하지 않는가?
- implement 클래스가 하나의 명확한 역할을 갖는가?
- infra가 Repository·외부 클라이언트 기술을 상위 레이어에 전파하지 않는가? (엔티티는 domain이므로 여기 해당하지 않는다)
- 도메인 모델이 presentation까지 그대로 올라가지 않고, 응답이 result → 응답 DTO로만 나가는가?
- domain에 Spring·Web 의존이 들어오지 않았는가?
- 레이어를 건너뛰는 참조나 무분별한 도메인 간 직접 참조가 없는가?
- 이벤트 페이로드가 직렬화 가능한 불변 record이며, 핸들러가 멱등하고 순서에 의존하지 않는가?
