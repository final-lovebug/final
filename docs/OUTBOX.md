# LLM Job Transactional Outbox

이 문서는 추출·대조 요청이 **어떻게 유실 없이 AI 워커에게 전달되는지**를 설명한다. 구현은 `T-INT-25`(커밋 `873095b`)이고, 계약 쪽 서술은 `docs/AI_CONTRACT.md` 2-1절·7절에 있다 — **이 문서는 그 결정의 배경과 내부 구조를 담고, 외부 계약을 복제하지 않는다.**

발표용 슬라이드 버전(그림 포함)이 따로 있다. 내용은 이 문서와 같다.

---

## 1. 문제 — 커밋과 발행 사이의 틈

용어 추출 요청 하나는 저장소 **두 곳**을 건드린다.

| | 무엇 | 어디 |
| --- | --- | --- |
| ① | `extraction_job` 행을 `PENDING`으로 남긴다 | MySQL |
| ② | 워커가 읽을 요청 메시지를 발행한다 | SQS |

두 저장소를 하나의 트랜잭션으로 묶을 수 없다. 전형적인 **dual write**다.

전환 이전 구조는 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`였다. 커밋 **뒤에** 보내야 하는 이유는 분명했다 — 워커는 메시지를 받자마자 DB를 읽고 콜백을 치기 때문에, 커밋 전에 보내면 아직 보이지 않는 행에 콜백이 닿는다. 그래서 발행이 커밋 뒤로 밀렸고, **바로 그 자리에 틈이 생겼다.**

```
BEFORE — dual write
  요청 트랜잭션 ──commit──▶ MySQL ──after_commit──▶ @Async 리스너 ──send()──╳──▶ SQS
   job INSERT               job=PENDING              (in-memory)              (유실)

  발행이 실패하거나 인스턴스가 죽으면 요청은 어디에도 남지 않는다.
  Job 만 진행 중으로 남아 워크스페이스가 잠긴다.

AFTER — transactional outbox
  요청 트랜잭션 ──one commit──▶ MySQL ──claim──▶ 디스패처 ──send()──▶ SQS
   job INSERT                   extraction_job   @Scheduled 5s
   + requestId                  llm_job_outbox   ◀──publish──
   + outbox INSERT

  발행 의도가 DB 에 함께 커밋된다. 프로세스가 죽어도 행은 남고, 다음 스윕이 이어서 보낸다.
