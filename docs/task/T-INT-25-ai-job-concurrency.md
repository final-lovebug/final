# T-INT-25 — AI 작업 동시성과 LLM 중복 호출 차단

상태: **완료(2026-09-16)** — 배포 전 조치 1건 남음(5절) | 담당자:
근거: 결정은 `docs/plan/CONFLICTS.md` 3-17절(`D-111`~`D-113`). 계약은 `docs/AI_CONTRACT.md` 7-2
의존: 없음

## 문제

「용어 추출·문서 대조를 동시에 누르면 동시성 문제가 생기는가」에서 출발했다. **생긴다.**
그중 **돈이 새는 것**은 LLM 중복 호출이고, 경로가 둘이며 성격이 다르다.

| | 무엇이 일어나는가 | 종전에 막던 것 |
| --- | --- | --- |
| **A. 작업이 둘 생긴다** | 생성 정책 검사가 락 없는 스냅샷 읽기라 동시 요청 둘을 모두 통과시킨다. 작업이 둘이면 `requestId`도 둘이라 워커는 **둘 다 정상 작업으로 보고** 모델을 두 번 부른다 | **없었다.** `D-72`의 콜백 멱등은 *같은* 작업의 중복 콜백만 막는다 |
| **B. 같은 메시지가 두 번 배달된다** | 요청 큐는 표준 큐이고 at-least-once 다(`D-53`). 워커가 같은 `requestId`로 모델을 두 번 부르고 콜백을 두 번 보낸다 | `D-72`가 **초안만** 하나로 접는다. 호출은 이미 끝났고 요금도 나갔다 |

B가 오래 눈에 띄지 않은 이유는 **DB가 깨끗하기 때문이다.** 어디에도 실패로 남지 않는다.

### A의 구조 (경로 전체)

```
request() [tx: 정책검사 → job insert → commit]
    └─ AFTER_COMMIT @Async → markDispatching(PENDING→RUNNING) → SQS send → 워커 → LLM
```

`DraftDictionaryExtractionService:39`(추출)·`DraftDocumentCheckService:38`(대조)의
"진행 중 작업 없음" 검사가 MySQL REPEATABLE READ 의 스냅샷 읽기다. 잠그지 않으므로 동시
트랜잭션 둘이 **모두 통과**하고, `extraction_job`·`check_job`에는 이를 막을 유니크 제약이
없었다(`idx_extraction_job_workspace_status`는 일반 인덱스).

## 설계 확정(2026-09-16, 사용자 결정)

| 축 | 결정 | ID |
| --- | --- | --- |
| A 차단 | 「진행 중 작업 1개」의 **최종 판정자는 DB 유니크 제약**. 사전 검사는 그대로 두고 제약 위반을 기존 409로 바꿔 던진다 | `D-112` |
| B 차단 | **호출 멱등은 워커 책임.** `requestId`가 멱등 키이며 모델 호출 **직전에** 선점한다 | `D-111` |
| 선점 저장소 | **Redis.** 백엔드 claim 엔드포인트 안은 물리쳤다 | `D-113` |

> **claim 엔드포인트를 물리친 이유** — 인증 없는 `/api/internal/**`에 상태를 바꾸는 경로를
> 하나 더 여는 셈이다. 그 경로의 방어가 `requestId` 대조뿐인 상황에서(`D-70`) 표면을 넓힌다.

## 1. A 차단 — 진행 중 작업 유니크 (`D-112`)

`V1__init_schema.sql`에 통합했다(**새 버전을 붙이지 않았다** — 모든 환경의 데이터가 비어 있어
사용자가 통합을 승인했다).

```sql
in_progress_flag  tinyint,   -- 진행 중이면 1, 끝나면 NULL
constraint uk_check_job_document_in_progress       unique (document_id, in_progress_flag)
constraint uk_extraction_job_workspace_in_progress unique (workspace_id, in_progress_flag)
```

MySQL 은 UNIQUE 에서 NULL 을 서로 다른 값으로 보므로 **끝난 작업은 몇 개든 쌓이고 진행 중인
것만 하나로 묶인다.** 그렇지 않으면 한 번 추출한 워크스페이스가 영영 다시 추출할 수 없다.

| 파일 | 변경 |
| --- | --- |
| `V1__init_schema.sql` | 두 테이블에 컬럼·유니크 추가 |
| `ExtractionJob`·`CheckJob` | `inProgressFlag` 필드(`@JdbcTypeCode(TINYINT)`), `@Table(uniqueConstraints=…)`, **`changeStatus()` 신설** |
| `ExtractionJobWriter`·`CheckJobWriter` | `save` → `saveAndFlush` + `DataIntegrityViolationException` → 기존 409 |

