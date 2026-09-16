# **목표**

테스트는 변경을 안전하게 만들기 위한 문서이자 자동 검증 수단이다. 이름만 읽어도 검증 대상을 알 수 있고, 실패 원인을 빠르게 좁힐 수 있으며, 구현 세부사항이 아니라 외부로 드러나는 행위를 검증해야 한다.

## **테스트 계층**

| **테스트** | **대상** | **목적** |
| --- | --- | --- |
| Unit Test | Domain, Implement 단위 클래스 | 빠른 피드백, 정책/계산/검증 확인 |
| Service Test | Service 유스케이스 | 비즈니스 흐름과 트랜잭션 경계 확인 |
| Repository Test | Infra(Repository) + Domain(엔티티 매핑) | 쿼리, 매핑, 영속성 확인 |
| Controller Test | Presentation | 요청/응답, 검증, 상태 코드 확인 |
| Integration Test | 여러 레이어 결합 | 주요 시나리오 회귀 방지. 도메인을 가로지르는 것은 `scenario/` 아래 둔다 |

> 도메인 모델이 JPA 엔티티를 겸하므로 단위 테스트는 도메인 객체를 `new`로 만들어 검증하며, 이때 식별자는 `null`이다. 식별자가 필요한 검증은 Repository·Service 테스트에서 하거나 Fixture Builder로 주입한다.

## **네이밍 규칙**

```java
@DisplayName("포인트가 부족하면 결제에 실패한다.")
@Test
void pay_pointIsNotEnough() {}
```

- `@DisplayName`은 사용자 행위 또는 비즈니스 규칙 중심으로 쓰고, 어투는 `~다.` 형태로 맞춘다.
- 메서드명은 영어를 기본으로 하고, 실패 케이스는 `{메서드명}_{실패이유}` 형식으로 쓴다.
- 하나의 테스트는 하나의 이유로 실패해야 한다.

## **Given-When-Then**

테스트 본문은 given(조건), when(검증 대상 행위 1회), then(결과 검증) 구역으로 주석을 달아 나눈다. **테스트를 이해하는 데 필요 없는 값은 fixture나 builder로 숨긴다.**

## **Fixture 규칙**

Fixture는 테스트의 의도를 가리는 중복을 줄이기 위해 사용한다.

- 모든 필드를 받는 거대한 fixture 메서드(`create(id, name, age, status, ...)`)는 만들지 않는다.
- 테스트에서 중요한 값은 테스트 본문에 드러내고, 중요하지 않은 기본값은 fixture 내부에 둔다.
- fixture는 운영 코드에 의존하지 않으며 `src/test` 아래에서만 쓴다.

### **중요한 값을 주입하는 Fixture Builder 규칙**

특정 값이 검증 의도에 중요하다면 fixture 메서드 인자를 늘리지 말고 **테스트 전용 builder**를 쓴다. `{도메인}/fixture/` 아래 기존 fixture와 같은 형태(정적 팩토리 + 중첩 builder, 체이닝을 위해 자기 자신 반환)를 따른다.

권장 형식:

```java
public class UserFixture {

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static class UserBuilder {

        private String id = "user-1";
        private UserGrade grade = UserGrade.NORMAL;
        private Point point = Point.wons(10_000);

        public UserBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserBuilder grade(UserGrade grade) {
            this.grade = grade;
            return this;
        }

        public UserBuilder point(Point point) {
            this.point = point;
            return this;
        }

        public User build() {
            return User.create(id, grade, point);
        }
    }
}
```

사용 예시:

```java
User user = UserFixture.user().grade(UserGrade.VIP).build();
```

- builder의 기본값은 정상 케이스를 만들 수 있는 값으로 두고, `build()`는 항상 유효한 객체를 반환한다.
- 테스트마다 값이 자주 바뀌는 필드만 builder 메서드로 노출한다.
- 식별자가 필요하면 builder에서 주입한다. 저장 전 id가 `null`이므로 테스트 코드에서의 리플렉션 주입을 허용한다.

## **Assertion 규칙**

JUnit 기본 assertion보다 AssertJ를 우선 사용한다.

```java
assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
assertThatThrownBy(() -> paymentService.pay(command))
    .isInstanceOf(PaymentException.class)
    .hasMessage(PAYMENT_NOT_ENOUGH_POINT.getMessage());
```

- 예외 검증은 `assertThatThrownBy` 또는 `assertThatExceptionOfType`을, 컬렉션 검증은 `containsExactly`·`extracting`·`filteredOn`을 쓴다.
- JUnit assertion은 테스트 생명주기와 관련된 특수 상황이 아니면 사용하지 않는다.

## **Mock 사용 규칙**

Mock은 외부 협력 객체의 결과를 통제해야 할 때만 쓴다 — 외부 API 호출, 메시지 발행, 시간·UUID·랜덤 같은 비결정 요소, 실패 상황 강제.

