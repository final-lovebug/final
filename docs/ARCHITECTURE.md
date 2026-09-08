# **Architecture Convention**

## **목표**

프로젝트는 **도메인 중심의 레이어드 아키텍처를 사용**한다.

**핵심 목표는 service가 상세 구현을 알지 않고도 비즈니스 흐름을 설명할 수 있게 만드는 것**이다. 신규 입사자, 기획자, 운영 담당자가 service 메서드를 읽었을 때 대략적인 업무 흐름을 이해할 수 있어야 한다.

이 문서는 Gemini Kim의 글 **지속 성장 가능한 소프트웨어를 만들어가는 방법**의 방향성을 프로젝트 컨벤션으로 구체화한 것이다.

## **기본 패키지 구조**

도메인을 최상위 기준으로 나누고, 도메인 내부에서 레이어를 나눈다.

```java
com.example.project
└── payment
    ├── presentation
    ├── service
    ├── implement
    ├── infra
    └── domain
```

| **패키지** | **역할** |
| --- | --- |
| presentation | HTTP 요청/응답, Controller, API DTO, 인증 사용자 해석 |
| service | 비즈니스 흐름 조립, 유스케이스 단위 트랜잭션 경계 |
| implement | 비즈니스 흐름을 구성하는 상세 구현 도구 |
| infra | DB, 외부 저장소, 외부 API 접근 기술 격리 |
| domain | 도메인 모델, 값 객체, 정책, 상태 전이 규칙 |

## **의존성 방향**

레이어 의존성은 항상 아래 방향으로만 흐른다.

```java
presentation -> service -> implement -> infra
                         -> domain
```

규칙은 다음과 같다.

1. 상위 레이어는 하위 레이어만 참조한다.
2. 하위 레이어는 상위 레이어를 참조하지 않는다.
3. 레이어를 건너뛰지 않는다. 예를 들어 service가 infra를 직접 참조하지 않는다.
4. 동일 레이어 간 참조는 피한다. 단, implement 레이어는 협력 도구 성격이 강하므로 필요한 경우 같은 도메인 안에서만 참조할 수 있다.
5. 다른 도메인을 직접 참조해야 한다면 먼저 도메인 간 의존 방향과 공개 API를 합의한다.

## **Service 작성 규칙**

service는 비즈니스 로직을 "직접 구현"하는 곳이 아니라 비즈니스 흐름을 "표현"하는 곳이다.

허용한다.

- 유스케이스를 나타내는 public 메서드
- 트랜잭션 경계
- 입력 커맨드 검증 중 비즈니스 흐름에 가까운 검증
- implement 객체를 조합한 업무 흐름
- 도메인 객체의 정책 호출

금지한다.

- Repository 직접 주입
- JPA, QueryDSL, Redis, 메시징 클라이언트, HTTP Client 같은 기술 객체 직접 사용
- 요청 DTO를 그대로 서비스 인자로 받기
- 응답 DTO를 서비스에서 직접 조립하기
- 복잡한 if, for, switch가 누적되어 구현 상세가 드러나는 코드
- 외부 API 응답 모델이나 DB Entity에 강하게 결합된 코드

권장 예시:

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserReader userReader;
    private final StoreReader storeReader;
    private final PointUseProcessor pointUseProcessor;
    private final PaymentAppender paymentAppender;

    @Transactional
    public PaymentResult pay(PaymentCommand command) {
        User user = userReader.read(command.userId());
        Store store = storeReader.readAvailableStore(command.storeId(), user.grade());
        UsedPoint usedPoint = pointUseProcessor.use(user.id(), command.usePoint());
        Payment payment = paymentAppender.append(user, store, usedPoint, command.amount());

        return PaymentResult.from(payment);
    }
}
```

피해야 할 예시:

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PointRepository pointRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        // 조회, 검증, 포인트 차감, 결제 생성, 응답 조립이 모두 섞인 형태는 지양한다.
    }
}
```

## **Implement 작성 규칙**

implement는 서비스가 사용하는 협력 도구다. 하나의 클래스는 하나의 명확한 역할을 가진다.

네이밍은 역할 중심으로 작성한다.

| **역할** | **예시** |
| --- | --- |
| 조회 | UserReader, OrderReader |
| 생성 | PaymentAppender, CouponIssuer |
| 수정 | OrderUpdater, PointUseProcessor |
| 검증 | StoreAccessValidator, PaymentPolicyValidator |
| 계산 | PriceCalculator, RefundAmountCalculator |
| 외부 연동 조율 | TaxInvoiceRequester, MessageSender |

규칙은 다음과 같다.

