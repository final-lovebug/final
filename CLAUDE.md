# CLAUDE.md

이 문서는 Claude Code(및 팀원)가 이 저장소에서 작업할 때 따르는 규칙이다.

---

## 프로젝트 개요

팀 문서의 형상관리와 **유비쿼터스 언어(Ubiquitous Language) 정합성**을 맞춰주는 협업 서비스.

- 여러 사용자가 작성한 문서를 분석해 **유비쿼터스 단어 사전**을 생성한다.
- 같은 의미이지만 문서마다 다르게 표현된 단어를 탐지해 사전 기준으로 정렬한다.
- 문서가 생성/변경되며 등장한 신규 단어를 사전에 반영(업데이트)한다.

---

## 문서 맵

다음 문서들은 반드시 읽고 하위 프로젝트에서 적용한다.

- `CLAUDE.md` (이 문서) — 프로젝트 공통 적용 규칙
- `docs/ARCHITECTURE.md` — 패키지 구조, 레이어 규칙
- `docs/CODE_STYLE.md` — 코드 작성 규칙
- `docs/LOG.md` — 로깅 코드 작성 규칙
- `docs/EXCEPTION.md` — 예외 처리 코드 작성 규칙
- `docs/TEST.md` — 테스트 코드 작성 규칙
- `docs/API.md` — API 요청/응답 규격, 공통 규칙

기획·도메인 문서는 구현 전에 읽고, 결정이 바뀌면 코드보다 먼저 갱신한다.

- `docs/REQUIREMENTS.md` — 요구사항 목록(MVP 구분, 우선순위, 진행 상태)
- `docs/DOMAIN.md` — 엔티티 속성 표, 관계, 정책·제약
- `docs/UBIQUITOUS_LANGUAGE.md` — 도메인별 유비쿼터스 언어 사전

하위 프로젝트에서 작업할 때는 해당 프로젝트의 문서를 함께 읽는다.

- `backend/CLAUDE.md` — Backend 기술 스택, 개발 명령어, 로컬 인프라

---

## AI 에이전트 작업 규칙

### 반드시 지킬 것

- **문서에 "미확정"으로 표시된 항목은 임의로 확정하지 않는다.** 결정이 필요하면 질문하고, 합의된 뒤 해당 문서에 먼저 반영한다.
- **테스트 없이 기능을 완료로 보고하지 않는다.** 새 로직에는 테스트를 함께 작성한다.
- 테스트가 실패하면 **테스트를 삭제하거나 조건을 완화하지 말고** 원인을 보고한다.
- 새 라이브러리·의존성 추가는 **사전에 제안하고 승인받는다.**

### 금지

- `git commit`, `git push`, `git reset --hard`, 브랜치 삭제 등 **git 상태를 바꾸는 명령을 사용자 지시 없이 실행하지 않는다.**
- `application.properties`, 인프라 설정, 시크릿, `.env` 파일을 임의로 변경하지 않는다.
- 운영 DB·AWS 리소스에 영향을 주는 명령 실행 금지.

---

## 환경 · 설정

- 시크릿은 **환경변수 또는 AWS 파라미터 스토어**로 주입한다. 저장소에 커밋 금지.
- Compose 파일은 **역할에 따라 둘로 나눈다.** 파일명은 `compose.yaml`을 쓰고, `docker-compose.yml`을 새로 만들지 않는다. 포트·이미지 태그를 임의로 바꾸지 않는다.
  - **`backend/compose.yaml` — 로컬 실행용.** `spring-boot-docker-compose`(`developmentOnly`)가 관리한다. `bootRun`이 컨테이너를 자동으로 띄우고 접속 정보를 주입하므로 `docker compose up -d`를 직접 실행하지 않는다.
  - **루트 `compose.yaml` — 전체 통합 테스트용.** `backend/Dockerfile`로 이미지를 빌드해 MySQL·MongoDB·Redis·Grafana LGTM과 함께 띄운다. 이쪽은 `docker compose up -d`로 직접 기동한다.
    - 첫 실행은 `docker compose build backend`를 먼저 돌린다. LGTM 스택과 Gradle 빌드 JVM이 겹치면 Docker Desktop 기본 메모리에서 빌드가 OOM으로 죽는다.
    - 노출 포트는 **앱 포트만** — backend `8080`, Grafana UI `3000`. DB는 Compose 네트워크 내부로만 접근한다.
    - **`bootRun`과 동시에 띄우지 않는다.** Compose 프로젝트는 분리되지만 호스트 포트 3000·8080이 겹쳐 `port is already allocated`로 죽는다.
    - 접속 정보는 전부 환경변수로 주입한다. `application.properties`에 적지 않는다.
  - 로컬 오버라이드가 필요하면 `compose.override.yaml`을 쓰고 `.gitignore`에 등록한다.