도메인 모델·값 객체·같은 모듈의 작은 객체까지 mock으로 덮거나, 실제 검증 없이 `verify()` 호출 수에만 집중한 테스트는 지양한다.

**메시지 브로커는 더 이상 mock 대상이 아니다.** LocalStack이 실제 SQS를 준다(`D-75`). `SqsTemplate` mock은 「큐로 나가는 본문을 사람이 눈으로 보는」 빠른 계약 테스트에만 남긴다 — 외부 계약이 되는 메시지는 그 모양 자체가 검증 대상이라 **계약 문서의 예시 JSON과 문자열 수준으로 맞춘다.**

## DB 테스트 독립 환경 설정

각 테스트의 독립적인 환경은 **`DbCleaner`가 테이블을 비우는 방식**으로 만든다. **개별 테스트에 `@Transactional`을 붙이지 않는다.** `support/IntegrationTestSupport`(`@ActiveProfiles("test")` + `@Import({TestcontainersConfiguration.class, DbCleaner.class})` + `@SpringBootTest(webEnvironment = NONE)` + `@BeforeEach dbCleaner.clean()`)를 상속하면 매 테스트 전에 정리된다.

**`@Transactional` 롤백에 기대지 않는 이유가 셋이다.**

- **서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써** 경계 자체를 검증하지 못하게 된다. 「Owner는 정확히 1명」이나 소유권 이전처럼 트랜잭션 경계로만 보장되는 불변식이 통과해 버린다.
- 커밋이 일어나지 않으므로 `@TransactionalEventListener(AFTER_COMMIT)` 리스너가 실행되지 않는다.
- 명시적 flush로 순서를 만드는 코드(`DictionaryUpdater.archive`의 `saveAndFlush`)가 무엇을 막는지 드러나지 않는다.

`DbCleaner`는 엔티티 메타모델이 아니라 `information_schema`에서 실제 테이블을 읽어 `truncate`한다 — 컨텍스트마다 스캔되는 엔티티 집합이 달라 메타모델에 없는 테이블이 남을 수 있다.

> **테스트 전용 엔티티를 만들지 않는다.** 스키마는 운영과 같은 엔티티 매핑에서 `ddl-auto=create-drop`이 만들고, 그 형태 위에서 매핑을 검증한다. 예외는 `BaseEntityAuditingTest` 하나다.
>
> 매핑이 **MySQL에서** DDL로 떨어지는지는 `SchemaGenerationTest`가 MySQL 컨테이너 위에서 확인한다. H2의 `MODE=MySQL`은 `columnDefinition`의 타입명·생성 컬럼 식을 그대로 검증해 주지 않는다.

## 큐 테스트 독립 환경 설정

외부 큐를 거치는 경로는 **LocalStack을 Testcontainers로 띄워 실제 메시지로** 검증한다(`D-75`). 인메모리 대역으로는 직렬화·큐 이름·계약 필드·at-least-once 재수신이 검증되지 않는데, 그것이 정확히 이 경로의 위험이다.

- **컨테이너는 `TestcontainersConfiguration`에 둔다.** 별도 `@TestConfiguration`으로 떼면 「큐가 필요한 테스트」와 아닌 테스트의 Spring 컨텍스트가 갈리고, **컨텍스트가 갈리면 MySQL·Redis·LGTM까지 한 벌 더 뜬다.** 컨테이너 하나를 더 띄우는 비용이 훨씬 싸다.
- **큐는 미리 만들지 않는다.** `spring.cloud.aws.sqs.queue-not-found-strategy: create`로 첫 접근에 만든다 — 초기화 스크립트와 리스너 컨테이너 기동의 순서 경합을 없앤다(`D-78`).
- **컨테이너 재사용(`withReuse`)을 켜지 않는다.** 머신마다 opt-in이라 CI와 로컬의 동작이 갈리고, 무엇보다 **큐가 실행 사이에 살아남아 지난 실행의 메시지가 다음 실행의 첫 테스트를 때린다.**
- **`PurgeQueue`를 쓰지 않는다.** 실 AWS는 60초에 한 번만 허용해 같은 코드가 AWS에서 돌지 않는다. 비워야 한다면 짧은 폴링으로 받아 지운다.
- 기본은 인프로세스 대역이고 **실제 큐를 보는 테스트만 `@TestPropertySource`로 켠다.** 전 테스트가 큐를 롱폴하면 빌드가 느려지고 결과가 타이밍에 휘둘린다.

## 계층별 테스트

### **Controller 테스트**

Controller 테스트는 HTTP 계약(URL, method, header, query parameter, request/response body, status code, 인증·인가 실패)을 검증한다. **RestAssuredMockMvc를 표준으로 사용하고, 단순 `MockMvc.perform()` 방식은 새 테스트에서 쓰지 않는다.**

