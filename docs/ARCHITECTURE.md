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
- JPA, QueryDSL, Redis, Kafka, HTTP Client 같은 기술 객체 직접 사용
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
