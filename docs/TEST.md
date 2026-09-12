# **목표**

테스트는 변경을 안전하게 만들기 위한 문서이자 자동 검증 수단이다. 이름만 읽어도 검증 대상을 알 수 있고, 실패 원인을 빠르게 좁힐 수 있으며, 구현 세부사항이 아니라 외부로 드러나는 행위를 검증해야 한다.

## **테스트 계층**

| **테스트** | **대상** | **목적** |
| --- | --- | --- |
| Unit Test | Domain, Implement 단위 클래스 | 빠른 피드백, 정책/계산/검증 확인 |
| Service Test | Service 유스케이스 | 비즈니스 흐름과 트랜잭션 경계 확인 |
| Repository Test | Infra(Repository) + Domain(엔티티 매핑) | 쿼리, 매핑, 영속성 확인 |
| Controller Test | Presentation | 요청/응답, 검증, 상태 코드 확인 |
| Integration Test | 여러 레이어 결합 | 주요 시나리오 회귀 방지 |

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

## DB 테스트 독립 환경 설정

각 테스트의 독립적인 환경은 **`DbCleaner`가 테이블을 비우는 방식**으로 만든다. **개별 테스트에 `@Transactional`을 붙이지 않는다.** `support/IntegrationTestSupport`(`@ActiveProfiles("test")` + `@Import({TestcontainersConfiguration.class, DbCleaner.class})` + `@SpringBootTest(webEnvironment = NONE)` + `@BeforeEach dbCleaner.clean()`)를 상속하면 매 테스트 전에 정리된다.

**`@Transactional` 롤백에 기대지 않는 이유가 셋이다.**

- **서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써** 경계 자체를 검증하지 못하게 된다. 「Owner는 정확히 1명」이나 소유권 이전처럼 트랜잭션 경계로만 보장되는 불변식이 통과해 버린다.
- 커밋이 일어나지 않으므로 `@TransactionalEventListener(AFTER_COMMIT)` 리스너가 실행되지 않는다.
- 명시적 flush로 순서를 만드는 코드(`DictionaryUpdater.archive`의 `saveAndFlush`)가 무엇을 막는지 드러나지 않는다.

`DbCleaner`는 엔티티 메타모델이 아니라 `information_schema`에서 실제 테이블을 읽어 `truncate`한다. Flyway 이력 테이블은 지우지 않는다 — 지우면 다음 컨텍스트에서 마이그레이션이 다시 돌아 스키마가 어긋난다.

> **테스트 전용 엔티티를 만들지 않는다.** 스키마는 Flyway 마이그레이션이 만들고 실제 운영 스키마와 같은 형태에서 매핑을 검증한다. 예외는 `BaseEntityAuditingTest` 하나이며 그 테스트만 `flyway.enabled=false` + `ddl-auto=create-drop`을 쓴다.

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

### Service 테스트

Service 테스트는 가능하면 실제와 가까운 환경에서 검증한다. 위 «DB 테스트 독립 환경 설정»의 `IntegrationTestSupport`를 상속해 쓴다.

### **Repository 테스트**

Repository 테스트는 실제 DB와 가까운 환경(Testcontainers 또는 프로젝트 표준 테스트 DB)에서 검증한다. `support/RepositoryTestSupport`(`@ActiveProfiles("test")` + `@Import(DbCleaner.class)` + `@DataJpaTest` + `TestEntityManager`)를 상속한다.

- 쿼리 조건과 정렬, 엔티티 매핑(domain 모델 ↔ 테이블), N+1이나 fetch join이 중요한 조회를 검증한다.
- H2와 운영 DB의 문법 차이를 무시한 테스트, 단순 Spring Data 메서드에 대한 과도한 테스트는 지양한다.

## **테스트 데이터 정리**

테스트는 서로 독립적이어야 하며 실행 순서에 의존하지 않는다. DB 정리는 위 «DB 테스트 독립 환경 설정»을 따른다.

## **리뷰 체크리스트**

- 테스트 이름이 비즈니스 행위를 설명하는가?
- 실패 케이스가 함께 검증되는가?
- 테스트가 구현 세부사항에 과하게 결합되어 있지 않은가?
- Fixture가 테스트 의도를 가리지 않는가?
- 통합 테스트와 단위 테스트의 역할이 분리되어 있는가?