```java
// @WebMvcTest(PaymentController.class) + Service는 @MockitoBean
// @BeforeEach에서 RestAssuredMockMvc.mockMvc(mockMvc)로 초기화한다
RestAssuredMockMvc.given().contentType(ContentType.JSON).body(request)
    .when().post("/api/v1/payments")
    .then().statusCode(HttpStatus.CREATED.value()).body("status", equalTo("PAID"));
```

- Service는 mock 처리하고 Controller의 HTTP 계약에 집중한다. 복잡한 비즈니스 성공/실패 조합은 Service 테스트에서 검증한다.
- **인증 주체가 없는 엔드포인트(`/api/internal/**`)는 principal을 주입하지 않는 것 자체가 검증이다.** `@WithLoginMember` 없이 호출해 동작해야 하고, 그 경로가 실제로 열려 있는지는 `SecurityConfigTest`가 본다 — **permitAll 범위가 넓어지지 않았다는 반대편 케이스도 함께 둔다.**

### Service 테스트

Service 테스트는 가능하면 실제와 가까운 환경에서 검증한다. 위 «DB 테스트 독립 환경 설정»의 `IntegrationTestSupport`를 상속해 쓴다.

### **Repository 테스트**

Repository 테스트는 실제 DB와 가까운 환경(Testcontainers 또는 프로젝트 표준 테스트 DB)에서 검증한다. `support/RepositoryTestSupport`(`@ActiveProfiles("test")` + `@Import(DbCleaner.class)` + `@DataJpaTest` + `TestEntityManager`)를 상속한다.

- 쿼리 조건과 정렬, 엔티티 매핑(domain 모델 ↔ 테이블), N+1이나 fetch join이 중요한 조회를 검증한다.
- H2와 운영 DB의 문법 차이를 무시한 테스트, 단순 Spring Data 메서드에 대한 과도한 테스트는 지양한다.

### Integration 테스트

Integration 테스트는 **여러 도메인과 여러 레이어가 실제로 물려 있는지**를 본다. Service 테스트와 같은 `IntegrationTestSupport`를 상속하고 `webEnvironment = NONE`이므로 HTTP 계층은 검증 대상이 아니다 — 컨트롤러의 계약은 Controller 테스트가, 인증·인가는 `SecurityConfigTest`가 각자 본다.

- **한 도메인에 속하는 시나리오는 그 도메인 패키지에 둔다.** 대조·추출의 비동기 완료처럼 주인이 분명한 것이 여기 해당한다.
- **주인이 없는 시나리오는 `com.ubidict.backend.scenario` 패키지에 둔다.** 워크스페이스 → 문서 → 추출 → 사전 초안 → 리뷰 → 사전집 발행처럼 여섯 도메인을 관통하는 흐름은 어느 도메인의 것도 아니다.
- **서비스 진입점만으로 시나리오를 엮는다.** 리포지토리에 직접 seeding하면 그 지점의 정책 검증과 이벤트 발행을 건너뛰어, 배선이 끊겨 있어도 초록이 된다.
- **비동기 경로를 기다릴 때는 Awaitility로 상한을 걸고 기다린다.** `Thread.sleep` 폴링 루프를 새로 만들지 않는다 — 손으로 쓴 루프는 타임아웃 시 마지막 상태를 조용히 반환해 무엇을 기다리다 실패했는지가 남지 않는다. **대기 상한은 한 상수에 둔다**(`support/AsyncWaits`) — 복제해 두면 한 번에 조정할 수 없다(`D-76`).
- **외부 워커가 필요한 경로는 테스트 전용 가짜 워커가 대신한다**(`D-75`). 실제 큐를 읽고 정해진 시나리오(성공·실패·중복·무응답)로 콜백 경로를 부른다. 콜백의 HTTP 계약은 Controller 테스트가, permitAll 범위는 `SecurityConfigTest`가 따로 보므로 여기서 HTTP를 타지 않아도 된다.
- **LLM 요청은 아웃박스까지 검증한다.** 접수 통합 테스트는 Job과 outbox가 함께 저장되고 전송 뒤 `PUBLISHED`가 되는지를 본다. DLQ 수신 테스트는 LocalStack에 전용 요청 큐·redrive 정책을 만들고, 재시도 소진이 Job `FAILED`로 회수되는지를 검증한다.

## **테스트 데이터 정리**

테스트는 서로 독립적이어야 하며 실행 순서에 의존하지 않는다. DB 정리는 위 «DB 테스트 독립 환경 설정»을 따른다.

## **리뷰 체크리스트**

- 테스트 이름이 비즈니스 행위를 설명하는가?
- 실패 케이스가 함께 검증되는가?
- 테스트가 구현 세부사항에 과하게 결합되어 있지 않은가?
- Fixture가 테스트 의도를 가리지 않는가?
- 통합 테스트와 단위 테스트의 역할이 분리되어 있는가?
