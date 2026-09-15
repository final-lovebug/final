# CLAUDE.md

이 문서는 Backend 프로젝트 작업 시 따라야 하는 규칙이다.

---

## 기술 스택

> **버전은 이 문서에 적지 않는다.** 정확한 버전은 `build.gradle`,
> `gradle/wrapper/gradle-wrapper.properties`가 기준이다.
> 버전이 필요하면 그 파일을 읽는다.

- **언어** — Java 25 (`toolchain`으로 고정)
- **프레임워크** — Spring Boot 4.x
- **빌드** — Gradle (Groovy DSL)
- **아키텍처** — 모놀리식 / 도메인별 레이어드
- **영속성** — Spring Data JPA + MySQL
- **스키마 마이그레이션** — Flyway
- **캐시·세션** — Redis
- **메시징** — **로컬·테스트는 Spring `ApplicationEvent`(인메모리), AWS 배포는 SQS**(`spring-cloud-aws-starter-sqs`). 어댑터 선택은 `app.messaging.mode` 프로퍼티로 하고 상위 레이어는 `EventPublisher` 포트만 참조한다. 발행 어댑터 둘은 `common/infra/event/`(인메모리)와 `common/infra/event/sqs/`(SQS)에 있고 `@ConditionalOnProperty`로 배타 선택된다. 수신 어댑터는 소비 도메인에 두며 **둘이 같은 공용 핸들러에 위임한다.** 오래 걸리는 작업(용어 추출·문서 대조)은 DB 작업 테이블로 상태를 관리하고 조회는 폴링이다. **큐는 둘이다** — 도메인 이벤트 큐(`app.messaging.sqs.queue`)와 **AI 워커 요청 큐**(`app.messaging.sqs.llm-request-queue`, `D-67`). **AI 워커 요청 큐는 `app.messaging.mode`와 무관하게 `app.ai.dispatch.mode`로 갈린다** — `local`·`test`는 인프로세스 대역, `dev`는 LocalStack, `prod`는 실 SQS다. **완료 통보는 큐가 아니라 워커가 치는 동기 HTTP 콜백(`/api/internal/llm/**`)이다**(`D-68`). 계약은 `docs/AI_CONTRACT.md`가 원본이다
- **인증·인가** — Spring Security, JWT (JJWT), OAuth2
- **API 문서** — SpringDoc OpenAPI (Swagger UI)
- **관측** — Actuator, Micrometer, OpenTelemetry. **세 신호(트레이스·메트릭·로그)를 OTLP로 내보낸다.** 받는 쪽은 프로파일마다 다르다 — 로컬·테스트는 `grafana/otel-lgtm` 컨테이너, `prod`는 **Grafana Cloud 직행**이다(`D-92`·`D-94`). 수집기 사이드카를 두지 않는다. **어느 설정 파일에도 수집기 주소를 적지 않는다** — 로컬은 `spring-boot-docker-compose`가, 테스트는 `@ServiceConnection LgtmStackContainer`가 실제로 매핑된 포트를 읽어 주입하고, Grafana Cloud 주소는 Parameter Store에서 온다. 로그는 `logback-spring.xml`의 `CONSOLE`(stdout) + `OTEL`(OTLP) 두 appender로 나가며 **파일 appender를 두지 않는다**(`D-93`). `traceId`는 Micrometer Tracing이 MDC에 넣는 값을 쓰고 **별도 필터를 만들지 않는다**(`D-96`, `docs/LOG.md`)
- **품질 도구** — Spotless(포맷팅, `palantirJavaFormat`)
- **설정·시크릿** — AWS Parameter Store (Spring Cloud AWS, `prod` 프로필 전용)
- **인프라** — Docker / Docker Compose, AWS

### Spring Boot 4 주의사항

- **Java 25 문법 범위를 넘지 않는다.** 상위 버전 문법을 쓰지 않는다.
- Spring Boot 3.x 기준의 블로그·예제 코드를 그대로 복사하지 않는다. 특히 **Security 설정, 프로퍼티 키, 자동 구성 클래스 위치**가 달라진 부분이 있다.
- 새 HTTP 클라이언트는 `RestClient` / 선언적 HTTP 인터페이스를 우선 사용한다.

### Flyway 마이그레이션 파일 규칙

**현재 마이그레이션은 `V1__init_schema.sql` 하나다.** 도메인별로 나뉘어 있던 31개(V1~V900)를
이것으로 합쳤다 — 뒤따르던 `ALTER`가 전부 최종 컬럼 정의에 반영돼 있어 이 파일이 곧 현재 스키마다.
합치기 전 파일이 필요하면 git 이력에서 본다.