**`changeStatus()`가 상태를 바꾸는 유일한 경로다.** 네 전이(생성·`markDispatching`·`succeed`·
`fail`)를 전부 그리로 통일해 플래그가 상태와 어긋날 지점을 없앴다.

**`saveAndFlush`인 이유** — 지연 flush 로 두면 제약 위반이 커밋 시점에 터져
`DataIntegrityViolationException`이 그대로 **500**으로 나간다. 즉시 터뜨려 사전 검사와 같은
409(`DRAFT_DICTIONARY_EXTRACTION_ALREADY_RUNNING`·`DRAFT_DOCUMENT_CHECK_ALREADY_RUNNING`)로 맞춘다.

### 생성 컬럼을 쓰지 못했다 — 미해결 관찰

`dictionary.active_flag`와 같은 **생성 컬럼**으로 먼저 만들었고, 그러자 **H2 위에서 도는 모든
테스트가 삽입 시점에** `The database has been closed`(H2 90098)로 깨졌다. 그 테이블에 아무 행이나
넣는 순간이며, 해당 테이블을 건드리는 테스트가 전부 실패했다.

좁혀 본 결과는 이렇다. **원인은 규명하지 못했다.**

| 시도 | 결과 |
| --- | --- |
| 같은 DDL·같은 H2(2.4.240)·`MODE=MySQL`·같은 INSERT 를 **순수 JDBC** 로 재현 | **정상** |
| 같은 것에 `set referential_integrity false` + `truncate`(DbCleaner 재현)까지 | **정상** |
| Hibernate 경유, 식 `case when status = 'PENDING' then 1 else null end` | **정상** |
| Hibernate 경유, 식에 `in (…)` 또는 조건 2개 이상 | **실패** |

그래서 **엔티티가 유지하는 일반 컬럼**으로 갔다. 잃은 것은 "플래그 정합이 DB 식이 아니라 코드에
달린다"이고, **잃지 않은 것은 동시 INSERT 를 막는 주체가 여전히 DB 유니크 제약이라는 점**이다 —
이 태스크의 목적은 그대로 달성된다.

## 2. B 차단 — 계약 (`D-111`)

`docs/AI_CONTRACT.md` 7-2 를 **결과 멱등(7-2-1, 백엔드)과 호출 멱등(7-2-2, 워커)으로 쪼갰다.**

종전 서술은 「워커는 확신이 없으면 다시 보내도 된다」로 끝나, 워커 입장에서 **모델 재호출까지
안전하다**고 읽혔다. 실제로는 배달마다 요금이 두 배로 나간다. 4-1(요청 큐)에도
"at-least-once 는 모델 호출까지 두 번 일어난다는 뜻"이라는 포인터를 넣었다.

같은 이유로 `SqsLlmJobRequestSender` 의 「중복 수신은 콜백 쪽 멱등이 막는다」 주석도 고쳤다 —
막는 것은 **결과뿐**이다.

## 3. B 차단 — 워커 구현 (`D-113`)

`ubidicExtractor/ubidict-py`. `app/claim.py` 신설 + `queue_consumer._handle_message` 배선.

```
SET llm:request:{requestId} <worker-id> NX EX 900
```

| 상황 | 동작 |
| --- | --- |
| 선점 성공 | 평소대로 처리 |
| 선점 실패(중복 배달) | 모델 호출 없음, **콜백도 없음**, 메시지만 삭제 |
| `REDIS_URL` 없음 / Redis 불통 | 모델 호출 없음, `CLAIM_UNAVAILABLE` 실패 콜백 후 삭제 |
| `mode=STUB`·`MOCK` | 선점하지 않음 |

- **키를 지우지 않는다.** TTL 만료에 맡긴다 — 지우면 그 뒤 재배달분이 다시 선점에 성공한다.
  `requestId`는 작업당 1회용이라(`D-70`) 남아도 다음 작업을 막지 않는다
- **TTL 900초는 `app.ai.timeout.job`(기본 `PT15M`)과 맞춘 것이다.** 짧으면 백엔드 스위퍼가
  작업을 회수하기 전에 키가 풀려 중복 호출이 다시 열린다
- **선점 여부를 모르면 부르지 않는다.** 조용히 건너뛰면 이 장치가 있으나 마나다

### 계약을 한 군데 정정했다

`REDIS_URL` 부재 시 **「워커 기동 실패」로 적었던 것을 「REAL 작업만 실패」로 바꿨다.** 구현하며
보니 기동 실패는 MOCK 전용 로컬 개발(`docker-compose.local.yml` + `scripts/`)을 Redis 없이는
못 돌게 막는다. 선점을 `REAL`로 한정해도 「조용히 건너뛰지 않는다」는 의도는 그대로 지켜진다.
`AI_CONTRACT.md` 7-2-2 와 `CONFLICTS.md` `D-113`을 함께 고쳤다.

