# 백엔드 성능 개선 계획

> 2026-09-15. 운영 환경에서 k6로 측정하며 성능을 개선한다. 인스턴스 증설, DB 이중화, RDS 전환은 범위 밖이다.

## 1. 전제와 목표

현재 구성은 Spring Boot 2대(`t4g.small`), 별도 FastAPI 워커 2대(`t4.micro`), MySQL 1대다. ALB가 Spring 인스턴스로 요청을 분산하고, AI 요청은 SQS를 거쳐 워커가 처리한다.

일반 API의 초기 SLO는 p95 500ms 이하, p99 1초 이하, 5xx 비율 1% 미만으로 둔다. AI는 요청 접수 API와 실제 완료를 분리해 큐 대기시간, 워커 처리시간, 콜백 저장시간, 전체 완료시간을 측정한다.

운영 부하 테스트는 실제 사용자 영향이 가능한 작업이다. k6는 서비스 EC2와 분리된 호스트에서 실행하고, 테스트 전용 워크스페이스·문서·테스트 사용자만 사용한다. OAuth 로그인은 부하 시나리오에서 제외하고, 사전에 발급한 JWT를 실행 환경변수로 주입한다. 토큰·URL·식별자는 저장소에 넣지 않는다.

## 2. 관측 기준선

설정이나 쿼리를 바꾸기 전 7일 동안 평시·피크·AI 작업 집중 시간의 다음 지표를 수집한다.

| 계층 | 지표 |
| --- | --- |
| ALB | 요청 수, target response time, 4xx/5xx, healthy target 수 |
| Spring | endpoint별 p50/p95/p99, 오류율, JVM heap/RSS, GC pause, CPU와 CPU credit |
| 요청 실행 | Tomcat 활성 스레드, async executor active/queue/rejection, scheduler 지연 |
| DB | Hikari active/idle/pending/acquire timeout, MySQL CPU, 연결 수, lock wait, slow query, 실행계획 |
| Redis | memory, eviction, command latency, cache hit/miss |
| AI | SQS 메시지 수와 최대 대기시간, FastAPI 처리시간, callback 성공/실패/중복, 작업 상태별 체류시간 |

모든 로그와 지표는 가능한 한 `traceId`, `requestId`, `jobId`, `workspaceId`로 연결한다. 토큰, 이메일, 문서 본문, LLM 요청/응답 전문은 남기지 않는다.

## 3. k6 실행 시나리오

### 3-1. 읽기 중심

워크스페이스·참여자, 문서 목록/상세/버전, 활성 사전집/용어, 알림 목록/미읽음 수, 리뷰 요청 목록을 호출한다. 목록 요청은 기본 `size=20`을 사용하고, 요청 비율은 목록 80%, 상세 20%로 둔다. 각 반복 사이에는 1~3초의 think time을 둔다.

| 단계 | VU | 유지 시간 | 다음 단계 조건 |
| --- | ---: | ---: | --- |
| Smoke | 1 | 2분 | 인증, 테스트 데이터, 응답 형식 확인 |
| Warm-up | 3 | 5분 | JVM·DB connection·Redis 캐시 워밍업 |
| Level 1 | 5 | 10분 | SLO와 DB pool 상태 정상 |
| Level 2 | 10 | 15분 | Level 1 대비 p95 악화 10% 미만 |
| Level 3 | 20 | 15분 | 중단 기준에 해당하는 지표 없음 |

### 3-2. 쓰기 중심

테스트 전용 워크스페이스에서 문서 생성·제목/라벨 변경·알림 읽음 처리·저빈도 리뷰 흐름을 검증한다. 1~3 VU로 시작한다. 같은 문서를 여러 VU가 수정하지 않으며, 낙관적 락 충돌 성능은 별도 시나리오로 분리한다.

### 3-3. AI 작업