> ⚠️ **이미 마이그레이션이 적용된 DB는 이 변경으로 깨진다.** `flyway_schema_history`에 남은
> 31개 행에 대응하는 파일이 없어 검증이 실패하고 `V1`의 체크섬도 달라진다. 해당 DB는 스키마와
> 이력 테이블을 비우고 새로 받는다.

새 마이그레이션은 아래 대역 규칙을 그대로 이어서 붙인다. 도메인별로 버전 대역을 다르게 생성하여
서로 다른 도메인 작업 간 충돌을 막는다.

- 1 - 99: member 도메인
- 100 - 199: workspace 도메인
- 200 - 299: document 도메인
- 300 - 399: dictionary 도메인
- 400 - 499: draftdocument 도메인
- 500 - 599: draftdictionary 도메인
- 600 - 699: reviewrequest 도메인
- 700 - 799: notification 도메인
- 800 - 899: revisionlog 도메인
- 900 - 999: 공통 / 사후 정리

---

## 개발 명령어

모든 명령어는 `backend/` 디렉터리에서 실행한다.

```bash
# 실행
./gradlew bootRun
./gradlew bootRun --args='--spring.profiles.active=local'

# 빌드 (테스트 포함)
./gradlew build

# 테스트
./gradlew test                              # 전체
./gradlew test --tests '*XxxServiceTest'    # 단일 테스트

# 포맷팅
./gradlew spotlessApply              # 코드 포맷 적용 (커밋 전 필수)
./gradlew check                      # 포맷·테스트 일괄 검증
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 커밋 전 최소 검증: `./gradlew spotlessApply && ./gradlew check`

### 로컬 인프라

`backend/compose.yaml`은 `spring-boot-docker-compose`(`developmentOnly`)가 관리한다.
**`docker compose up -d`를 직접 실행하지 않는다.** `bootRun`이 컨테이너를 자동으로
띄우고 접속 정보를 애플리케이션에 주입하므로, 로컬 실행에 필요한 서비스는
`compose.yaml`에 정의하는 것으로 충분하다.

현재 `compose.yaml`이 제공하는 서비스는 다음과 같다.

| 서비스 | 이미지 |
| --- | --- |
| Grafana LGTM | `grafana/otel-lgtm:0.32.1` |
| MySQL | `mysql:8.4` |
| Redis | `redis:8.2-alpine` |
| LocalStack (SQS) | `localstack/localstack:4` |

- MySQL의 계정과 데이터베이스 이름은 `compose.yaml`의 환경변수로만 정의한다.
  `spring-boot-docker-compose`가 이 값을 읽어 접속 정보를 주입하므로
  `application.yml`에 접속 설정을 적지 않는다. 로컬 전용 값이므로
  운영 계정과 같은 값을 쓰지 않는다.
- 테스트용 컨테이너는 별도로 `TestcontainersConfiguration`이 관리한다.
  (MySQL, Redis, Grafana LGTM, LocalStack — 이미지 태그는 `compose.yaml`과 맞춘다.)
- **도메인 이벤트** 버스는 로컬·테스트에서 인메모리 어댑터를 쓴다(`app.messaging.mode`).
  **AI 워커 요청 큐는 다르다** — `dev` 프로파일과 통합 테스트가 **LocalStack으로 실제 SQS 경로를 탄다**(`D-74`).
  컨테이너는 `compose.yaml`과 `TestcontainersConfiguration` **양쪽에** 들어 있다 — 한쪽만 있으면
  테스트가 로컬에서만 돌거나 그 반대가 된다.
- LocalStack 포트(4566)를 고정한 이유는 `spring-boot-docker-compose`가 LocalStack 커넥션 정보를
  자동 주입하지 않기 때문이다. `application-dev.yml`이 그 주소를 직접 가리킨다.
  테스트는 Testcontainers가 띄우므로 포트가 무엇이든 `DynamicPropertyRegistrar`가 주입한다.
- **큐를 미리 만들지 않는다.** `spring.cloud.aws.sqs.queue-not-found-strategy: create`가 첫 접근에 만든다.
  **`prod`는 기본값(`fail`)을 유지한다** — 큐 이름을 틀린 채 조용히 새 큐가 생기는 것을 막는다(`D-78`).

### 환경변수

로컬에서 실행하려면 다음 환경변수를 직접 설정한다. 저장소에는 값 대신
`${VAR_NAME}` 형태로만 참조하고, 실제 값은 커밋하지 않는다.

| 변수 | 용도 | 로컬 기본값 |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID (Google Cloud Console에서 발급) | 없음 — 반드시 설정해야 함 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿 | 없음 — 반드시 설정해야 함 |
| `JWT_SECRET` | JWT 서명 키 (HMAC-SHA, 최소 256비트/32바이트 이상 필요) | `application.yml`에 로컬 전용 기본값이 있어 설정 안 해도 `bootRun`/테스트가 동작함 |
| `OAUTH_FRONTEND_REDIRECT_URI` | Google 로그인 성공/실패 후 서버가 리다이렉트할 프론트엔드 URL(교환 코드를 쿼리 파라미터로 붙임) | `http://localhost:5173/oauth/callback` — 프론트 dev 서버(Vite) 주소. 배포 시 실제 프론트 도메인으로 교체 |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 origin(콤마로 여러 개 지정 가능). refresh token이 쿠키 기반이라 `*` 불가 | `http://localhost:5173` — 프론트 dev 서버 주소. 배포 시 실제 프론트 도메인으로 교체 |
| `MEMBER_ENCRYPTION_KEY` | `member.email`/`member.display_name` 컬럼 AES-256 암호화 키(`MemberFieldEncryptor`) | `application.yml`에 로컬 전용 기본값이 있어 설정 안 해도 동작함 |
| `OTEL_SAMPLE_RATIO` | 트레이스 샘플링 비율. **`prod`에서만 읽는다** | `prod` 기본값 `0.1`. 로컬·테스트는 전량(`1.0`) 수집한다 |
| `OTEL_SERVICE_VERSION` | 리소스 속성 `service.version`. 어느 리비전이 낸 텔레메트리인지 구분하는 축 | 배포에서는 `deploy/scripts/start_container.sh`가 이미지 태그(커밋 SHA)를 넣는다. 없으면 `unknown` |
| `OTEL_AUTH_HEADER` | OTLP `Authorization` 헤더 **수동 덮어쓰기**. 평소에는 쓰지 않는다 | 없음. Parameter Store의 `/lovebug/otel/auth`에서 파생된 값이 기본이며, 이 변수는 자격증명을 손으로 바꿔 넣어 볼 때만 쓴다 |

