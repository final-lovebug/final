# T-INT-23 — OpenTelemetry 관측성 도입 (Grafana Cloud)

상태: **진행중(2026-09-15)** | 담당자: (`WLSH-173`)
근거: 결정은 `docs/plan/CONFLICTS.md` 3-10절(`D-92`~`D-98`)
의존: 없음. **`X-08`(`ErrorResponse.traceId`)의 담당 태스크이며 `NFR-CMN-003`의 선행**

## 문제

배포된 백엔드는 **관측이 불가능하다.** 관측 인프라가 반쯤 들어와 있다가 끊겨 있다.

| 있는 것 | 실제 동작 |
| --- | --- |
| `backend/build.gradle` `spring-boot-starter-opentelemetry` | **주석 처리됨** → 런타임 OTLP 전송 0 |
| `build.gradle`의 OTel 테스트 스타터·`testcontainers-grafana` | 테스트 클래스패스에만 있음 |
| `backend/compose.yaml`·루트 `compose.yaml`의 `grafana/otel-lgtm`, `TestcontainersConfiguration`의 `LgtmStackContainer` | **컨테이너는 뜨는데 앱 설정이 없다** |
| `micrometer-registry-prometheus`(`runtimeOnly`) | `management.endpoints.web.exposure.include`가 없어 `/actuator/prometheus` 미노출 |
| `backend/CLAUDE.md` 「관측 — Actuator, Micrometer, OpenTelemetry / Grafana LGTM」 | 문서와 코드 불일치 |

여기에 더해:

- **로그가 재배포 한 번에 사라진다.** `deploy/scripts/start_container.sh`가 `json-file` 드라이버로 인스턴스 로컬에만 남기는데 CodeDeploy가 `docker rm -f spring`으로 컨테이너를 갈아끼운다. 장애 원인 추적이 배포와 함께 증발한다.
- **`MDC` 사용처가 코드 전체에 0건**이라 `docs/LOG.md` 「Trace ID와 MDC」가 문서에만 있다. `ErrorResponse`에 `traceId`가 없어 `NFR-CMN-003`이 대기다(`X-08`).
- **`/actuator/**`가 전 경로 무인증 공개다**(`SecurityConfig`).
- **`NFR-CMN-001`(일반 조회 p95 1초)은 측정 수단 자체가 없다.**

## 설계 확정(2026-09-15, 사용자 결정)

| 축 | 결정 | ID |
| --- | --- | --- |
| 수집 경로 | 앱 → Grafana Cloud OTLP **직행**. 수집기 사이드카 없음 | `D-92` |
| 신호 범위 | 트레이스 + 메트릭 + 로그 전부. 파일 appender 없음, `CONSOLE` 유지 | `D-93` |
| 전송 범위 | Grafana Cloud는 **`prod`만**. 로컬·테스트는 기존 LGTM 유지 | `D-94` |
| 시크릿 | Parameter Store `/lovebug/otel/{auth,endpoint,enabled}` + EPP가 base64 파생 | `D-95` |
| traceId | Micrometer Tracing의 MDC 값을 쓰고 별도 필터를 만들지 않는다 | `D-96` |
| actuator | `health`·`info`만 노출, `env`·`configprops`는 `exclude`로 영구 차단 | `D-97` |
| 가용성 | 관측은 기동 조건이 아니다. 익스포터 로거는 `ERROR` | `D-98` |

## 착수 전 사람이 해야 할 것

- [x] Parameter Store — 인프라가 `/lovebug/otel/auth`(`instanceID:token`)와 `/lovebug/otel/endpoint`를
      등록해 두었다(2026-09-15 확인)
- [x] EC2 인스턴스 역할 — `/lovebug/*`가 기존 권한에 들어 있어 추가가 필요 없다.
      **이것이 파라미터를 옮기지 않고 경로에 맞춘 이유다**(`D-95`)
- [x] **`/lovebug/otel/endpoint`의 값 형태 확인** — **경로가 없고 끝 슬래시도 없다**(2026-09-15 확인).
      설계 전제와 같다. 신호별 경로는 애플리케이션이 붙이며, 이 전제는 이제
      `ObservabilityPropertiesTest`가 고정한다
- [ ] `/lovebug/otel/enabled` — **선택 사항.** 없으면 켠 것으로 본다. 설정 변경만 단독으로
      검증하려면 먼저 `false`로 만들어 둔다

## 체크리스트

### 1. 문서 선행 갱신

- [x] `docs/plan/CONFLICTS.md` 3-10절에 `D-92`~`D-98` 등재, `X-08`을 `D-96`으로 해소하고
      담당을 `T-INT-3` → `T-INT-23`으로 재지정
- [x] `docs/LOG.md` — 「Trace ID와 MDC」를 `D-96` 기준으로, 「로그 저장과 롤링 전략」을
      컨테이너 전제로(`D-93`), 「요청/응답 로그」에 트레이스와의 중복 주의 추가
