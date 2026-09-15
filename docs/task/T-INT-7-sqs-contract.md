# T-INT-7 — SQS 작업 큐 계약 문서화

상태: **완료(대체됨, 2026-09-14)** | 담당자: `WLSH-166`(AI 워커 전환 세션)
근거: `docs/plan/INTEGRATION_PLAN.md` 1절(SQS 작업 큐 계약) 전체
의존: 없음(T-INT-8의 선행)

## ⚠️ 이 태스크는 다른 세션이 다른 모양으로 이미 끝냈다

이 파일이 쓰인 뒤(2026-09-14 오전) 같은 날 **AI 워커 전환 세션**(`WLSH-166`, PR #62,
`EXECUTION_ORDER.md`의 `T-INT-6`)이 큐 계약을 `D-66`~`D-78`로 확정하고 구현까지 마쳤다.
아래 체크리스트는 **그 세션 이전의 설계**를 전제하고 있어 항목 그대로는 더 이상 맞지 않는다.

> **`docs/task/`의 `T-INT-6`과 `EXECUTION_ORDER.md`의 `T-INT-6`은 서로 다른 태스크다.**
> 전자는 NotificationController 인증 주체 전환, 후자는 AI 워커 전환이다. 두 문서가 같은
> 접두사를 각자 매기고 있어 생긴 충돌이므로 ID를 인용할 때 출처를 함께 적는다.

### 무엇이 달라졌나

| 이 파일의 전제 | 실제 확정 |
| --- | --- |
| 계약을 `CONFLICTS.md`에 `D-62`로 등재 | `D-62`는 회원 조회 API(`T-INT-18`)가 가져갔다. 큐 계약은 **`D-66`~`D-78`**로 등재됐고 계약 본문은 **`docs/AI_CONTRACT.md`**가 정본이다 |
| 봉투가 `{jobId, type, payload}`이고 payload는 `ubidict-py/app/schema.py`를 그대로 채택 | **요청 메시지는 식별자만 싣고 워커가 DB를 직접 읽는다**(`D-69`). 본문을 실으면 문서 상한 10,000자 × 여러 건이 SQS 표준 한도 256KB를 넘는다. 봉투는 `LlmJobRequest` 레코드이며 `contractVersion`·`jobType`을 갖는다(`D-67`·`R-28`) |
| 응답 큐 `llm-reply-queue` 키를 활성화 | **응답 큐를 두지 않는다**(`D-68`). 완료·실패 통보는 워커가 치는 **동기 HTTP 콜백**(`/api/internal/llm/**`)이고, 인증은 작업마다 발행하는 1회용 `requestId` 대조다(`D-70`) |
| `type` 와이어 값이 `"extract"`·`"contrast"` | `jobType`이 **`TERM_EXTRACTION`·`DOCUMENT_CHECK`**다(`LlmJobType`) |
| `jobId`는 문자열 | `jobId`는 `Long` 그대로 나간다 |

### 체크리스트 대조 결과

- [x] 계약을 결정 ID로 등재 — **`D-66`~`D-78` + `docs/AI_CONTRACT.md`**로 완료
- [x] `application.yml`의 큐 키 활성화 — `app.messaging.sqs.queue`와
      `app.messaging.sqs.llm-request-queue`가 활성. **`llm-reply-queue`는 되살리지 않고
      지웠다**(`D-68`, `Y-32` 해소)
- [x] `application-prod.yml` 반영 — `messaging.mode: sqs`, `ai.dispatch.mode: sqs`,
      `ai.mode: real`까지 함께 들어갔다
- [x] 테스트 프로파일 반영 — `src/test/resources/application-test.yml`이
      `app.ai.dispatch.mode: in-process`(기본 대역)와 타임아웃 스위퍼 off를 덮는다.
      실 SQS 왕복을 보는 테스트만 `@TestPropertySource`로 `sqs`를 켠다(`D-75`·`D-78`)
- [x] `EXECUTION_ORDER.md` 태스크 표 — AI 워커 전환이 `T-INT-6`으로 등재돼 있다
      (공유 파일 주인 표의 `common/infra/ai/**` 행 포함)
- [ ] ~~`ubidict-py`의 `task.md`·`queue_schema.py` 정리~~ — **이 저장소 밖이라 여기서
      확인할 수 없다.** `ubidicExtractor/`는 이 워킹 트리에 없다. 다만 **초안 봉투를 그대로
      채택한다는 전제 자체가 `D-69`로 뒤집혔으므로**, 그쪽에 전달할 내용은 "초안 확정"이
      아니라 "계약이 `docs/AI_CONTRACT.md`로 바뀌었다"다 — 워커 구현 시점에 함께 맞춘다

### 남은 것

**없다.** 백엔드 쪽 계약·설정은 전부 반영돼 있다. 워커(FastAPI) 구현과 그쪽 레포의 문서
정리만 저장소 밖 과제로 남는다(루트 `CLAUDE.md` 「미구성 항목」에 이미 기록돼 있다).