- 어떤 경우에도 운영·공용 환경의 `ddl-auto`를 `create`/`create-drop`/`update`로 설정하지 않는다.

---

## 미구성 항목

2026-09-08 기준으로 아직 채워지지 않은 부분이다. 관련 작업을 할 때 함께 정리한다.

- **Flyway 마이그레이션 없음.** `db/migration`이 비어 있어 테스트가 `spring.flyway.enabled=false` + `ddl-auto=create-drop`으로 우회 중이다. 첫 마이그레이션을 추가할 때 이 테스트 설정도 함께 정리한다.
  - 루트 `compose.yaml`은 `ddl-auto`를 설정하지 않는다. `flyway.fail-on-missing-locations` 기본값이 `false`이고 `ddl-auto` 기본값이 `none`이라 **기동은 성공하고 첫 쿼리에서 `Table doesn't exist`로 실패**한다. "초록불인데 안 돌아가는" 상태이므로 스키마 부재를 기동 실패로 오해하지 않는다.
- **`SecurityConfig` 없음.** Spring Security 기본 설정이 적용되면 Swagger UI를 포함한 모든 요청이 인증에 막힌다. (`/actuator/health`는 예외로 통과한다.)
  - 그래서 루트 `compose.yaml`의 backend healthcheck는 HTTP가 아니라 bash 내장 `/dev/tcp`로 리스닝만 확인한다(`eclipse-temurin:25-jre`에는 `curl`·`wget`이 없다). `SecurityConfig`를 추가하면 `/actuator/health`로 교체한다.
- **관측 배포 경로 미확정 — CloudWatch 유력.** 로컬 `grafana/otel-lgtm`은 OTLP 수신기이고, CloudWatch도 OTLP를 직접 받으므로(트레이스는 X-Ray `/v1/traces`, 메트릭은 CloudWatch OTLP) **엔드포인트 URL만 바뀐다.** 전환 시 세 가지를 지킨다.
  - AWS OTLP 엔드포인트는 SigV4를 요구하고 Spring의 OTLP exporter는 서명을 못 한다 → **ADOT Collector 사이드카**를 경유한다. 앱은 그대로 `localhost:4318`로 보낸다.
  - **트레이스 샘플링을 낮춘다.** 로컬의 `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0`을 그대로 두면 권장치(5%) 대비 최대 20배 인제스트 비용이 된다.
  - **로그는 OTLP로 보내지 않는다.** stdout + `awslogs`/FireLens가 표준이다(`docs/LOG.md`). 애초에 `spring-boot-starter-opentelemetry`에 Logback → OTel 브리지가 없어 OTLP 로그 전송이 동작하지 않는다.
  - CloudWatch는 커스텀 메트릭 개수로 과금한다. Micrometer 태그 카디널리티와 `management.otlp.metrics.export.step`(기본 1분)을 함께 본다.
- **메시징 배포 대상 미확정.** Kafka 의존성은 제거했고, 로컬은 Spring `ApplicationEvent` 인메모리 어댑터로 동작한다. 배포 환경에서 쓸 메시지 큐가 정해지면 해당 어댑터와 로컬 대체 컨테이너를 함께 추가한다. 이벤트 발행 규약은 `docs/ARCHITECTURE.md`를 따른다.
