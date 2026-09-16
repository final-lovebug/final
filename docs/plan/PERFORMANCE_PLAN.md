# 로컬·개발 환경 성능 테스트 계획

> 2026-09-16. `local`·`dev` 환경에서 k6로 애플리케이션 성능을 확인한다. 운영 배포, 인스턴스 증설, DB 이중화, 실제 LLM 품질·비용 측정은 범위 밖이다.

## 1. 테스트 전제와 목표

`local`은 빠른 API·UI 스모크 테스트에 사용하고, SQS 왕복을 포함한 통합 성능 테스트는 `SPRING_PROFILES_ACTIVE=local,dev`로 실행한다. `dev`는 LocalStack SQS, MySQL, Redis, Grafana LGTM, FastAPI worker를 사용한다. 실제 LLM 대신 worker `MOCK` 모드가 작업마다 1~3초 랜덤 지연 후 고정된 계약 응답을 반환한다.

일반 API의 초기 목표는 p95 500ms 이하, p99 1초 이하, 5xx 비율 1% 미만이다. AI 작업은 접수시간, SQS 대기시간, worker 처리시간, callback 저장시간, 전체 완료시간을 분리해 기록한다. AI 처리시간은 실제 모델 성능이 아니라 모의 외부 처리 지연을 측정한다.

모든 테스트는 테스트 전용 회원·워크스페이스·문서·사전집을 사용한다. OAuth 로그인은 시나리오에서 제외하고 사전에 발급한 JWT를 환경변수로 주입한다. 토큰·URL·식별자·문서 본문은 저장소에 남기지 않는다.

## 2. 환경별 관측 기준

변경 전 기준선을 먼저 수집하고, 실행 ID·Git SHA·프로파일·VU 단계·설정값과 함께 결과를 남긴다.

| 계층 | `local` | `dev` |
| --- | --- | --- |
| 애플리케이션 | Spring API, 인프로세스 AI 대역 | Spring API, LocalStack SQS, FastAPI mock worker |
| 저장소 | MySQL, Redis | MySQL, Redis |
| 관측 | Actuator·콘솔 로그 | Actuator·Grafana LGTM·콘솔 로그 |
| AI 지연 | 인프로세스 대역 시간 | worker의 1~3초 랜덤 모의 지연 + 큐·callback 시간 |

확인할 지표는 endpoint별 p50/p95/p99·오류율, JVM heap·GC·CPU, Tomcat/async executor 상태, Hikari active/idle/pending/timeout, MySQL CPU·연결 수·slow query·lock wait, Redis latency·memory·eviction, SQS queue depth·age, worker 처리시간, callback 성공·실패·중복, 작업 상태별 체류시간이다. 로그는 `traceId`, `requestId`, `jobId`, `workspaceId`로 연결하되 민감정보와 LLM 전문은 기록하지 않는다.

## 3. k6 시나리오

### 3-1. 읽기 중심

워크스페이스·참여자, 문서 목록/상세/버전, 활성 사전집/용어, 알림 목록/미읽음 수, 리뷰 요청 목록을 호출한다. 목록 80%, 상세 20%, 목록 기본 `size=20`, 반복 사이 think time 1~3초를 사용한다.

| 단계 | VU | 유지 시간 | 조건 |
| --- | ---: | ---: | --- |
| Smoke | 1 | 2분 | 인증·테스트 데이터·응답 형식 확인 |
| Warm-up | 3 | 5분 | JVM·DB connection·Redis 캐시 워밍업 |
| Level 1 | 5 | 10분 | SLO와 DB pool 정상 |
| Level 2 | 10 | 15분 | 직전 단계 대비 p95 악화 10% 미만 |
| Level 3 | 20 | 15분 | 중단 기준 없음 |

### 3-2. 쓰기 중심

테스트 전용 워크스페이스에서 문서 생성·제목/라벨 변경·알림 읽음 처리를 1~3 VU로 실행한다. 같은 문서를 동시에 수정하지 않으며, 낙관적 락 충돌은 별도 시나리오로 분리한다.

### 3-3. AI 작업

용어 추출과 문서 대조를 분리한다. `dev`에서 한 번에 하나의 작업만 요청하고, 2초 간격으로 최대 5분까지 상태를 폴링한다. 선행 작업 종료 후 다음 작업을 보낸다. 대량 동시 AI 요청은 이번 모의 지연 기준선의 목적과 맞지 않으므로 제외한다.

## 4. 중단 기준

k6 threshold와 로컬 Grafana/로그 지표를 함께 본다. 최소 threshold는 `http_req_failed < 1%`, 일반 API `p(95) < 750ms`로 두고 최종 목표는 p95 500ms다.

| 조건 | 대응 |
| --- | --- |
| 5xx가 2분 연속 1% 이상 | 테스트 중단 후 오류 API와 의존성 오류 조사 |
| p99가 3분 연속 2초 초과 | 다음 VU 단계로 진행하지 않음 |
| MySQL CPU 85% 이상 또는 lock wait 급증 | 즉시 중단하고 SQL·인덱스 분석 |
| Hikari pending connection이 1분 지속 | 즉시 중단하고 pool/쿼리 문제 분류 |
| JVM heap 85% 이상 또는 Full GC | 중단 후 캐시·응답·조회량 확인 |
| SQS queue age 5분 초과 또는 AI 작업 timeout 증가 | AI 시나리오 중단 후 worker·DB·callback 조사 |

## 5. 결과별 검토 순서

- CPU만 높고 heap·GC·DB가 정상이면 직렬화·보안 필터·애플리케이션 CPU를 프로파일링한다. 스레드를 먼저 늘리지 않는다.
- heap/GC가 나쁘면 `open-in-view=false`, DTO projection, batch 조회, page size 상한, 응답·캐시 payload 축소를 우선 검토한다.
- Hikari pending이 발생하면 pool 증설보다 slow query, lock wait, N+1, 실행계획을 먼저 확인한다. DB CPU가 낮고 slow query가 없을 때만 8→10으로 한 번 조정해 재측정한다.
- full scan/filesort가 실행계획으로 확인된 경우에만 복합 인덱스를 Flyway로 추가하고 전후 계획을 비교한다.
- Redis는 변경 시 무효화 가능한 활성 사전집·용어·워크스페이스 권한 조회만 cache-aside 후보로 삼는다. 페이지 목록과 AI job 상태는 캐시하지 않는다.
- SQS age가 늘고 MySQL이 정상이라면 worker 처리·모의 지연·callback을 분리해 본다. 실제 LLM 호출시간으로 해석하지 않는다.

## 6. 실행 및 결과물

1. Compose와 FastAPI worker를 `local,dev` 조합으로 기동하고 Smoke/Warm-up을 통과시킨다.
2. 읽기·쓰기 기준선을 수집한 뒤 AI 추출·대조를 별도로 실행한다.
3. 각 실행에 k6 summary, 단계별 p50/p95/p99·오류율, 프로파일, Git SHA, 주요 지표, SQL 실행계획을 기록한다.
4. 현재 저장소에는 k6 script·전용 대시보드·자동 중단 알람이 없으므로 별도 태스크로 준비하며, 문서만으로 준비 완료로 판정하지 않는다.
