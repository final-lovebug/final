## **목표**

로그는 운영 중인 서버에서 무슨 일이 일어났는지 확인하기 위한 기록이다.

개발 환경에서는 디버거로 코드를 한 줄씩 볼 수 있지만, 운영 환경에서는 사용자의 요청이 언제 실패했는지, 어떤 데이터와 관련 있었는지, 어느 외부 시스템에서 문제가 났는지를 로그로 추적해야 한다.

좋은 로그는 다음 질문에 답할 수 있어야 한다.

- 어떤 일이 발생했는가?
- 어떤 사용자, 주문, 결제, 리소스와 관련 있는가?
- 성공했는가, 실패했는가?
- 실패했다면 원인이 무엇인가?
- 같은 요청의 다른 로그를 어떻게 찾아갈 수 있는가?

로그는 많이 남기는 것이 목적이 아니다. 필요한 맥락을 안전하게 남기는 것이 목적이다.

## **기본 사용법**

Spring 프로젝트에서는 SLF4J Logger를 사용한다. 구현체는 프로젝트 설정에 따라 Logback 등을 사용한다.

클래스마다 다음 방식으로 logger를 선언한다.

```java
private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
```

Lombok을 사용하는 프로젝트라면 @Slf4j를 사용할 수 있다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
}
```

로그 메시지는 문자열 더하기가 아니라 placeholder를 사용한다.

권장한다.

```java
log.info("[PaymentService.pay] Payment completed. paymentId={}, orderId={}", paymentId, orderId);
```

지양한다.

```java
log.info("[PaymentService.pay] Payment completed. paymentId=" + paymentId + ", orderId=" + orderId);
```

이유는 다음과 같다.

- placeholder 방식은 불필요한 문자열 생성을 줄인다.
- 로그 메시지와 값을 분리해서 읽기 쉽다.
- 구조화 로그로 전환하기 쉽다.

## **로그 메시지 작성 규칙**

### **로그 위치 Prefix 규칙**

로그 메시지의 맨 앞에는 로그가 발생한 위치를 알 수 있도록 [클래스명.메서드명] 형식의 prefix를 붙인다.

기본 형식:

```java
log.level("[{ClassName}.{methodName}] {message}", value);
```

예시:

```java
log.info("[PaymentService.pay] Payment completed. paymentId={}, orderId={}", paymentId, orderId);
log.warn("[OrderService.cancel] Order cancel rejected. orderId={}, currentStatus={}", orderId, status);
log.error("[PaymentClient.approve] Failed to approve payment. paymentId={}, provider={}",
    paymentId, provider, exception);