```

달라진 것은 화살표 하나다 — 발행이 **메모리 위의 콜백**에서 출발하던 것을 **커밋된 행**에서 출발하도록 옮겼다.

### 틈에서 실제로 일어나는 일

| 언제 | 결과 | 왜 나쁜가 |
| --- | --- | --- |
| 발행 직전 배포·OOM·인스턴스 교체 | Job 은 `RUNNING`, 큐는 비어 있다 | 받을 메시지가 없으니 콜백도 없다. `uk_extraction_job_workspace_in_progress` 때문에 같은 워크스페이스의 다음 요청이 계속 409 로 막힌다 |
| SQS 일시 장애(스로틀·네트워크) | 한 번 실패하면 곧바로 `FAILED` | 리스너가 예외를 잡아 Job 을 실패로 끝냈다. 몇 초 뒤면 성공할 요청인데 재시도 지점이 없었다 |
| 인스턴스가 여럿일 때 | 복구 주체가 없다 | 유실된 발행은 어느 인스턴스의 메모리에 있던 일이라 다른 인스턴스가 이어받을 근거가 없다. 회수는 타임아웃 스위퍼의 실패 처리뿐이었다 |

---

## 2. 설계 — 발행 의도를 행으로 남긴다

요청 트랜잭션은 SQS 를 부르지 않는다. 대신 **보내야 할 메시지 자체를 같은 트랜잭션으로 DB 에 적는다.**

```java
// DraftDictionaryExtractionService.request — 접수 트랜잭션
@Transactional
public ExtractionJobResult request(ExtractionRequestCommand command) {
    ...
    ExtractionJob job = extractionJobWriter.append(...);

    String requestId = LlmJobRequest.newRequestId();
    job.assignRequestId(requestId);                     // 상관 식별자를 먼저 고정
    outboxService.enqueue(LlmJobRequest.termExtraction( // SQS 대신 테이블로
            requestId, job.getId(), job.getWorkspaceId(),
            job.getDictionaryId(), job.getSourceDocumentIds(),
            llmProperties.mode()));
}
```

커밋이 성공하면 발행은 **반드시 일어날 일**이 되고, 롤백되면 outbox 행도 함께 사라져 유령 메시지가 남지 않는다.

`requestId` 를 접수 시점에 고정하는 것이 핵심이다. payload 안의 값과 Job 행의 값이 같은 트랜잭션에서 정해지므로, 재시도로 메시지가 몇 번 나가든 **콜백을 대조할 기준은 하나뿐이다**(`D-70`).

### llm_job_outbox

| 컬럼 | 역할 |
| --- | --- |
| `job_type`, `job_id` | 어느 Job 의 발행인지. `unique (job_type, job_id)` — 한 Job 에 행은 하나뿐이라 접수 경로가 중복 호출돼도 메시지가 둘 생기지 않는다 |
| `request_id` | 콜백 인증 토큰을 겸하는 상관 식별자. 역시 유니크 |
| `payload` | 직렬화된 `LlmJobRequest` 전문. 디스패처는 도메인을 다시 읽지 않고 이 값을 그대로 보낸다 |
| `status` | `PENDING` · `PROCESSING` · `PUBLISHED` · `CANCELLED` |
| `attempt_count`, `next_attempt_at` | 지수 백오프의 상태. 스윕은 `next_attempt_at` 이 지난 행만 집어 든다 |
| `lease_token`, `lease_expires_at` | 집어 든 행의 소유권. 만료되면 다른 인스턴스가 회수한다 |
| `last_error` | 마지막 발행 실패 사유(1,000자로 자름) |

인덱스도 스윕 쿼리에 맞춰 둘이다 — `(status, next_attempt_at)` 은 보낼 차례가 된 행을, `(status, lease_expires_at)` 은 리스가 만료돼 회수할 행을 찾는다.

---

## 3. 디스패처 — 짧게 잠그고, 밖에서 보낸다

5초마다 깨어 최대 20건을 처리한다. 중요한 건 순서다. **claim 트랜잭션은 SQS 호출을 품지 않는다.**

```java
// LlmJobOutboxClaimService — 별도 빈이라 트랜잭션이 여기서 끝난다
@Transactional
List<ClaimedOutbox> claimDispatchable() {
    return repository.findDispatchableForUpdate(now, PageRequest.of(0, 20))
        .stream().map(outbox -> {
            String leaseToken = UUID.randomUUID().toString();
            outbox.claim(leaseToken, now, Duration.ofSeconds(30));
            return new ClaimedOutbox(outbox.getId(), leaseToken, outbox.getPayload());
        }).toList();
}

// LlmJobOutboxDispatcher — 락 밖, 건별 처리
claimService.claimDispatchable().forEach(this::dispatchOne);
```

행을 `PROCESSING` 으로 바꾸고 리스를 새긴 뒤 트랜잭션을 **먼저 닫고**, 네트워크 호출은 락 밖에서 한다. 외부 호출을 트랜잭션 안에 두면 SQS 가 느려질 때 커넥션과 행 락을 그만큼 붙잡는다.

### 보호는 두 겹이다

`PESSIMISTIC_WRITE`(`SELECT … FOR UPDATE`)는 **같은 순간** 스윕하는 두 인스턴스가 같은 행을 집는 것을 막고, 리스는 **집어 든 뒤 죽는** 경우를 막는다. 락은 트랜잭션과 함께 풀리므로, 락만으로는 `PROCESSING` 에 멈춘 행을 아무도 되찾을 수 없다.

```
t=0                     t=0+ε              t=30s          리스 만료 후 첫 스윕
 │                        │                  │                    │
A├ FOR UPDATE → PROCESSING + lease ─╳ 프로세스 종료
 │                        │                  │                    │
B│                        ├ 같은 행 skip      │                    ├ lease_expires_at < now
 │                        │  (락 대기·미대상)  │                    │   → 재claim
 │                        │                  │                    │
