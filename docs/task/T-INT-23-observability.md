# T-INT-23 — OpenTelemetry 관측성 도입 (Grafana Cloud)

상태: **진행중(2026-09-15)** | 담당자: (`WLSH-173`)
근거: 결정은 `docs/plan/CONFLICTS.md` 3-14절(`D-99`~`D-105`)
의존: 없음. **`X-08`(`ErrorResponse.traceId`)의 담당 태스크이며 `NFR-CMN-003`의 선행**

## 문제

배포된 백엔드는 **관측이 불가능하다.** 관측 인프라가 반쯤 들어와 있다가 끊겨 있다.

| 있는 것 | 실제 동작 |
| --- | --- |
| `backend/build.gradle` `spring-boot-starter-opentelemetry` | **주석 처리됨** → 런타임 OTLP 전송 0 |
| `backend/compose.yaml`·루트 `compose.yaml`의 `grafana/otel-lgtm` | **컨테이너는 뜨는데 앱 설정이 없다** |
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
| 수집 경로 | 앱 → Grafana Cloud OTLP **직행**. 수집기 사이드카 없음 | `D-99` |
| 신호 범위 | 트레이스 + 메트릭 + 로그 전부. 파일 appender 없음, `CONSOLE` 유지 | `D-100` |
| 전송 범위 | Grafana Cloud는 **`prod`만**. 로컬·테스트는 기존 LGTM 유지 | `D-101` |
| 시크릿 | Parameter Store `/lovebug/otel/{auth,endpoint,enabled}` + EPP가 base64 파생 | `D-102` |
| traceId | Micrometer Tracing의 MDC 값을 쓰고 별도 필터를 만들지 않는다 | `D-103` |
| actuator | `health`·`info`만 노출, `env`·`configprops`는 `exclude`로 영구 차단 | `D-104` |
| 가용성 | 관측은 기동 조건이 아니다. 익스포터 로거는 `ERROR` | `D-105` |

## 착수 전 사람이 해야 할 것

- [x] Parameter Store — 인프라가 `/lovebug/otel/auth`(`instanceID:token`)와 `/lovebug/otel/endpoint`를
      등록해 두었다(2026-09-15 확인)
- [x] EC2 인스턴스 역할 — `/lovebug/*`가 기존 권한에 들어 있어 추가가 필요 없다.
      **이것이 파라미터를 옮기지 않고 경로에 맞춘 이유다**(`D-102`)
- [x] **`/lovebug/otel/endpoint`의 값 형태 확인** — **경로가 없고 끝 슬래시도 없다**(2026-09-15 확인).
      설계 전제와 같다. 신호별 경로는 애플리케이션이 붙이며, 배포 전 설정 확인 항목으로 관리한다
- [ ] `/lovebug/otel/enabled` — **선택 사항.** 없으면 켠 것으로 본다. 설정 변경만 단독으로
      검증하려면 먼저 `false`로 만들어 둔다

## 체크리스트

### 1. 문서 선행 갱신

- [x] `docs/plan/CONFLICTS.md` 3-14절에 `D-99`~`D-105` 등재, `X-08`을 `D-103`으로 해소하고
      담당을 `T-INT-3` → `T-INT-23`으로 재지정
- [x] `docs/LOG.md` — 「Trace ID와 MDC」를 `D-103` 기준으로, 「로그 저장과 롤링 전략」을
      컨테이너 전제로(`D-100`), 「요청/응답 로그」에 트레이스와의 중복 주의 추가
- [x] `docs/API.md`·`docs/EXCEPTION.md` 에러 응답 형식에 `traceId` 반영
- [x] `docs/plan/EXECUTION_ORDER.md` 0절의 「새 결정은 `D-79`부터」 정정

### 2. 백엔드 OTel 배선

- [x] `build.gradle` — OTel 스타터 주석 해제 + `opentelemetry-logback-appender-1.0`.
      **`opentelemetry-instrumentation-bom-alpha`를 import하지 않는다** — 그 BOM을 import하면
      `io.opentelemetry:*` 전체가 Boot가 관리하는 버전에서 내려간다. 버전을 직접 고정한다
- [x] `application.yml` — actuator 노출·리소스 속성·샘플링·OTel 익스포터 로거 `ERROR`.
      **수집기 주소를 적지 않는다**(`D-101`)
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

### 5. actuator 노출 범위 (`D-104`)

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

관측성 전용 테스트 코드는 제거했다(2026-09-15). 따라서 아래 항목은 배포 전 수동 확인과 일반
백엔드 회귀 테스트로 검증한다.

- [x] **설정 키·엔드포인트·Basic 헤더·킬 스위치** — `application-prod.yml`과 Parameter Store
      계약을 대조한다