- [x] `docs/API.md`·`docs/EXCEPTION.md` 에러 응답 형식에 `traceId` 반영
- [x] `docs/plan/EXECUTION_ORDER.md` 0절의 「새 결정은 `D-79`부터」 정정

### 2. 백엔드 OTel 배선

- [x] `build.gradle` — OTel 스타터 주석 해제 + `opentelemetry-logback-appender-1.0`.
      **`opentelemetry-instrumentation-bom-alpha`를 import하지 않는다** — 그 BOM을 import하면
      `io.opentelemetry:*` 전체가 Boot가 관리하는 버전에서 내려간다. 버전을 직접 고정한다
- [x] `application.yml` — actuator 노출·리소스 속성·샘플링·OTel 익스포터 로거 `ERROR`.
      **수집기 주소를 적지 않는다**(`D-94`)
- [x] `logback-spring.xml` 신설 — `CONSOLE` + `OTEL`. `OpenTelemetryAppenderInitializer`가
      `InitializingBean`으로 install한다(Logback이 Spring 컨텍스트보다 먼저 뜨므로 appender가
      스스로 `OpenTelemetry`를 얻지 못한다)
- [x] `local`·`dev`에 `exposure.include`로 `prometheus` 추가

### 3. traceId (`X-08`)

- [x] `ErrorResponse`에 `traceId` 필드(`@JsonInclude(NON_NULL)`) + 팩토리에서 MDC 조회
- [x] `GlobalExceptionHandler`는 손대지 않는다 — 두 팩토리가 유일한 통로라 핸들러 전체에
      자동 적용된다. **착수 시 이 전제가 여전한지 코드로 확인할 것**

### 4. `@Async` 컨텍스트 전파

- [x] `spring.task.execution.propagate-context`는 **Boot 4.0.8에 없다**(4.1 추가분).
      `ContextPropagatingTaskDecorator`를 빈으로 등록한다
- [x] **`AsyncEventConfig`에 두지 않는다** — `AsyncConfigurer` 구현체에 이 `@Bean`을 함께 두면
      `applicationTaskExecutor` 생성 중 `Illegal factory instance`로 컨텍스트가 기동에 실패한다

### 5. actuator 노출 범위 (`D-97`)

- [x] `SecurityConfig` — `/actuator/**` `permitAll`을 `/actuator/health`·`/actuator/health/**`·
      `/actuator/info`로 좁힌다. **health 경로는 반드시 열어 둔다** — `deploy/scripts/validate.sh`의
      readiness 폴링이 여기를 친다
- [x] `app.security.actuator.permit-all` 신설 — `prod`는 기본 `false`, `local`·`dev`만 연다.
      **노출(`exposure.include`)만으로는 `/actuator/prometheus`가 401이라** 「눈으로 확인하는
      수단」이 실제로는 동작하지 않는다
- [x] 나머지 경로를 인증으로 막을 때 **`isAuthenticated()`를 직접 쓰지 않는다** — 익명 인증
      토큰도 `true`라 무인증이 통과한다

### 6. prod — Grafana Cloud 직행

- [x] `OtlpAuthHeaderEnvironmentPostProcessor` 신설 + `META-INF/spring.factories` 등록.
      `otel.auth`(원문 `instanceID:token`)를 base64해 `otel.auth-header`로 파생한다.
      **`getOrder()`가 `ConfigDataEnvironmentPostProcessor.ORDER`보다 커야** Parameter Store가
      올라온 뒤에 돈다. **`trim()` 필수** — 값에 개행이 섞이면 base64가 깨져 401이 나는데
      익스포터 로거가 `ERROR`라 조용히 실패한다. **토큰을 로그에 찍지 않는다**
- [x] `application-prod.yml` — 엔드포인트·헤더·`enabled`·샘플링·리소스 속성.
      `health.show-details`도 `always` → `when-authorized`
- [x] `start_container.sh` — `-e OTEL_SERVICE_VERSION="$TAG"` 한 줄

### 7. 검증

배포 전에 로컬에서 확인할 수 있는 것은 모두 테스트로 옮겼다(2026-09-15). 사슬을 넷으로 나눠 본다.

- [x] **키가 맞는가** — `OtlpPropertyBindingTest`. 세 신호의 네임스페이스가 비대칭이고 4.x 키의
      오타는 조용히 무시되므로 사람 눈으로는 못 잡는다
- [x] **설정이 그 키에 올바른 값을 넣는가** — `ObservabilityPropertiesTest`. 엔드포인트 합성,
      Basic 헤더, 킬 스위치, 자격증명이 없을 때 기동이 막히지 않는 것. **Boot 3.x 낡은 키가
      되살아나지 않는지도 함께 막는다** — 그 키들은 deprecation level이 `error`인데 `prod`
      프로파일은 CI에서 뜨지 않아 배포 때까지 드러나지 않는다
- [x] **값이 와이어까지 나가는가** — `OtlpExportOverHttpTest`. JDK 내장 HTTP 서버를 가짜 수신기로
      세워 경로·`Authorization`·`Content-Encoding: gzip`을 확인한다. 헤더가 빠지면 Grafana Cloud가
      401로 돌려주는데 익스포터 로거가 `ERROR`라 조용히 실패한다