행 PENDING                 PROCESSING(소유자 A) ──────────────────▶ PROCESSING(소유자 B)
```

리스가 막는 것은 경합이 아니라 **고아 행**이다.

### 상태 전이

발행 직전에 디스패처는 도메인에 한 번 물어본다. `LlmJobDispatchLifecycle.prepare` 가 `PENDING → RUNNING` 전이를 시도하고, **Job 이 이미 끝났거나 `requestId` 가 다르면 발행을 포기하고 행을 `CANCELLED` 로 닫는다.** 상태 전이의 주인은 여전히 도메인이고, 공통 인프라는 포트를 통해 물어보기만 한다.

```
            claim + lease 30s            send 성공
  PENDING ───────────────────▶ PROCESSING ──────────▶ PUBLISHED
     ▲                             │                   published_at
     │                             │
     └──── reschedule ─────────────┘
           발행 실패 · 리스 만료 → attempt++ , 백오프 1→60s
     │                             │
     └───────────┬─────────────────┘
                 ▼
            CANCELLED
   Job 이 이미 종결 · requestId 불일치 · 타임아웃 회수
```

재시도는 **지수 백오프에 60초 상한**(1·2·4·8·16·32·60s)으로 돌고, 무한히 돌지 않도록 바깥에서 끊긴다 — Job 타임아웃 스위퍼가 Job 을 실패로 회수할 때 `outboxService.cancel(requestId)` 로 행까지 닫는다.

---

## 4. 대가 — at-least-once, 그래서 중복 방어

outbox 는 유실을 없애는 대신 **중복을 허용한다.** SQS 가 성공을 응답한 *뒤*, `PUBLISHED` 를 커밋하기 *전에* 프로세스가 죽으면 리스 만료 후 같은 메시지가 한 번 더 나간다. 이건 버그가 아니라 선택이다 — **유실과 중복 중 하나를 골라야 하고, 중복은 막을 수 있다.**

```
같은 requestId, 두 번 배달
  msg #1 ─┐
  msg #2 ─┴─▶ GATE 1 ──▶ (제거됨) ──▶ GATE 3 ──▶ GATE 4
  (재배달)     DB 유니크    Redis 선점    Job 상태 재확인  콜백 멱등
             (job_type,   R-35 로       RUNNING?        초안 하나
              job_id)     걷어냈다       종결이면 ack
             ─────────   ──────────    ────────────    ──────────
  막는 지점:  접수 경로의   —            타임아웃 회수와  콜백 재시도
             중복 요청                  경합한 지연 메시지
```

**Gate 3 은 비용이 드는 모델 호출을 막고, Gate 1·4 는 데이터 중복을 막는다. 둘은 다른 문제다.**

> ⚠️ **Gate 2 를 제거했다**(`R-35`, 2026-09-16). 운영에서 워커에 `REDIS_URL` 이 없어 모든 `REAL` 작업이
> 실패했고, 되살리는 비용 대신 걷어냈다. **그래서 「아직 `RUNNING` 인 작업의 재배달」은 이제 아무것도
> 막지 못한다** — Gate 3 은 *종결된* 작업만 걸러내고, Gate 4 는 이미 나간 모델 호출을 되돌리지 못한다.
> 중복 배달마다 요금이 두 배로 나가며 어디에도 실패로 남지 않는다. 규격은 `docs/AI_CONTRACT.md` 7-2-2.

Gate 3 은 워커 쪽에 있다. outbox 재시도와 백엔드 타임아웃 회수가 경합하면 *이미 실패로 끝난 Job* 의 메시지가 늦게 도착할 수 있다. 그 상태로 모델을 부르면 버려질 결과에 돈을 쓰는 것이다.

```python
# app/queue_consumer.py
if job.mode == "REAL":
    if not is_running_llm_job(job.jobType, job.jobId, job.requestId):
        client.delete_message(...)   # 모델을 부르지 않고 ack
        return
```

`stub`·`mock` 은 호출 비용이 없어 읽기 한 번을 더 들일 이유가 없다 — **`REAL` 에서만 통과한다.**

---

## 5. 실패 경로와 타이밍 예산

outbox 는 **발행까지**만 책임진다. 메시지가 큐에 들어간 뒤의 실패는 다른 장치가 받는다.

| 장치 | 담당 |
| --- | --- |
| outbox 백오프 재시도 | 발행 자체의 실패 |
| `LlmJobDeadLetterListener` | 워커가 ack 하지 못해 redrive 를 소진한 요청. payload 의 `jobType`·`jobId`·`requestId` 를 대조해 Job 을 `FAILED` 로 끝낸다 |
| Job 타임아웃 스위퍼(`D-77`) | DLQ 에도 닿지 못한 무응답. 최후 회수 |

세 장치의 **순서가 어긋나면 서로를 무력화한다.** 그래서 숫자를 함께 정했다.

```
0        15m       30m       45m       50m               60m
├─────────┼─────────┼─────────┼─────────┼─────────────────┤
│                                                           경과 시간
▌ outbox sweep 5s — 접수와 발행 사이의 최대 지연
├─────────┼─────────┼─────────┤
  worker visibility 15m × 3회
                              ● 45m — DLQ 리스너가 Job 을 FAILED 로
                                        ● 50m — job timeout, 최후 회수
