# T-INT-8 — 추출·대조 real 어댑터(SQS)

상태: **완료(대체됨, 2026-09-14)** | 담당자: `WLSH-166`(AI 워커 전환 세션)
근거: `docs/plan/INTEGRATION_PLAN.md` 1절(변환 매핑) · 2절 Track B(T-INT-8a/8b/8c)
의존: T-INT-7(→ 함께 대체됨)

## ⚠️ 이 태스크는 다른 세션이 다른 모양으로 이미 끝냈다

`T-INT-7`과 같은 경위다 — **AI 워커 전환 세션**(`WLSH-166`, PR #62)이 `D-66`~`D-78`로
설계를 다시 잡고 구현·테스트까지 마쳤다. 아래 체크리스트의 클래스 이름은 하나도 실재하지
않는다. **기능은 전부 있고, 있는 자리가 다르다.**

### 설계가 바뀐 지점

| 이 파일의 전제 | 실제 구현 |
| --- | --- |
| `TermExtractorPort`/`TermCheckerPort`의 `real` 어댑터를 만든다 | **두 포트와 스텁을 통째로 제거했다**(`D-66`·`R-26`). 감출 대상이 프로세스 경계 밖으로 나가 인터페이스가 남을 자리가 없다 |
| 도메인마다 `Sqs*Adapter`를 하나씩 | **공용 포트 하나**(`common/infra/ai/LlmJobRequestSender`) + 어댑터 둘 — `InProcessLlmJobRequestSender`(대역)·`SqsLlmJobRequestSender`(실 큐). 선택은 `app.ai.dispatch.mode` |
| 응답을 `@SqsListener`로 받는 `ExtractionReplyListener`/`CheckReplyListener` | **응답 큐가 없다**(`D-68`). 워커가 치는 동기 HTTP 콜백을 `ExtractionCallbackController`·`CheckCallbackController`(`/api/internal/llm/**`)가 받는다 |
| 어댑터가 문서 본문·용어를 담아 보낸다 | **식별자만 보낸다**(`D-69`). 워커가 DB를 직접 읽으므로 `ubidict-py` 스키마로의 변환 매핑표 자체가 백엔드 범위에서 사라졌다 |
| `app.ai.extractor.mode`/`app.ai.checker.mode`로 `real` 선택 | 두 키는 **제거됐다**. 축이 둘로 갈렸다 — `app.ai.dispatch.mode`(백엔드가 어디로 보낼지)와 `app.ai.mode`(워커가 실제 모델을 부를지, `D-71`) |

### 체크리스트 대조 결과 — 요구한 기능은 전부 있다

- [x] **요청 발행** — `SqsLlmJobRequestSender`(전용 LLM 요청 큐로 발행)
      + `InProcessLlmJobRequestSender`(로컬·테스트 대역)
- [x] **"발행만" 하는 형태로 전환** — `DraftDictionaryExtractionDispatchListener`·
      `DraftDocumentCheckDispatchListener`가 `AFTER_COMMIT`에 상관 식별자를 새기고 발행만
      한다. **발행 실패 시 작업을 `FAILED`로 끝낸다**(그대로 두면 다음 요청이 영구히 막힌다)
- [x] **결과 수신 → `complete`/`fail` 직접 연결** —
      `DraftDictionaryExtractionCallbackService`·`DraftDocumentCheckCallbackService`
- [x] **멱등성** — 종단 상태에 도착한 중복·지각 콜백은 무시하고 **그 경우에도 2xx**로 답한다
      (`D-72`). 4xx로 답하면 워커가 영원히 재시도한다
- [x] **상관 식별자 대조** — 작업마다 발행되는 UUIDv4(`requestId`)를 작업 행에 저장해 두고
      콜백이 같은 값을 돌려주지 못하면 403(`D-70`, 비교는 `MessageDigest.isEqual`)
- [x] **테스트** — `SqsLlmJobRequestSenderTest`, 두 `*DispatchListenerTest`,
      두 `*CallbackControllerTest`(상태 코드를 못 박는다, `D-73`), 두 `*CallbackServiceTest`,
      두 `*TimeoutServiceTest`, 그리고 `support/ai/FakeLlmWorker`가 LocalStack 위에서
      FastAPI 자리를 대신한다(`D-75`)
- [x] `PARTIAL` 취급 — **해당 없음.** 콜백 계약에 `status` 필드가 없다(성공·실패 엔드포인트가
      갈려 있다)
- [x] `jobId` 문자열↔`Long` 변환 — **해당 없음.** `jobId`가 `Long` 그대로 나가고 경로 변수로
      돌아온다
- [x] 응답 없는 작업 회수 — 체크리스트에 없던 항목이지만 함께 들어왔다. `@Scheduled` 스위퍼가
      `app.ai.timeout.job` 뒤에 `FAILED`로 회수한다(`D-77`) — 이것이 없으면 고아 작업 하나가
      워크스페이스 전체의 추출을 영구히 막는다

### 남은 것

**백엔드에는 없다.** 실제 왕복은 FastAPI 워커가 떠야 볼 수 있고, 그건 이 저장소 밖이다 —
루트 `CLAUDE.md` 「미구성 항목」의 「AI 워커(FastAPI) 구현이 이 저장소에 없다」가 그 기록이다.