- `JWT_SECRET`은 값을 아예 안 정해도 테스트가 깨지지 않도록 `application.yml`에
  `${JWT_SECRET:로컬 전용 기본값}` 형태의 기본값을 뒀다. 이 기본값은 공개돼 있어
  **보안 목적이 아니며, 실제 배포 환경에서는 반드시 실제 값으로 덮어써야 한다.**
  로컬에서 직접 만들려면 `openssl rand -base64 48`로 생성한 값을 쓰면 된다.
- `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`은 기본값이 없다 — 값이 없으면 OAuth2
  클라이언트 등록 프로퍼티가 빈 값 취급되지만, 로그인 API를 실제로 호출하기 전까지는
  기동 자체는 막히지 않는다(값 검증은 실제 로그인 시도 시점에 이뤄짐).
- refresh token 쿠키의 `Secure` 플래그(`app.auth.cookie.secure`)는 기본 `true`이고,
  `application-local.yml`(`spring.profiles.active=local`)이 `false`로 덮어쓴다.
  `http://localhost`에서는 `Secure` 쿠키를 브라우저가 돌려보내지 않기 때문이다.
- `MEMBER_ENCRYPTION_KEY`도 `JWT_SECRET`과 같은 이유로 로컬 전용 기본값이 있다 — **실제
  배포 환경에서는 반드시 실제 값으로 덮어써야 한다.** 이 값을 바꾸면 기존에 암호화 저장된
  email/displayName을 더 이상 복호화할 수 없으니(키가 달라지면 GCM 인증 실패) 운영에서
  한 번 정하면 함부로 바꾸지 않는다.

### 프로파일

프로파일 파일은 넷이다. **공통 기본값은 `application.yml` 한 곳에만 두고 각 프로파일은 차이만 덮는다**(`D-46`).

| 프로파일 | 파일 | 용도 |
| --- | --- | --- |
| (없음) | `application.yml` | 모든 프로파일의 기본값. `app.crossdomain.*`·`app.messaging.mode`·`app.ai.*`가 여기 있다 |
| `local` | `application-local.yml` | 로컬 실행. `http://localhost`에서 `Secure` 쿠키가 돌아오지 않으므로 끈다 |
| `dev` | `application-dev.yml` | 개발 서버. 시크릿을 환경변수로 받고 Parameter Store를 쓰지 않는다 |
| `prod` | `application-prod.yml` | 운영. Parameter Store에서 시크릿을 읽는다 |
| `test` | `src/test/resources/application-test.yml` | 테스트. DB·Redis 접속은 Testcontainers가 주입하므로 여기 적지 않는다 |

**AWS Parameter Store는 기본 off다.** `spring.cloud.aws.parameterstore.enabled`가 켜져 있으면
`SsmClient` 빈이 곧바로 만들어져 로컬·테스트에서도 AWS 리전을 요구하고, 리전이 없으면 컨텍스트가
뜨지 않는다. 그래서 `application.yml`이 `false`로 두고 **`prod`만 `true`로 켠다** — 시크릿을
Parameter Store에서 읽는 프로파일이 그것뿐이다.