```

- **DLQ 도착(45m)이 Job 타임아웃(50m)보다 먼저**여야 사용자가 "워커 재시도 소진" 같은 구체적인 사유를 본다.

| 값 | 설정 | 출처 |
| --- | --- | --- |
| outbox sweep | `PT5S` | `app.ai.outbox.sweep-interval` (`AI_OUTBOX_SWEEP_INTERVAL`) |
| lease | 30s | `LlmJobOutboxClaimService.LEASE_DURATION` |
| backoff | 1→60s (2ⁿ, 상한 60) | `LlmJobOutboxClaimService.retryDelay` |
| batch | 20 | `LlmJobOutboxClaimService.BATCH_SIZE` |
| job timeout | `PT50M` (prod) / `PT15M` (기본) | `app.ai.timeout.job` |
| idempotency TTL | 3000s | 워커 `LLM_IDEMPOTENCY_TTL_SECONDS` |

---

## 6. 남은 것

- **발행이 최대 5초 늦는다.** 폴링 기반이라 접수 즉시 나가지 않는다. 추출·대조는 모델 호출이 분 단위인 작업이고 접수 응답은 202, 사용자는 폴링으로 상태를 보므로(`D-34`) 5초를 문제로 보지 않았다. 더 줄여야 하면 커밋 직후 스윕을 한 번 깨우는 것이 다음 단계다.
- **`PUBLISHED` 행이 쌓인다.** 정리(아카이빙·파티션 드롭)가 아직 없다. 인덱스가 `status` 를 앞에 두고 있어 스윕 성능에는 영향이 없지만 보관 정책은 정해야 한다.
- **outbox 자체에는 재시도 상한이 없다.** 백오프는 60초에서 멈추고 계속 돈다. 멈추는 근거가 바깥에 있어서, **`app.ai.timeout.enabled=false` 인 환경에서는 무한히 재시도한다.**
- **이전 경로가 아직 코드에 있다.** `DraftDictionaryExtractionDispatchListener`·`DraftDocumentCheckDispatchListener` 와 `markDispatching` 이 `@Deprecated` 로 남아 있다(테스트 호환). 접수 경로가 이벤트를 더 이상 발행하지 않으므로 동작하지는 않지만, 제거는 별도 정리 대상이다.

> **한 문장으로** — outbox 는 신뢰성을 *얻는* 장치가 아니라 **유실을 중복으로 바꾸는 장치**다. 중복은 `requestId` 와 유니크 제약으로 막을 수 있고 유실은 막을 수 없다는 판단이 이 설계의 전부다.

---

## 7. 코드 위치

| | 파일 |
| --- | --- |
| 엔티티·상태 | `common/infra/ai/LlmJobOutbox`, `LlmJobOutboxStatus` |
| 접수 | `common/infra/ai/LlmJobOutboxService` |
| 스윕·claim | `common/infra/ai/LlmJobOutboxDispatcher`, `LlmJobOutboxClaimService`, `LlmJobOutboxRepository` |
| 도메인 포트 | `common/infra/ai/LlmJobDispatchLifecycle`, `LlmJobFailureHandler` |
| 포트 구현 | `draftdictionary/infra/ai/ExtractionJobOutboxLifecycle`, `draftdocument/infra/ai/CheckJobOutboxLifecycle` |
| DLQ | `common/infra/ai/sqs/LlmJobDeadLetterListener` |
| 스키마 | `db/migration/V1__init_schema.sql` (`llm_job_outbox`) |
| 워커 | `ubidicExtractor/ubidict-py/app/queue_consumer.py`, `app/backend_db.py`, `app/claim.py` |
