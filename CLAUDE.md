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
- `docs/AI_CONTRACT.md` — **Spring Boot ↔ FastAPI 워커 계약**(요청 메시지·콜백 스키마, 워커가 읽는 테이블, 재시도 규약). AI 워커를 붙이거나 계약을 바꿀 때 읽는다

기획·도메인 문서는 구현 전에 읽고, 결정이 바뀌면 코드보다 먼저 갱신한다.

- `docs/REQUIREMENTS.md` — 요구사항 목록(MVP 구분, 우선순위, 진행 상태)
- `docs/DOMAIN.md` — 엔티티 속성 표, 관계, 정책·제약
- `docs/UBIQUITOUS_LANGUAGE.md` — 도메인별 유비쿼터스 언어 사전

도메인 구현 계획은 `docs/plan/`에 있다. 해당 도메인을 구현할 때 읽는다. **7개 도메인이 같은 13절 목차를 쓴다.**

- `docs/plan/CONFLICTS.md` — **결정·충돌 인벤토리. 어느 도메인을 구현하든 함께 읽는다.** 확정된 결정(`G-*`·`D-*`), **뒤집힌 결정(`R-*`)**, 문서·코드 충돌 목록이 여기 모여 있다. 계획 문서의 서술과 `R-*`가 어긋나면 `R-*`를 따른다
- `docs/plan/EXECUTION_ORDER.md` — **여러 세션으로 나눠 구현할 때 먼저 읽는다.** 태스크 ID 접두사, 도메인 간 태스크 순서, 공유 파일 주인, 커밋 단위, 동시 실행 제약
- `docs/plan/WORKSPACE_PLAN.md` — 워크스페이스(Workspace) 구현 계획
- `docs/plan/DOCUMENT_PLAN.md` — 문서(Document) 구현 계획
- `docs/plan/DICTIONARY_PLAN.md` — 사전집(Dictionary) 구현 계획
- `docs/plan/DRAFT_DOCUMENT_PLAN.md` — 문서 초안(DraftDocument) 구현 계획
- `docs/plan/DRAFT_DICTIONARY_PLAN.md` — 사전 초안(DraftDictionary) 구현 계획
- `docs/plan/REVIEW_REQUEST_PLAN.md` — 리뷰 요청(ReviewRequest) 구현 계획
- `docs/plan/NOTIFICATION_PLAN.md` — 알림(Notification) 구현 계획. **2026-09-13 신설**이라 낡은 서술이 없다
- `docs/plan/REVISION_LOG_PLAN.md` — 개정 이력(RevisionLog) 구현 계획. **2026-09-13 신설.** 사전집·문서가 확정될 때마다 무엇이 달라졌는지를 발행 시점에 얼려 둔다

> 앞의 3개는 **이미 구현된 도메인**이라 as-built 스냅샷과 변경 델타를 함께 담는다. 그다음 3개는 미구현이며 **2026-09-10 큰 흐름 확정 이전에 작성되어 낡은 서술이 남아 있다** — `CONFLICTS.md`의 `R-8`~`R-14`·`R-19`가 그 목록이다. 마지막 Notification은 **6개 도메인이 계약으로만 두던 이벤트의 첫 소비자**다(`D-47`).

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
- `application.yml`(또는 `.properties`), 인프라 설정, 시크릿, `.env` 파일을 임의로 변경하지 않는다.
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

2026-09-13 기준으로 아직 채워지지 않은 부분이다. 관련 작업을 할 때 함께 정리한다.

