# **목표**

코드 스타일 컨벤션은 개인 취향을 줄이고, 팀 전체가 같은 리듬으로 코드를 읽고 수정하기 위한 기준이다.

## **Java 기본 규칙**

- 클래스명은 명사 또는 역할이 드러나는 명사구를 사용한다.
- 메서드명은 동사로 시작한다.
- boolean 메서드는 is, has, can, should를 사용한다.
- 약어는 일반적으로 대문자 연속 사용을 피한다. 예: ApiClient, UrlParser
- null 반환보다 빈 컬렉션, Optional, 예외 중 의미에 맞는 방식을 선택한다.

## **Spring Bean 네이밍**

| **역할** | **접미사 예시** |
| --- | --- |
| Controller | OrderController |
| Service | OrderService |
| Reader | OrderReader |
| Appender | OrderAppender |
| Updater | OrderUpdater |
| Validator | OrderValidator |
| Calculator | PriceCalculator |
| Repository | OrderRepository |
| Client | OrderApiClient |

## **메서드 작성 규칙**

- public 메서드는 의도가 분명해야 한다.
- private 메서드는 복잡한 구현을 숨기기 위해 사용하되 과도하게 쪼개지 않는다.
- 한 메서드 안에서 추상화 수준을 섞지 않는다.
- 중첩 if가 깊어지면 early return, 정책 객체, validator 분리를 검토한다.
- for 반복문이 복잡해지면 컬렉션 처리 책임을 별도 객체로 분리한다.

## **Lombok 규칙**

허용한다.

- @Getter
- @RequiredArgsConstructor
- @NoArgsConstructor(access = AccessLevel.PROTECTED)
- @Builder는 테스트 fixture 중심으로 제한적 사용

지양한다.

- Entity의 무분별한 @Setter
- Entity의 @Data
- 순환 참조 가능성이 있는 객체의 @ToString
- equals/hashCode 기준이 불명확한 Entity의 @EqualsAndHashCode

## **주석 규칙**

- 코드가 "무엇을 하는지" 설명하는 주석은 피한다.
- 코드만으로 드러나지 않는 "왜 그렇게 했는지"는 주석으로 남긴다.
- 외부 정책, 법적 요구, 장애 회피 로직은 근거를 남긴다.

## **상수 규칙**

- 의미 있는 도메인 값은 상수보다 값 객체 또는 enum을 우선 검토한다.
- 단순 숫자 리터럴은 의미가 드러나는 이름으로 분리한다.
- 테스트에서만 쓰는 값은 테스트 fixture에 둔다.

## **리뷰 체크리스트**

- 클래스 이름이 역할을 정확히 설명하는가?
- 메서드 추상화 수준이 일정한가?
- Lombok이 객체의 불변성과 캡슐화를 깨지 않는가?
- 주석이 코드의 이유를 설명하는가?
- 도메인 의미가 원시 타입에 과하게 흩어져 있지 않은가?