```

규칙은 다음과 같다.

- 클래스명은 패키지를 제외한 simple class name을 사용한다.
- 메서드명은 실제 로그가 작성된 메서드명을 사용한다.
- prefix 뒤에는 공백 한 칸을 둔 뒤 이벤트 메시지를 작성한다.
- 호출 스택을 읽어서 클래스명과 메서드명을 자동 생성하지 않는다. 성능 비용이 있고, 람다나 프록시 환경에서 기대와 다른 값이 나올 수 있다.
- 공통 로깅 유틸을 사용하는 경우에도 최종 로그 메시지에는 같은 prefix 형식을 유지한다.
- prefix만 보고도 코드 위치를 검색할 수 있어야 한다.

좋은 로그 메시지는 이벤트 중심으로 작성한다.

권장한다.

```java
log.info("[PaymentService.pay] Payment completed. paymentId={}, orderId={}, userId={}", paymentId, orderId, userId);
log.warn("[OrderService.cancel] Order cancel rejected. orderId={}, currentStatus={}", orderId, status);
log.error("[OrderEventPublisher.publish] Failed to publish order event. orderId={}, eventType={}", orderId, eventType, exception);
```

지양한다.

```java
log.info("success");
log.warn("bad request");
log.error("error occurred");
log.info("payment={}", payment);
```

규칙:

- 문장은 짧고 명확하게 쓴다.
- 이벤트 이름은 과거형 또는 결과 중심으로 쓴다. 예: Payment completed, Order cancel rejected
- 식별자는 반드시 포함한다. 예: userId, orderId, paymentId
- 객체 전체를 남기기보다 필요한 필드만 남긴다.
- 로그 메시지에 줄바꿈을 넣지 않는다.

## **로그 레벨**

로그 레벨은 "얼마나 심각한가"와 "운영자가 봐야 하는가"를 기준으로 선택한다.

| **Level** | **사용 기준** | **예시** |
| --- | --- | --- |
| ERROR | 즉시 확인이 필요한 시스템 장애 | DB 연결 실패, 결제 승인 API 장애 |
| WARN | 요청은 실패했지만 시스템 장애는 아닌 경우 | 잔액 부족, 잘못된 상태 변경 요청, 재시도 가능한 외부 API 실패 |
| INFO | 운영 흐름상 의미 있는 주요 이벤트 | 결제 완료, 주문 취소 완료, 쿠폰 발급 완료 |
| DEBUG | 개발 또는 장애 분석용 상세 정보 | 분기 조건, 중간 계산값, 조회 조건 |
| TRACE | 매우 상세한 흐름 추적 | 반복문 내부, 아주 세밀한 단계 |

### **ERROR**

ERROR는 개발자 또는 운영자가 확인해야 하는 장애에 사용한다.

```java
log.error("[PaymentClient.approve] Failed to approve payment. orderId={}, provider={}", orderId, provider, exception);
```

규칙:

- 예상하지 못한 예외에 사용한다.
- 외부 시스템 장애, DB 장애, 메시지 발행 실패처럼 운영 영향이 있는 실패에 사용한다.
- exception 객체를 마지막 인자로 전달해 stack trace를 남긴다.

### **WARN**

WARN은 비정상 흐름이지만 시스템 장애는 아닌 경우에 사용한다.

```java
log.warn("[PaymentService.pay] Payment rejected. orderId={}, reason={}", orderId, PaymentErrorCode.NOT_ENOUGH_POINT);
```

규칙:

- 비즈니스 규칙 위반에 사용한다.
- 재시도 가능하거나 모니터링할 가치가 있는 실패에 사용한다.
- 사용자 실수나 도메인 실패를 무조건 ERROR로 남기지 않는다.

### **INFO**

INFO는 운영에서 추적할 가치가 있는 정상 이벤트에 사용한다.

```java
log.info("[OrderService.cancel] Order canceled. orderId={}, userId={}, reason={}", orderId, userId, cancelReason);
```

규칙:

- 주요 상태 변경에 사용한다.
- 결제, 주문, 회원, 쿠폰, 정산처럼 중요한 업무 이벤트에 사용한다.
- 모든 메서드 시작/종료를 INFO로 남기지 않는다.

### **DEBUG**

DEBUG는 개발 중 확인하거나 장애 분석 때 임시로 보고 싶은 상세 정보에 사용한다.

```java
log.debug("[DiscountCalculator.calculate] Calculated discount. userId={}, grade={}, discountAmount={}", userId, grade, discountAmount);
```

규칙:

- 운영 기본 로그 레벨에서 보이지 않아도 되는 정보에 사용한다.
- 너무 큰 객체나 컬렉션 전체를 남기지 않는다.
- 장애 분석이 끝난 임시 debug 로그는 제거한다.

## **어디에 로그를 남길까?**

로그는 책임이 있는 위치에 남긴다.

### **Controller**

Controller에서는 HTTP 요청/응답 자체보다 API 계약과 관련된 실패를 추적한다.

권장한다.

- 인증 사용자 식별 실패
- 잘못된 요청 값
- 중요한 API 진입 이벤트

지양한다.

- 모든 request body를 그대로 로그로 남기기
- 비즈니스 성공/실패 로그를 Controller에 중복으로 남기기

### **Service**

Service에서는 유스케이스의 주요 업무 이벤트를 남긴다.

```java
@Transactional
public PaymentResult pay(PaymentCommand command) {
    Payment payment = paymentProcessor.pay(command);

    log.info("[PaymentService.pay] Payment completed. paymentId={}, orderId={}, userId={}",
        payment.id(), command.orderId(), command.userId());

    return PaymentResult.from(payment);
}
```

규칙:

- 비즈니스적으로 중요한 상태 변경이 끝난 시점에 남긴다.
- service가 상세 구현을 알면 안 되므로 하위 기술 세부 로그는 남기지 않는다.
- 실패 로그는 예외 처리 정책과 중복되지 않게 조심한다.

### **Implement**

Implement에서는 세부 비즈니스 도구의 의미 있는 실패나 분기 결과를 남길 수 있다.

```java
log.warn("[CouponUseProcessor.use] Coupon use rejected. couponId={}, userId={}, reason={}", couponId, userId, reason);
```

규칙:

- service 흐름에서는 보이지 않는 도메인 판단을 남긴다.
- 같은 실패를 service와 implement에서 중복으로 남기지 않는다.

### **Data Access**

Data Access에서는 외부 시스템, DB, 캐시, 메시지 브로커와의 연동 실패를 남긴다.

```java
log.error("[PaymentClient.approve] Failed to send payment approval request. paymentId={}, provider={}",
    paymentId, provider, exception);