문서 대조와 용어 추출을 분리한다. 작업은 1 VU로 한 번에 하나만 요청하고, 기존 규격대로 2초 간격·최대 5분으로 상태를 폴링한다. 다음 요청은 선행 작업이 종료된 뒤에만 보낸다. 대량 LLM 요청은 비용과 큐 적체를 인위적으로 만들므로 운영 성능 시험에서 하지 않는다.

## 4. 중단 기준

k6 threshold와 Grafana/CloudWatch 알람을 함께 사용한다. k6의 최소 threshold는 `http_req_failed < 1%`, 일반 API `p(95) < 750ms`로 두고, 최종 SLO 판정은 500ms로 한다.

| 조건 | 대응 |
| --- | --- |
| 5xx가 2분 연속 1% 이상 | 즉시 k6를 중단하고 오류 API와 의존성 오류를 조사한다. |
| p99가 3분 연속 2초 초과 | 다음 VU 단계로 진행하지 않는다. |
| MySQL CPU 85% 이상 또는 lock wait 급증 | 즉시 중단한다. Hikari pool 증설보다 SQL·인덱스 분석이 우선이다. |
| Hikari pending connection이 1분 지속 | 즉시 중단한다. DB pool/쿼리 문제로 분류한다. |
| JVM heap 85% 이상 또는 Full GC | 중단하고 캐시·대형 응답·엔티티 적재량을 확인한다. |
| CPU credit 지속 하락 | 다음 단계로 올리지 않고 Spring/FastAPI/MySQL 중 CPU 사용 주체를 분리한다. |
| AI queue age 5분 초과 | AI 시나리오를 중단하고 워커, DB 읽기, 모델 호출 시간을 분석한다. |
| 실제 사용자 p95가 시험 전보다 20% 이상 악화 | 즉시 중단하고 운영 안정화를 확인한다. |

## 5. 결과별 조절

### Spring과 JVM

초기 운영값은 JVM `Xms=512m`, `Xmx=1024m`, async executor `core=2`, `max=4`, `queue=100`, scheduler 1~2 스레드다. async executor는 bounded queue와 `CallerRunsPolicy`를 사용해 이벤트 작업 유실을 막는다.

| 결과 | 조절 |
| --- | --- |
| CPU가 높고 heap/GC/DB가 정상 | 직렬화, 보안 필터, 애플리케이션 CPU 사용을 프로파일링한다. 스레드를 늘리지 않는다. |
| heap 75% 초과 또는 GC pause 증가 | `open-in-view=false`, DTO projection, batch 조회, page size 상한, 캐시 payload 축소를 우선 적용한다. 원인 제거 전 heap만 늘리지 않는다. |
| async queue가 5분 이상 80% 초과 | SQS 발행 지연, DB 상태 전이, AWS SDK 오류를 확인한다. 2 vCPU에서 max thread를 4 이상으로 올리지 않는다. |
| async 포화와 API p95 증가 동반 | `CallerRunsPolicy`로 요청 스레드가 작업 중인 상태다. SQS/DB 병목을 해결한 후 queue 용량만 조절한다. |
| ALB 지연만 높고 Spring 지표 정상 | target response time, 헬스체크, 네트워크를 점검한다. JVM/Hikari를 먼저 바꾸지 않는다. |

### MySQL과 Hikari

초기 Hikari 값은 Spring 인스턴스당 maximum 8, minimum-idle 2, connection timeout 2초다. timeout을 늘려 요청을 적체시키지 않는다.

| 결과 | 조절 |
| --- | --- |
| active 7~8, pending 발생, MySQL CPU 높음 | pool을 늘리지 않는다. slow query, lock wait, N+1, 실행계획부터 개선한다. |
| active 7~8, pending 발생, MySQL CPU 60% 미만, slow query 없음 | pool을 8→10으로 한 번만 올리고 같은 k6 단계로 재측정한다. |
| pool 증가 후 p95 개선 없음 | 8로 되돌리고 쿼리 수, count query, DTO 조립을 분석한다. |
| 특정 API만 느림 | 해당 요청의 SQL 수·row 수와 `EXPLAIN ANALYZE`를 수집한다. |
| full scan/filesort | 필터와 정렬을 함께 만족하는 복합 인덱스를 Flyway로 추가한다. |
| 읽기 개선 뒤 쓰기 지연 증가 | 중복·저선택도 인덱스를 제거 후보로 관리한다. |