- ~~**Flyway 마이그레이션 없음.**~~ **해소** — `V1__create_workspace_and_participant.sql`을 추가했다. Repository 테스트는 `RepositoryTestSupport`를 통해 Flyway가 만든 스키마를 쓴다. `BaseEntityAuditingTest`만 테스트 전용 엔티티를 쓰므로 `flyway.enabled=false` + `ddl-auto=create-drop`을 유지한다.
- ~~**`SecurityConfig` 없음.**~~ **해소** — `member/infra/security/SecurityConfig`와 `JwtAuthenticationFilter`·`JwtAuthenticationEntryPoint`·`JwtAccessDeniedHandler`·`SecurityConfigTest`가 들어왔다(Google OAuth2 로그인 연동, `WLSH-75`·`WLSH-122`).
- ~~**인증 정합이 아직 남아 있다.**~~ **해소** — `T-INT-3`이 둘을 함께 정리했다. ① **프로파일을 `local`·`dev`·`prod`·`test` 넷으로 분리**했고 `src/test/resources/application.yml`의 수동 복제를 걷어냈다(`NFR-INF-002`·`D-46`). ② **컨트롤러 18개가 받던 `memberId` 요청 파라미터 74곳을 제거**하고 `@AuthenticationPrincipal`로 인증 주체에서 해석한다(`NFR-USR-001`). 컨트롤러 테스트의 `@AutoConfigureMockMvc(addFilters = false)`는 유지하되 `@WithLoginMember`로 principal을 주입한다 — 인증·인가 흐름 자체는 `SecurityConfigTest`가 단독으로 검증한다.
- **AI 워커(FastAPI) 구현이 이 저장소에 없다.** 백엔드의 책임은 계약·발행·콜백 수신·타임아웃 회수까지이며(`docs/AI_CONTRACT.md`), **워커가 뜨기 전에는 `app.ai.dispatch.mode=sqs`인 환경에서 추출·대조 작업이 제한 시간 뒤 실패로 회수된다.** 로컬에서 흐름을 보려면 기본값(`in-process`)을 쓰면 대역이 즉시 끝낸다 — **추출 대역은 고정 후보어 2건을 돌려주고**(`InProcessExtractionWorker`) 대조 대역은 빈 결과로 끝낸다.
- **콜백 보호가 1회용 토큰과 네트워크 격리에 의존한다.** `/api/internal/**`은 인증 필터를 통과하며, 방어는 작업마다 발행되는 `requestId` 대조뿐이다(`D-70`). **배포 전에 보안 그룹·인그레스로 이 경로를 워커 출발지로 제한해야 하고, 그것이 없으면 `NFR-AI-002`가 미충족이다.**
- **`ErrorResponse`에 `traceId`가 없다.** `NFR-CMN-003`과 `docs/LOG.md`의 「에러 응답에는 trace id를 포함한다」가 아직 미충족이다(`docs/plan/CONFLICTS.md` `X-08`). 마무리 통합에서 범위 밖으로 뒀으므로 담당 태스크를 다시 정해야 한다.
- ~~**메시징 배포 대상 미확정.**~~ **해소** — Kafka는 제거했고 **로컬·테스트는 Spring `ApplicationEvent` 인메모리 어댑터, AWS 배포는 SQS**로 확정했다(`docs/plan/CONFLICTS.md` `D-24`). 어댑터 선택은 `app.messaging.mode` 프로퍼티로 하고 `EventPublisher` 포트는 그대로 쓴다. 추출·대조 같은 오래 걸리는 작업은 **DB 작업 테이블**(대기·실행중·성공·실패)로 관리하고 상태 조회는 폴링이다. ~~SQS 어댑터와 로컬 대체 컨테이너는 배포 준비 시점에 추가한다.~~ **추가했다(2026-09-14, `T-INT-6`)** — `compose.yaml`과 `TestcontainersConfiguration`에 LocalStack이 들어왔다. **큐는 둘이다** — 도메인 이벤트 큐와 **AI 워커 요청 큐**(`D-67`). 추출·대조는 이제 인메모리 스텁이 아니라 **외부 FastAPI 워커**가 실행하고 완료를 동기 HTTP 콜백으로 알린다(`D-66`). 선택 축도 갈렸다 — LLM 경로는 `app.ai.dispatch.mode`이고 `app.messaging.mode`와 독립이다. 이벤트 발행 규약은 `docs/ARCHITECTURE.md`를 따른다.