```

규칙:

- 외부 API 요청 실패, timeout, 재시도, circuit breaker open 등을 남긴다.
- SQL 전체나 요청/응답 전문은 기본적으로 남기지 않는다.
- 민감 정보가 들어갈 수 있는 payload는 마스킹한다.

## **무엇을 남겨야 할까?**

상황별로 남기면 좋은 값은 다음과 같다.

| **상황** | **권장 필드** |
| --- | --- |
| 사용자 요청 | traceId, userId, path, method |
| 주문 | orderId, orderStatus, userId |
| 결제 | paymentId, orderId, provider, amount |
| 쿠폰 | couponId, userId, reason |
| 외부 API | provider, requestId, statusCode, elapsedMs |
| 메시지 발행 | eventType, aggregateId, topic |

필드 선택 기준:

- 장애 원인을 찾는 데 도움이 되는가?
- 같은 요청의 다른 로그와 연결할 수 있는가?
- 개인정보나 민감 정보가 아닌가?
- 값이 너무 크지 않은가?

## **민감 정보 규칙**

로그에 남기면 안 되는 정보:

- 비밀번호
- 인증 토큰
- refresh token
- session id
- 주민등록번호
- 카드번호
- 계좌번호
- 휴대폰 번호 전체
- 이메일 전체
- 주소 전체
- 외부 API secret
- 결제 승인 전문 원본

필요하면 마스킹한다.

`log.info("[AuthController.login] User login requested. email={}", MaskingUtils.maskEmail(email));`

마스킹 예시:

| **원본** | **로그** |
| --- | --- |
| hong@example.com | h***@example.com |
| 010-1234-5678 | 010-****-5678 |
| 1234-5678-9012-3456 | 1234-****-****-3456 |

## **Trace ID와 MDC**

Trace ID는 하나의 요청을 따라가기 위한 식별자다.

예를 들어 사용자가 결제 API를 호출하면 Controller, Service, Data Access, 외부 API Client에서 여러 로그가 남는다. 모든 로그에 같은 trace id가 있으면 한 요청의 전체 흐름을 쉽게 찾을 수 있다.

Spring에서는 보통 Filter 또는 Interceptor에서 trace id를 만들고 MDC에 넣는다.

```java
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader("X-Trace-Id"))
            .orElse(UUID.randomUUID().toString());

        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

규칙:

- 모든 요청은 trace id를 가진다.
- API 에러 응답에는 trace id를 포함한다.
- 외부 API 요청 시 가능하면 trace id를 헤더로 전달한다.
- 비동기 메시지에는 correlation id를 포함한다.
- MDC에 넣은 값은 요청 종료 시 반드시 제거한다.