## 4. 검증

| 대상 | 결과 |
| --- | --- |
| `backend` `./gradlew check` | **1000 tests, 0 failed.** `SchemaValidationTest`(실 MySQL + Flyway + `validate`) 포함 |
| `ubidict-py` `pytest` / `ruff` | **81 passed**(기존 71 + 신규 10), lint 통과 |

문서 마무리 시점에 핵심 경로도 다시 실행했다.

- `backend`: `ExtractionJob`·`CheckJob`의 Repository/Writer 테스트 4개 클래스를 실행해 통과했다.
- `ubidict-py`: `tests/test_claim.py`·`tests/test_queue_consumer.py` **29 passed**, `ruff check app tests` 통과.

추가한 테스트 28개.

- `ExtractionJobRepositoryTest`·`CheckJobRepositoryTest` 각 4개 — 중복 거절, RUNNING 이 새 요청을
  막음, **끝난 작업은 쌓임**, 워크스페이스/문서가 다르면 공존
- `ExtractionJobWriterTest`·`CheckJobWriterTest` 각 2개 — 제약 위반의 409 변환
- `tests/test_claim.py` 6개 — NX/TTL 900, 중복 시 `False`, 접속 불가 시 예외, 클라이언트 재사용
- `tests/test_queue_consumer.py` 4개 — 선점 호출, 중복 배달 드롭(**콜백 없음**), 저장소 장애,
  MOCK 은 선점 안 함. 기존 REAL 테스트 5개는 autouse fixture 로 「선점 성공」 상태에서 돈다

> `SchemaValidationTest`가 실제로 한 번 잡았다 — `@JdbcTypeCode(TINYINT)`를 빠뜨렸을 때
> `found [tinyint], but expecting [integer]`로 막았다(`D-108`이 의도한 그대로).

## 5. 배포 전 반드시 할 일

**`ubidicExtractor/ubidict-py/deploy/scripts/start_container.sh`에 `REDIS_URL`이 없다. 이대로
배포하면 운영의 모든 `REAL` 작업이 `CLAIM_UNAVAILABLE`로 실패한다.**

인프라 설정이라 이 태스크에서 건드리지 않았다. 정할 것은 **「백엔드가 쓰는 ElastiCache 를
공유할지, 워커용을 따로 둘지」**다. 공유한다면 RDS 블록과 같은 방식으로 `/lovebug/redis/host`·
`/lovebug/redis/port`를 읽어 `rediss://host:port`로 넘기면 되지만, **워커 EC2 → Redis 6379
보안그룹과 TLS·인증 확인이 먼저다.**

## 6. 이번 범위 밖

- **`markDispatching`이 여전히 read-check-write 다.** 조건부 `UPDATE`(`where status = 'PENDING'`)로
  바꾸면 발행 측 중복이 원자적으로 막힌다. 지금은 이벤트가 한 번만 발행되므로 이론적 경로에 가깝다
- **같은 작업에 콜백이 동시에 두 번 들어오면** `draft_dictionary`·`draft_document`가 둘 생길 수
  있다. `D-112`와 같은 패턴으로 닫을 수 있다
- **서로 다른 리뷰 요청이 같은 대상을 동시에 개정**하면 `uk_document_version`·
  `uk_dictionary_workspace_active`가 잡지만 **500으로 나간다**(409가 아니라).
  `DataIntegrityViolationException` → 409 매핑이 남아 있다
- **추출 중 문서가 개정되면 옛 본문 기준 후보어가 조용히 초안이 된다.** 대조는
  `documentVersionNo` 비교로 회수하는데(`DraftDocumentCheckExecutionService:88`) 추출에는 그 장치가
  없다 — `extraction_job`이 문서 버전을 기록하지 않는다
- **`ubidict-py`의 `app/idempotency.py`·`db/schema.sql`(`processed_jobs`)이 죽은 코드다.**
  어디서도 import 하지 않는다. ①응답 큐 시절 설계라 지금의 HTTP 콜백 계약과 맞지 않고
  ②「끝난 뒤 기록」이라 모델 중복 호출을 막지 못하며(그 자리는 `app/claim.py`가 맡았다)
  ③`processed_jobs`가 백엔드 MySQL 에 있어 `backend_db.py`가 지키는 「백엔드 소유 DB 에는 쓰지
  않는다」와도 어긋난다. **삭제를 권한다**(`tests/test_idempotency.py` 포함)