인덱스는 문서 목록, 알림 목록, 리뷰 요청 검색, 작업 상태 회수 쿼리부터 검토한다. `%검색어%`처럼 B-tree 이득이 없는 검색에는 근거 없이 인덱스를 추가하지 않는다. 온라인 DDL 가능 여부와 적용 전후 실행계획을 확인한 뒤 피크 시간 밖에 배포한다.

### Redis 캐시

활성 사전집·용어 목록·워크스페이스 참여/권한처럼 변경 시 즉시 무효화 가능한 읽기만 cache-aside로 저장한다. 검색 결과, 페이지 목록, AI job 상태는 캐시하지 않는다.

| 결과 | 조절 |
| --- | --- |
| cache hit 80% 미만, DB read 감소 없음 | 키 범위, TTL, 무효화 누락을 점검한다. TTL만 늘리지 않는다. |
| hit 80% 이상, DB p95 감소 | 현재 범위를 유지한다. |
| Redis memory 70% 초과 또는 eviction | TTL/payload를 줄이고 큰 목록 캐시를 제거한다. |
| 변경 직후 오래된 데이터 | 쓰기 트랜잭션 커밋 뒤 workspace 단위 키 무효화를 보강한다. |
| Redis 장애가 API 5xx로 전파 | cache miss로 DB를 조회하는 fallback을 적용한다. |

### AI 작업

| 결과 | 조절 |
| --- | --- |
| queue age 증가, MySQL 정상 | FastAPI/LLM 처리 병목이다. 워커별 동시 처리 1·prefetch 1을 유지하고 모델 호출시간·문서 크기를 최적화한다. |
| queue age 증가와 MySQL CPU/lock wait 동반 | 워커와 Spring이 DB를 경합한다. 워커 읽기 연결 상한을 낮추고 워커 SQL 실행계획을 개선한다. |
| callback 저장시간 증가 | 후보어/제안어 대량 저장, collection 저장, 인덱스 쓰기 비용을 측정한다. batch 처리 여부를 검토하되 requestId 멱등성은 유지한다. |
| timeout 회수 증가 | FastAPI 오류, SQS visibility timeout, callback 접근 경로를 확인한다. 근거 없이 job timeout을 늘리지 않는다. |

## 6. 적용 순서와 검증

1. 대시보드, k6 script, 중단 알람을 준비하고 Smoke/Warm-up 결과를 남긴다.
2. 읽기 중심 기준선을 수집한다.
3. JVM, Hikari, bounded async executor, scheduler 설정을 Spring 한 대에 canary로 적용한다.
4. 기존 인스턴스와 canary를 같은 k6 단계에서 p95/p99, 5xx, GC, Hikari pending으로 비교한다.
5. p95가 10% 이상 나빠지거나 5xx가 1%를 넘으면 설정만 즉시 이전값으로 되돌린다. 안정적이면 두 번째 Spring에 적용한다.
6. 상위 비용 SQL부터 projection, batch 조회, pagination을 적용하고 회귀를 확인한다.
7. 실행계획으로 입증된 인덱스만 별도 Flyway 배포한다.
8. Redis 캐시는 데이터 종류 하나씩 활성화해 hit ratio, DB read 감소, stale data를 검증한다.
9. 마지막으로 AI 시나리오를 실행해 일반 API SLO가 유지되는지 확인한다.

매 실행마다 실행 ID, 시간대, VU 단계, 배포 Git SHA, 적용 설정값, k6 summary, Grafana 링크, API별 p50/p95/p99·오류율, SQL 실행계획 전후 결과를 남긴다.
