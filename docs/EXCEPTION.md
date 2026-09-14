# Exception Handling

예외 처리는 클라이언트에 일관된 실패 응답을 주고, 개발자에게 원인을 추적할 정보를 남기기 위한 규칙이다. 비즈니스 실패와 시스템 장애를 구분하고, 사용자 노출 메시지와 내부 디버깅 정보를 분리한다.

## **에러 응답 형식**

API 에러 응답은 다음 형식을 사용한다.

```json
{
  "code": "PAYMENT_NOT_ENOUGH_POINT",
  "message": "사용 가능한 포인트가 부족합니다.",
  "errors": [
    { "field": "usePoint", "message": "0보다 커야 합니다." }
  ]
}
```

- `code`는 클라이언트가 분기할 수 있는 안정적인 값이고, `message`는 사용자에게 보여줄 수 있는 문장이다.
- `errors`는 **요청 값 검증 실패일 때만** 필드 단위로 내려간다. 검증 실패가 아니면 응답에서 빠진다.

## **ErrorCode 규칙**

에러 코드는 도메인 단위로 관리한다.

```java
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_ENOUGH_POINT(HttpStatus.BAD_REQUEST, "사용 가능한 포인트가 부족합니다."),
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "이미 결제된 주문입니다.");
}
```

- 형식은 `{DOMAIN}_{REASON}`, 대문자 스네이크 케이스를 사용한다.
- HTTP 상태 코드는 에러 코드가 가지며, 같은 의미의 에러 코드를 중복 생성하지 않는다.

## **BusinessException 규칙**

비즈니스 예외는 `ErrorCode`를 담은 공통 상위 타입 `BusinessException`(RuntimeException)을 사용한다.

## **GlobalExceptionHandler 규칙**

`@RestControllerAdvice`에서 예외를 응답으로 변환한다. 처리 우선순위는 `BusinessException` → `MethodArgumentNotValidException` → `ConstraintViolationException` → `AuthenticationException` → `AccessDeniedException` → 예상하지 못한 `Exception` 순이다.

- `BusinessException`은 warn, 예상하지 못한 `Exception`은 error 로그를 남긴다.
- 클라이언트 응답에 stack trace를 포함하지 않는다.
- 외부 API 장애는 내부 예외를 그대로 노출하지 않고 프로젝트 에러 코드로 변환한다.

## **예외를 던지는 위치**

- 요청 형식 검증은 presentation에서 처리한다.
- 비즈니스 규칙 위반은 domain 또는 implement에서 던진다.
- service는 비즈니스 흐름 중 실패를 자연스럽게 전파한다.
- infra의 기술 예외는 가능한 도메인 의미가 있는 예외로 변환한다.

## **리뷰 체크리스트**

- 에러 코드가 도메인 기준으로 명확하고, 사용자가 볼 수 없는 내부 정보가 응답에 포함되지 않았는가?
- 예상 가능한 실패를 Exception이나 RuntimeException으로 직접 던지지 않았는가?
- 검증 실패 응답이 필드 단위로 내려가고, 로그 레벨이 실패 성격에 맞는가?