## **예외 로그 규칙**

예외 로그는 중복으로 남기지 않는다.

권장 패턴:

- 예상 가능한 비즈니스 예외는 GlobalExceptionHandler에서 WARN으로 한 번만 남긴다.
- 예상하지 못한 예외는 GlobalExceptionHandler에서 ERROR로 한 번만 남긴다.
- 외부 시스템 호출 실패처럼 원인 위치가 중요한 경우 dataaccess/client 레이어에서 맥락을 포함해 남긴다.

좋은 예시:

```java
log.error("[PaymentClient.approve] Failed to approve payment. paymentId={}, provider={}", paymentId, provider, exception);
```

나쁜 예시:

```java
log.error(exception.getMessage());
```

이유:

- 메시지만 남기면 stack trace가 없어 원인 추적이 어렵다.
- 어떤 결제, 어떤 외부 시스템에서 실패했는지 알 수 없다.

## **외부 연동 로그**

외부 연동은 장애 지점이 되기 쉽기 때문에 더 명확한 로그가 필요하다.

남길 이벤트:

- 요청 시작
- 응답 성공
- 응답 실패
- 타임아웃
- 재시도
- 최종 실패

예시:

```java
log.info("[PaymentClient.approve] Payment provider request started. paymentId={}, provider={}", paymentId, provider);

log.info("[PaymentClient.approve] Payment provider request completed. paymentId={}, provider={}, statusCode={}, elapsedMs={}",
    paymentId, provider, statusCode, elapsedMs);

log.warn("[PaymentClient.approve] Payment provider request retrying. paymentId={}, provider={}, retryCount={}, reason={}",
    paymentId, provider, retryCount, reason);

log.error("[PaymentClient.approve] Payment provider request failed. paymentId={}, provider={}, elapsedMs={}",
    paymentId, provider, elapsedMs, exception);
```

규칙:

- 외부 시스템 이름을 남긴다.
- 내부 식별자와 외부 요청 식별자를 함께 남긴다.
- 처리 시간을 남긴다.
- 요청/응답 전문은 기본적으로 남기지 않는다.
- 전문 로그가 필요하면 마스킹, 보관 기간, 접근 권한을 먼저 합의한다.

## **요청/응답 로그**

모든 API 요청과 응답을 직접 Controller마다 남기지 않는다.

필요하다면 Filter 또는 Interceptor에서 공통으로 남긴다.

권장 필드:

- traceId
- HTTP method
- path
- status code
- elapsed time
- user id
- client ip

예시:

```java
[HttpLoggingFilter.doFilterInternal] HTTP request completed. method=POST, path=/api/v1/payments, status=201, elapsedMs=132, userId=1
```

주의:

- request body와 response body는 기본적으로 남기지 않는다.
- 파일 업로드, 결제, 인증 API는 body 로그를 금지한다.
- 2xx 응답을 너무 많이 남기면 로그 비용이 커질 수 있다.

## **로그 저장과 롤링 전략**

작은 서비스에서는 처음부터 복잡한 로그 수집 시스템을 도입하기보다, 서버 로컬 파일에 안전하게 저장하고 오래된 로그를 자동 정리하는 전략을 기본으로 한다.

기본 원칙:

- 콘솔 로그는 개발 환경과 컨테이너 환경에서 사용한다.
- 운영 서버에서는 파일 로그를 함께 남긴다.
- 로그 파일은 날짜와 용량 기준으로 롤링한다.
- 오래된 로그는 자동 삭제한다.
- 로그 때문에 서버 디스크가 가득 차면 안 된다.

### **권장 보관 기준**

작은 서비스의 기본값은 다음을 권장한다.

| **항목** | **권장값** |
| --- | --- |
| 로그 파일 위치 | /var/log/{service-name} |
| 로그 파일명 | {service-name}.log |
| 롤링 기준 | 일자 + 파일 크기 |
| 파일당 최대 크기 | 100MB |
| 보관 기간 | 14일 |
| 전체 로그 최대 용량 | 2GB |
| 압축 여부 | 지난 로그는 .gz 압축 |

