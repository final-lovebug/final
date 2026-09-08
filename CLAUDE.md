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
  - **루트 `compose.yaml` — 전체 통합 테스트용.** 아직 없으며 추후 추가 예정이다.
  - 로컬 오버라이드가 필요하면 `compose.override.yaml`을 쓰고 `.gitignore`에 등록한다.
- 어떤 경우에도 운영·공용 환경의 `ddl-auto`를 `create`/`create-drop`/`update`로 설정하지 않는다.

---

## 미구성 항목

2026-09-08 기준으로 아직 채워지지 않은 부분이다. 관련 작업을 할 때 함께 정리한다.

- **로컬 `bootRun` 불가.** MySQL·MongoDB 의존성은 있으나 `backend/compose.yaml`에 해당 서비스가 없고 `application.properties`에도 접속 설정이 없다.
- **MongoDB 미연결.** 의존성만 있고 `TestcontainersConfiguration`에 컨테이너 빈이 없다.
- **Flyway 마이그레이션 없음.** `db/migration`이 비어 있어 테스트가 `spring.flyway.enabled=false` + `ddl-auto=create-drop`으로 우회 중이다. 첫 마이그레이션을 추가할 때 이 테스트 설정도 함께 정리한다.
- **`SecurityConfig` 없음.** Spring Security 기본 설정이 적용되면 Swagger UI를 포함한 모든 요청이 인증에 막힌다.
