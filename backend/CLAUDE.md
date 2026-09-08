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
- **메시징** — Kafka
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
| Redis | `redis:latest` |

- MySQL, MongoDB, Kafka는 의존성만 추가되어 있고 `compose.yaml`에는 아직 없다.
  이 상태에서는 해당 저장소에 접속하는 코드를 `bootRun`으로 확인할 수 없다.
- 테스트용 컨테이너는 별도로 `TestcontainersConfiguration`이 관리한다.
  (MySQL, Kafka, Redis, Grafana LGTM — MongoDB는 아직 없다.)