**`spring.profiles.active` 기본값을 두지 않는다.** `bootRun`은 무-프로파일로 뜨고, 로컬은
`./gradlew bootRun --args='--spring.profiles.active=local'`로 명시한다. 테스트는
`IntegrationTestSupport`·`RepositoryTestSupport`의 `@ActiveProfiles("test")`가 지정한다.

### 운영 설정 — AWS Parameter Store

운영(`prod` 프로필)에서는 위 환경변수를 쓰지 않고 **AWS Parameter Store의 `/lovebug/` 이하**
값을 애플리케이션이 직접 읽는다. `application-prod.yml`의
`spring.config.import: aws-parameterstore:/lovebug/`가 담당한다.

파라미터 이름은 접두어 `/lovebug/`를 뗀 뒤 `/`를 `.`으로 바꾼 프로퍼티로 노출된다.
예를 들어 `/lovebug/rds/password`는 `rds.password`로 읽힌다.

| 파라미터 | 타입 | 매핑되는 프로퍼티 |
| --- | --- | --- |
| `/lovebug/rds/url` | String | `spring.datasource.url` |
| `/lovebug/rds/username` | String | `spring.datasource.username` |
| `/lovebug/rds/password` | SecureString | `spring.datasource.password` |
| `/lovebug/jwt/secret` | SecureString | `app.jwt.secret` |
| `/lovebug/oauth/google/client-id` | String | `spring.security.oauth2....google.client-id` |
| `/lovebug/oauth/google/client-secret` | SecureString | `spring.security.oauth2....google.client-secret` |
| `/lovebug/oauth/frontend-redirect-uri` | String | `app.oauth.frontend-redirect-uri` |
| `/lovebug/redis/host` | String | `spring.data.redis.host` |
| `/lovebug/redis/port` | String | `spring.data.redis.port` |
| `/lovebug/otel/endpoint` | String | OTLP 게이트웨이 주소. `/v1/traces`·`/v1/logs`·`/v1/metrics`를 **애플리케이션이 붙인다** |
| `/lovebug/otel/auth` | SecureString | Grafana Cloud 쓰기 토큰(`instanceID:token` 원문). `OtlpAuthHeaderEnvironmentPostProcessor`가 base64로 인코딩해 `otel.auth-header`를 만든다 |
| `/lovebug/otel/enabled` | String | **선택.** 세 익스포터의 킬 스위치. 없으면 켠 것으로 본다 |

- **`/lovebug/otel/*`를 뺀 나머지는 하나라도 없으면 기동이 실패한다.** 플레이스홀더에 기본값을
  두지 않는 것은 운영에서 시크릿이 조용히 로컬 기본값으로 떨어지는 것을 막기 위함이다.
  관측 셋만 예외이며 그 이유는 아래에 적는다.
- Redis 운영 설정은 `application-prod.yml`에서 **이미 활성**이며 `/lovebug/redis/host`,
  `/lovebug/redis/port`를 읽는다(TLS 켜짐). 표의 파라미터와 마찬가지로 **없으면 기동이
  실패한다.**
- 리전은 컨테이너에 주입되는 `AWS_REGION`(`deploy/scripts/start_container.sh`)에서 결정된다.
- EC2 인스턴스 역할에 다음 권한이 필요하다.
  `ssm:GetParametersByPath`(리소스 `arn:aws:ssm:<region>:<account>:parameter/lovebug/*`)와
  SecureString 복호화용 `kms:Decrypt`.
- 로컬·테스트는 이 프로필을 쓰지 않으므로 AWS 호출이 발생하지 않는다.
- **관측 파라미터 셋 중 `endpoint`·`auth`는 없어도 기동이 막히지 않는다.** 위 시크릿들과 달리
  플레이스홀더에 빈 기본값을 뒀다 — 관측 때문에 서비스가 죽는 것을 막기 위해서다(`D-98`). 대신
  **조용히 실패한다.** 값이 잘못됐는지 확인하려면 `docker logs spring`에서
  `OtlpAuthHeaderEnvironmentPostProcessor`가 남긴 `instanceId=` 줄을 본다. 그 줄이 없으면 파라미터를
  읽지 못한 것이다.
- ⚠️ **`spring.config.import: aws-parameterstore:/lovebug/`가 경로 하위를 통째로 끌어오므로
  Grafana Cloud 쓰기 토큰이 애플리케이션 `Environment`에 평문으로 상주한다**(`D-95`). 막는 것은
  `management.endpoints.web.exposure.exclude: env,configprops`와 `SecurityConfig`의 경로 축소
  둘뿐이고 **해소가 아니라 완화다.** 프로퍼티를 통째로 찍는 디버깅 코드나 예외 로그를 넣지 않는다.