- implement는 상세 구현 로직을 가진다.
- implement는 infra가 제공하는 인터페이스 또는 저장소 접근 객체를 사용할 수 있다.
- implement는 다른 implement와 협력할 수 있지만 순환 참조는 금지한다.
- 재사용 가능한 단위로 작게 유지한다.
- 클래스 이름만 봐도 서비스 흐름에서 맡는 역할이 드러나야 한다.

## **Infra 작성 규칙**

infra는 기술 의존성을 격리한다.

- JPA Entity, Spring Data Repository, QueryDSL, Redis, 외부 API Client 구현체는 이 레이어에 둔다.
- 상위 레이어에는 기술 세부사항을 노출하지 않는다.
- 필요한 경우 상위 레이어가 사용할 순수 인터페이스와 조회 결과 모델을 제공한다.
- 외부 API 응답 DTO를 그대로 상위 레이어로 올리지 않는다.
- DB 기술 변경이 service나 implement의 대규모 변경으로 번지지 않아야 한다.

## **이벤트 발행 규약**

메시징 기술은 로컬과 배포 환경에서 달라진다. 로컬은 Spring `ApplicationEvent`를 사용하고, 배포 환경에서는 외부 메시지 큐 어댑터로 교체하는 것을 전제한다. 상위 레이어가 이 차이를 알지 않도록 다음 규약을 따른다.

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

이벤트는 **직렬화 가능한 불변 record**로 정의한다.

허용한다.

- 식별자, 원시값, 값 객체, 시각 타입

금지한다.

- JPA Entity, 지연 로딩 프록시, 연관 컬렉션
- 영속성 컨텍스트나 트랜잭션이 살아 있어야 읽을 수 있는 값

인메모리 어댑터는 객체 참조를 그대로 전달하므로 이 규칙을 어겨도 로컬에서는 동작한다. 외부 큐 어댑터로 바꾸는 순간 직렬화 실패나 `LazyInitializationException`으로 드러난다. **이 규약은 로컬에서 검증되지 않으므로 리뷰에서 확인한다.**

### **핸들러**

- 핸들러는 **멱등**하게 작성한다. 대부분의 메시지 큐는 at-least-once라 같은 이벤트가 두 번 도착할 수 있다.
- 이벤트 순서에 의존하지 않는다.
- 어댑터는 수신 애노테이션만 담당하고 처리 로직은 공용 핸들러에 위임한다. 인메모리와 외부 큐가 같은 핸들러를 호출해야 비즈니스 로직이 한 벌로 유지된다.

### **실패 처리**

`@TransactionalEventListener(AFTER_COMMIT)`에서 발생한 예외는 호출자에게 전파되지 않고 사라진다. `@Async`가 붙으면 더 확실히 묻힌다.

- 외부 큐라면 재시도 후 DLQ로 갔을 실패가 로컬에서는 아무 흔적 없이 지나간다.
- 인메모리 경로의 비동기 수신 실패는 `AsyncEventConfig`의 `AsyncUncaughtExceptionHandler`가 ERROR 로그로 남긴다. 리스너에서 예외를 삼키지 않는다.

## **Domain 작성 규칙**

domain은 프로젝트의 핵심 개념과 정책을 담는다.

- 값 객체는 불변으로 설계한다.
- 상태 전이 규칙은 도메인 객체 내부에 둔다.
- 단순 데이터 컨테이너가 아니라 의미 있는 행위를 제공한다.
- Spring, JPA, Web 같은 프레임워크 의존은 최소화한다.

## **트랜잭션 경계**

- 기본 트랜잭션 경계는 service public 메서드에 둔다.
- 조회 전용 유스케이스는 @Transactional(readOnly = true)를 사용한다.
- implement에는 원칙적으로 트랜잭션을 선언하지 않는다.
- 하위 도구 클래스에서 독립 트랜잭션이 필요하면 이유를 PR에 명시한다.

## **리뷰 체크리스트**

- service 메서드가 비즈니스 흐름으로 읽히는가?
- service가 Repository나 외부 기술 객체를 직접 참조하지 않는가?
- implement 클래스가 하나의 명확한 역할을 갖는가?
- infra가 기술 의존성을 상위 레이어에 전파하지 않는가?
- 레이어를 건너뛰는 참조가 없는가?
- 도메인 간 직접 참조가 무분별하게 늘어나지 않았는가?
- 이벤트 페이로드가 직렬화 가능한 불변 record인가? Entity나 지연 로딩 대상이 섞이지 않았는가?
- 이벤트 핸들러가 멱등하며 순서에 의존하지 않는가?