트래픽이 적은 내부 서비스라면 보관 기간은 7일, 전체 용량은 1GB로 줄일 수 있다.

장애 분석이나 정산, 결제처럼 추적 기간이 중요한 서비스는 보관 기간을 30일로 늘릴 수 있다. 단, 디스크 용량과 개인정보 보관 정책을 함께 확인한다.

### **Logback 설정 예시**

Spring Boot에서 Logback을 사용한다면 logback-spring.xml에 다음과 같이 설정할 수 있다.

```xml
<configuration>
    <property name="SERVICE_NAME" value="payment-service"/>
    <property name="LOG_PATH" value="/var/log/${SERVICE_NAME}"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${SERVICE_NAME}.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
        </encoder>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/archive/${SERVICE_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>14</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

설정 의미:

- file: 현재 기록 중인 로그 파일이다.
- fileNamePattern: 롤링된 로그가 저장될 파일명 규칙이다.
- %d{yyyy-MM-dd}: 날짜별로 로그를 나눈다.
- %i: 같은 날짜에 파일이 여러 개 생기면 번호를 붙인다.
- maxFileSize: 파일 하나가 이 크기를 넘으면 새 파일로 넘어간다.
- maxHistory: 며칠 동안 보관할지 정한다.
- totalSizeCap: 전체 로그 파일 용량 상한을 정한다.

### **Profile별 로그 전략**

환경별로 로그 전략을 다르게 둔다.

| **Profile** | **로그 출력** | **로그 레벨** | **목적** |
| --- | --- | --- | --- |
| local | console | DEBUG 가능 | 개발 중 확인 |
| dev | console + file | DEBUG 또는 INFO | 개발 서버 확인 |
| staging | console + file | INFO | 운영 전 검증 |
| prod | console + file | INFO | 운영 추적 |

운영 환경에서는 기본 로그 레벨을 INFO로 둔다.

DEBUG 로그가 필요하면 일시적으로 특정 패키지만 올리고, 분석이 끝나면 다시 내린다.

```yaml
logging:
  level:
    root: INFO
    com.example.project.payment: DEBUG
```

### **디스크 보호 규칙**

작은 서비스일수록 로그가 서버 디스크를 가득 채우는 상황을 조심해야 한다.

규칙:

- totalSizeCap을 반드시 설정한다.
- 로그 디렉터리의 디스크 사용량을 모니터링한다.
- 로그 파일을 직접 삭제하기보다 롤링 정책으로 자동 정리한다.
- 긴급하게 삭제해야 한다면 현재 쓰는 파일보다 archive 파일을 먼저 정리한다.
- 같은 장애가 반복되며 로그가 폭증하면 로그를 줄이기보다 장애 원인을 먼저 해결한다.

운영 점검 명령 예시:

```bash
du -sh /var/log/payment-service
ls -lh /var/log/payment-service/archive
```

### **컨테이너 환경 참고**

Docker나 Kubernetes 환경에서는 애플리케이션이 파일보다 stdout으로 로그를 남기고, 플랫폼이 로그를 수집하는 구성이 흔하다.

작은 서비스에서 단일 서버로 운영한다면 파일 로그 전략을 사용한다. 컨테이너 기반 운영으로 전환하면 다음 기준을 따른다.

- 애플리케이션은 stdout에 로그를 남긴다.
- 로그 파일 롤링은 Docker, Kubernetes, Cloud 로그 수집기가 담당한다.
- 애플리케이션 내부 파일 로그와 플랫폼 로그가 중복 저장되지 않게 한다.

Docker 기본 로그도 용량 제한을 둔다.

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "10"
  }
}
```

### **로그 백업 기준**

작은 서비스에서는 모든 로그를 장기 보관하지 않는다.

장기 보관이 필요한 경우:

- 결제, 정산, 포인트처럼 금전과 관련된 이벤트
- 법적 감사나 CS 추적이 필요한 이벤트
- 보안 사고 분석에 필요한 인증/인가 이벤트

이 경우 애플리케이션 로그 파일을 오래 보관하기보다, 중요한 업무 이벤트를 별도 테이블이나 이벤트 저장소에 남기는 방식을 우선 검토한다.

로그는 장애 분석용 기록이고, 영구 감사 데이터의 대체재가 아니다.

## **흔한 실수**

### **System.out.println 사용**

지양한다.

```java
System.out.println("payment success");
```

이유:

- 로그 레벨 제어가 어렵다.
- trace id 같은 공통 문맥이 붙지 않는다.
- 운영 로그 수집 도구와 일관되게 연결되지 않을 수 있다.

### **객체 전체 출력**

지양한다.

```java
log.info("[PaymentController.pay] request={}", request);
log.info("[UserService.findUser] user={}", user);
```

이유:

- 민감 정보가 섞일 수 있다.
- 로그가 너무 커진다.
- 객체의 toString() 변경에 로그 내용이 영향을 받는다.

### **모든 곳에 로그 남기기**

지양한다.

```java
log.info("[PaymentService.pay] start");
log.info("[PaymentService.pay] step1");
log.info("[PaymentService.pay] step2");
log.info("[PaymentService.pay] end");
```

이유:

- 중요한 로그가 묻힌다.
- 로그 비용이 증가한다.
- 장애 분석에 오히려 방해된다.

## **예시: 결제 성공 흐름**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessor paymentProcessor;

    @Transactional
    public PaymentResult pay(PaymentCommand command) {
        Payment payment = paymentProcessor.pay(command);

        log.info("[PaymentService.pay] Payment completed. paymentId={}, orderId={}, userId={}, amount={}",
            payment.id(), command.orderId(), command.userId(), command.amount());

        return PaymentResult.from(payment);
    }
}
```

포인트:

- 결제가 완료된 후 한 번만 남긴다.
- 비즈니스 식별자인 paymentId, orderId, userId를 포함한다.
- 금액처럼 운영 확인에 필요한 값을 포함한다.

## **예시: 비즈니스 실패**

```java
if (point.isLessThan(command.usePoint())) {
    log.warn("[PaymentProcessor.validatePoint] Payment rejected by insufficient point. userId={}, orderId={}, currentPoint={}, usePoint={}",
        command.userId(), command.orderId(), point, command.usePoint());

    throw new NotEnoughPointException();
}
```

포인트:

- 비즈니스 실패이므로 WARN을 사용한다.
- 어떤 조건 때문에 실패했는지 남긴다.
- 같은 예외가 GlobalExceptionHandler에서 또 로그로 남는다면 중복 여부를 검토한다.

## **리뷰 체크리스트**

- System.out.println 대신 logger를 사용했는가?
- 로그 메시지 맨 앞에 [클래스명.메서드명] prefix를 붙였는가?
- 로그 레벨이 상황에 맞는가?
- 장애 원인을 추적할 수 있는 식별자가 포함되어 있는가?
- 객체 전체를 무심코 출력하지 않았는가?
- 민감 정보가 로그에 남지 않는가?
- 예상 가능한 비즈니스 실패를 ERROR로 남기지 않았는가?
- 예외 로그에 stack trace와 맥락 정보가 함께 남는가?
- 같은 예외를 여러 레이어에서 중복으로 남기지 않았는가?
- 외부 연동 실패에 provider, statusCode, elapsedMs 같은 정보가 있는가?
- 에러 응답과 로그를 trace id로 연결할 수 있는가?
- 운영 로그 파일에 날짜/용량 기반 롤링 정책이 설정되어 있는가?
- maxHistory와 totalSizeCap으로 오래된 로그와 전체 용량을 제한했는가?
- 운영 서버 디스크를 로그가 가득 채우지 않도록 모니터링하는가?
