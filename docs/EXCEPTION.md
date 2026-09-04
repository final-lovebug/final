# Exception Handling

예외 처리는 사용자에게 일관된 실패 응답을 제공하고, 개발자에게 원인을 빠르게 추적할 수 있는 정보를 남기기 위한 규칙이다.

모든 예외는 다음 기준을 만족해야 한다.

- 클라이언트 응답 형식이 일관적이다.
- 비즈니스 실패와 시스템 장애가 구분된다.
- 로그에는 추적 가능한 정보가 남지만 민감 정보는 남지 않는다.
- 예외 메시지는 사용자 노출 메시지와 내부 디버깅 정보를 분리한다.

## **예외 분류**

| **분류** | **의미** | **예시** |
| --- | --- | --- |
| Business(Domain) Exception | 정상적인 비즈니스 규칙 위반 | 잔액 부족, 이미 사용한 쿠폰 |
| Validation Exception | 요청 형식 또는 값 검증 실패 | 필수값 누락, 형식 오류 |
| Authentication Exception | 인증 실패 | 토큰 만료, 로그인 필요 |
| Authorization Exception | 권한 부족 | 본인 리소스 아님, 관리자 권한 필요 |
| Not Found Exception | 대상 리소스 없음 | 존재하지 않는 주문 |
| Conflict Exception | 상태 충돌 | 이미 취소된 주문 재취소 |
| System Exception | 예측하지 못한 서버 오류 | DB 장애, 외부 API 장애 |

## **에러 응답 형식**

API 에러 응답은 다음 형식을 사용한다.

```json
{
  "code": "PAYMENT_NOT_ENOUGH_POINT",
  "message": "사용 가능한 포인트가 부족합니다."
}
```

규칙은 다음과 같다.

- code는 클라이언트가 분기할 수 있는 안정적인 값이다.
- message는 사용자에게 보여줄 수 있는 문장이다.

## **ErrorCode 규칙**

에러 코드는 도메인 단위로 관리한다.

```java
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_ENOUGH_POINT(HttpStatus.BAD_REQUEST, "사용 가능한 포인트가 부족합니다."),
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "이미 결제된 주문입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

네이밍 규칙:

- 형식은 {DOMAIN}_{REASON}을 사용한다.
- 대문자 스네이크 케이스를 사용한다.
- HTTP 상태 코드는 에러 코드가 가진다.
- 같은 의미의 에러 코드를 중복 생성하지 않는다.

## **BusinessException 규칙**

비즈니스 예외는 공통 상위 타입을 사용한다.

```java
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
```

## **GlobalExceptionHandler 규칙**

@RestControllerAdvice에서 예외를 응답으로 변환한다.

처리 우선순위:

1. `BusinessException`
2. `MethodArgumentNotValidException`
3. `ConstraintViolationException`
4. `AuthenticationException`
5. `AccessDeniedException`
6. 예상하지 못한 `Exception`

규칙은 다음과 같다.

- BusinessException은 warn 로그를 남긴다.
- 예상하지 못한 Exception은 error 로그를 남긴다.
- 클라이언트 응답에 stack trace를 포함하지 않는다.
- 외부 API 장애는 내부 예외를 그대로 노출하지 않고 프로젝트 에러 코드로 변환한다.

## **예외를 던지는 위치**

- 요청 형식 검증은 presentation에서 처리한다.
- 비즈니스 규칙 위반은 domain 또는 implement에서 던진다.
- service는 비즈니스 흐름 중 실패를 자연스럽게 전파한다.
- infra의 기술 예외는 가능한 도메인 의미가 있는 예외로 변환한다.

## **리뷰 체크리스트**

- 에러 코드가 도메인 기준으로 명확한가?
- 사용자가 볼 수 없는 내부 정보가 응답에 포함되지 않았는가?
- 예상 가능한 실패를 Exception이나 RuntimeException으로 직접 던지지 않았는가?
- 검증 실패 응답이 필드 단위로 내려가는가?
- 로그 레벨이 실패 성격에 맞는가?
