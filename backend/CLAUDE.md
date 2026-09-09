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
- **영속성** — Spring Data JPA + MySQL, Spring Data MongoDB + MongoDB
- **스키마 마이그레이션** — Flyway
- **캐시·세션** — Redis
- **메시징** — Spring `ApplicationEvent`(인메모리). 배포용 어댑터는 미확정
- **인증·인가** — Spring Security, JWT (JJWT), OAuth2
- **API 문서** — SpringDoc OpenAPI (Swagger UI)
- **관측** — Actuator, Micrometer(Prometheus), OpenTelemetry / Grafana LGTM
- **품질 도구** — Spotless(포맷팅, `palantirJavaFormat`)
- **인프라** — Docker / Docker Compose, AWS

### Spring Boot 4 주의사항

- **Java 25 문법 범위를 넘지 않는다.** 상위 버전 문법을 쓰지 않는다.
- Spring Boot 3.x 기준의 블로그·예제 코드를 그대로 복사하지 않는다. 특히 **Security 설정, 프로퍼티 키, 자동 구성 클래스 위치**가 달라진 부분이 있다.
- 새 HTTP 클라이언트는 `RestClient` / 선언적 HTTP 인터페이스를 우선 사용한다.

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
| Grafana LGTM | `grafana/otel-lgtm:latest` |
| MongoDB | `mongo:8.0` |
| MySQL | `mysql:8.4` |
| Redis | `redis:latest` |

- MySQL·MongoDB의 계정과 데이터베이스 이름은 `compose.yaml`의 환경변수로만 정의한다.
  `spring-boot-docker-compose`가 이 값을 읽어 접속 정보를 주입하므로
  `application.properties`에 접속 설정을 적지 않는다. 로컬 전용 값이므로
  운영 계정과 같은 값을 쓰지 않는다.
- 테스트용 컨테이너는 별도로 `TestcontainersConfiguration`이 관리한다.
  (MySQL, MongoDB, Redis, Grafana LGTM — 이미지 태그는 `compose.yaml`과 맞춘다.)
- 메시징은 인메모리 어댑터를 사용하므로 로컬 인프라가 필요 없다. 배포용 어댑터를
  추가할 때 대응하는 로컬 컨테이너를 `compose.yaml`과 테스트에 함께 넣는다.

### 환경변수

로컬에서 실행하려면 다음 환경변수를 직접 설정한다. 저장소에는 값 대신
`${VAR_NAME}` 형태로만 참조하고, 실제 값은 커밋하지 않는다.

| 변수 | 용도 | 로컬 기본값 |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | Google OAuth2 클라이언트 ID (Google Cloud Console에서 발급) | 없음 — 반드시 설정해야 함 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿 | 없음 — 반드시 설정해야 함 |
| `JWT_SECRET` | JWT 서명 키 (HMAC-SHA, 최소 256비트/32바이트 이상 필요) | `application.properties`에 로컬 전용 기본값이 있어 설정 안 해도 `bootRun`/테스트가 동작함 |
| `OAUTH_FRONTEND_REDIRECT_URI` | Google 로그인 성공/실패 후 서버가 리다이렉트할 프론트엔드 URL(교환 코드를 쿼리 파라미터로 붙임) | `http://localhost:3000/oauth/callback` — 프론트 미확정이라 임시값. 실제 프론트 주소가 정해지면 교체 |

- `JWT_SECRET`은 값을 아예 안 정해도 테스트가 깨지지 않도록 `application.properties`에
  `${JWT_SECRET:로컬 전용 기본값}` 형태의 기본값을 뒀다. 이 기본값은 공개돼 있어
  **보안 목적이 아니며, 실제 배포 환경에서는 반드시 실제 값으로 덮어써야 한다.**
  로컬에서 직접 만들려면 `openssl rand -base64 48`로 생성한 값을 쓰면 된다.
- `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`은 기본값이 없다 — 값이 없으면 OAuth2
  클라이언트 등록 프로퍼티가 빈 값 취급되지만, 로그인 API를 실제로 호출하기 전까지는
  기동 자체는 막히지 않는다(값 검증은 실제 로그인 시도 시점에 이뤄짐).
- refresh token 쿠키의 `Secure` 플래그(`app.auth.cookie.secure`)는 기본 `true`이고,
  `application-local.properties`(`spring.profiles.active=local`)가 `false`로 덮어쓴다.
  `http://localhost`에서는 `Secure` 쿠키를 브라우저가 돌려보내지 않기 때문이다.