- [x] **Parameter Store에서 거기까지 이어지는가** — `OtlpParameterStoreChainTest`. LocalStack SSM에
      실제로 파라미터를 넣어 경로→프로퍼티 매핑과 **EPP가 Parameter Store 뒤에 도는 것**을 본다.
      순서가 뒤집히면 예외 없이 아무것도 하지 않는다

> **테스트에서는 텔레메트리가 LGTM으로 나가지 않는다.** `spring-boot-starter-opentelemetry-test`가
> `@SpringBootTest`마다 `management.tracing.export.enabled=false`를 꽂는다(메트릭도 같다). 켜려면
> `@AutoConfigureObservability`나 `spring.test.tracing.export=true`가 필요하다. **로컬 LGTM으로
> 실제 신호를 보내는 것은 `bootRun`뿐이다** — 아래 수동 확인이 그래서 남는다.

- [x] `./gradlew spotlessApply && ./gradlew check`
- [ ] 로컬 — `bootRun`으로 Tempo·Loki·Prometheus 3신호 확인. **에러 응답의 `traceId`로 트레이스가
      조회되고 Loki 로그의 `trace_id`가 같은 값인지**까지 본다. 배치 플러시에 수 초 걸리므로
      즉시 조회하면 안 나온다
- [ ] 로컬 — **LGTM을 내린 채로도 서비스가 계속되는지**(`D-98`). export 실패 로그가 쌓이지 않는지
- [x] `grep -r "grafana.net" backend/src`가 0건 — 로컬·테스트 경로에 Cloud 주소가 없다
- [ ] 배포 — `/lovebug/otel/enabled=false`로 먼저 배포해 **설정 변경만 단독 검증**
- [ ] 배포 — `true`로 바꾸고 재배포. Grafana Cloud에서 3신호, `docker logs spring`에 EPP의
      `instanceID` 한 줄
- [ ] 배포 — `free -m`·`docker stats --no-stream`. **t4g.small(2 GiB)에 `--memory 1600m`이라
      형식적 확인이 아니다**
- [ ] 24시간 후 Grafana Cloud usage. 초과 조짐이면 샘플링 하향 → 메트릭 `step` 상향 순

### 8. 사후 문서

- [x] `backend/CLAUDE.md` — 「관측」 절을 실제 구성으로, 환경변수 표에 `OTEL_*`,
      Parameter Store 절에 `/lovebug/otel/`
- [x] 루트 `CLAUDE.md` 「미구성 항목」의 `ErrorResponse.traceId` 항목을 해소로
- [x] `docs/REQUIREMENTS.md` — `NFR-CMN-003` 완료.
      **`NFR-INF-008`은 올리지 않는다**(워커 구간 전파가 범위 밖).
      **`NFR-CMN-001`도 올리지 않는다** — 측정 수단이 생겼을 뿐이다

## 진행 중 드러난 선행 결함 (이 태스크가 만든 것이 아니다)

`./gradlew check` 984건 중 **1건이 실패한다.** `develop` 시점부터 이미 실패하고 있었다 —
작업분을 `git stash` 로 걷어내고 돌려 확인했다. 조건을 완화하지 않고 그대로 둔다
(루트 `CLAUDE.md` 「테스트가 실패하면 테스트를 삭제하거나 조건을 완화하지 말고 원인을 보고한다」).

- **`UbiquitousLanguageLifecycleTest.firstDictionaryIsBornFromDocuments`**
- 기대: 사전집에 손으로 등록한 `결제수단` 하나 / 실제: `결제`·`결제수단`·`주문`
- 원인: `InProcessExtractionWorker.mockTerms` 가 고정 후보어 2건(`결제`·`주문`)을 돌려주는데
  테스트는 아직 「대역이 추출 결과를 빈 목록으로 돌려준다」를 전제한다. **그 테스트의 주석이
  이 드리프트를 스스로 예고해 뒀다** — 「대역이 후보어를 돌려주게 되면 그것들도 함께 실리며 이
  기대값이 늘어난다」
- `D-88`(후보어를 판정 없이 전부 발행) 기준으로는 **현재 동작이 맞고 기대값이 낡았다.**
  고친다면 기대값을 세 건으로 늘리는 쪽이며, 이 태스크의 범위가 아니라 담당을 따로 정한다

## 이번 범위 밖

- **워커 구간 `traceparent` 전파.** `LlmJobRequest`에 `traceparent`를 싣고 `docs/AI_CONTRACT.md`를
  호환 변경으로 고치면 Spring → SQS → FastAPI 3단이 이어진다. `NFR-INF-008`은 그때까지 미충족이다.
- **`NFR-AI-002`(서비스 간 인증).** 보안 그룹·인그레스로 `/api/internal/**`을 워커 출발지로
  제한하는 것이 충족 조건이며 이번 태스크는 보안 그룹을 건드리지 않는다.