- [x] **Parameter Store 연동 순서** — 운영 프로파일 기동 로그와 실제 설정 주입 결과를 확인한다
- [x] **전송·로그 격리** — Grafana Cloud 또는 로컬 LGTM 수신기에서 신호 경로와 exporter 오류의
      stdout 노출을 확인한다

> ⚠️ **수집기를 없애도 메트릭만은 조용히 계속 두드린다.** 트레이스·로그는 엔드포인트가 없으면
> `*ConnectionDetails` 빈이 없어 익스포터도 안 생기지만, 메트릭은 `OtlpMeterRegistry`가 그대로
> 만들어져 Micrometer 기본 주소(`http://localhost:4318/v1/metrics`)로 60초마다 붙으려 한다
> 설정의 신호별 네임스페이스가 비대칭이므로 주의한다. 실패 로그는 되먹임 차단 때문에
> 눌려 있어 보이지 않는다. **해롭지는 않지만 `backend/compose.yaml`의 LGTM을 영구히 빼지 않는
> 이유다.** 수집기 없이 돌려야 한다면 `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=false`를 준다.

> **일반 테스트에서는 텔레메트리를 LGTM으로 보내지 않는다.** 관측성 전용 테스트 의존성과 테스트
> 코드는 제거했으며, **로컬 LGTM으로 실제 신호를 보내는 것은 `bootRun`뿐이다** — 아래 수동 확인이
> 그래서 남는다.

- [x] `./gradlew spotlessApply && ./gradlew check`
- [x] 로컬 — **추적 컨텍스트가 실제로 돈다**(2026-09-15, `bootRun`). 잘못된 토큰으로 로그인을
      시도하니 로그에 `[{traceId}-{spanId}]`가 붙었다. **수집기가 없어도 이 값이 있다**는 것이
      함께 확인된 셈이라 `D-103`의 「익스포터를 꺼도 `Tracer`는 만들어진다」가 실물로 증명됐다
- [x] 로컬 — **수집기를 아예 두지 않아도 서비스가 계속된다**(2026-09-15). `backend/compose.yaml`에서
      `grafana-lgtm`을 빼고도 같은 형태로 응답·로그가 나왔다
- [x] 로컬 — **에러 응답 본문의 `traceId`가 로그의 값과 일치한다**(2026-09-15). 잘못된 토큰 요청에
      서버 로그의 `[{traceId}-{spanId}]`와 응답 JSON의 `traceId`가 같은 값이었다.
      **`X-08`·`NFR-CMN-003`이 실물로 확인됐다**
- [x] 로컬 — **Tempo·Loki·Prometheus 3신호 확인**(2026-09-15)
- [x] 로컬 — **수집기가 있는데 죽었을 때도 서비스가 계속된다**(2026-09-15, `D-105`). LGTM 컨테이너를
      내린 상태에서 `/actuator/health`가 정상이고 로그의 `[{traceId}-{spanId}]`도 그대로였다
- [x] 로컬 — **되먹임 차단이 듣지 않는 것을 발견하고 고쳤다**(2026-09-15). 자세한 것은 아래 「드러난 결함」
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

## 드러난 결함 — 되먹임 차단이 거꾸로 걸려 있었다 (2026-09-15, 해소)

수집기를 **띄웠다가 내리는** 확인에서 드러났다. `logging.level.io.opentelemetry.exporter.internal=ERROR`
로 되먹임을 끊으려 했는데, `HttpExporter`가 찍는 레벨이 둘이다.

| 상황 | 레벨 | `ERROR` 설정의 결과 |
| --- | --- | --- |
| 연결 실패(수집기가 죽음) | `SEVERE`(= ERROR) | **못 막는다** — 스택 트레이스가 그대로 나온다 |
| HTTP 상태코드 실패(Grafana Cloud **401** 등) | `WARNING` | **가려버린다** — 토큰이 틀려도 알 수 없다 |

즉 **운영에서 가장 중요한 메시지를 숨기고 시끄러운 쪽만 보여주는** 조합이었다. 배포해서 401을
맞았다면 아무 단서 없이 「텔레메트리가 안 온다」만 남았을 것이다.

**고친 방법은 레벨이 아니라 전파 차단이다.** `logback-spring.xml`에서 익스포터 로거 셋을
`additivity="false"` + `CONSOLE` 전용으로 두었다. 되먹임의 고리는 「실패 로그가 OTEL appender 로
가는 것」이므로 거기서 끊고, 메시지는 stdout 에 남겨 진단 수단을 지킨다. 폭주는 OTel 의
`ThrottlingLogger`가 같은 메시지를 분당 1회로 묶어 막는다. `additivity="false"`와 `CONSOLE`
전용 설정이 이 배선을 보장한다.

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
