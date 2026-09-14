# T-INT-8 — 추출·대조 real 어댑터(SQS)

상태: 대기 | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 1절(변환 매핑) · 2절 Track B(T-INT-8a/8b/8c)
의존: T-INT-7(큐 계약·프로퍼티 키가 먼저 있어야 한다)

`TermExtractorPort`/`TermCheckerPort`의 `real` 어댑터를 만들어 실제로 ubidict-py와
SQS로 왕복한다. **포트 반환값을 없애고 "발행만" 하는 형태로 바꾼다** — 응답이 비동기로
딴 리스너에서 오기 때문이다(기존 `DraftDocumentCheckEventListener`/
`DraftDictionaryExtractionEventListener`가 포트를 동기 호출해 반환값을 바로 쓰던
구조에서, "요청 발행" + "응답 리스너가 job 완료 처리" 2단계로 바뀐다).

## 8a. 추출(Extraction) 어댑터

- [ ] `draftdictionary/infra/adapter/SqsTermExtractorAdapter.java` 신설 —
      `sourceDocumentIds`+문서 본문 조회, `TermSnapshot` 목록을 ubidict-py
      `ExtractRequest` shape으로 변환해 `lovebug-llm-request` 큐에 발행
      (`INTEGRATION_PLAN.md` 1-3절 매핑표의 extract 행 그대로 적용)
- [ ] `draftdictionary/infra/event/ExtractionReplyListener.java` 신설 —
      `@SqsListener("${app.messaging.sqs.llm-reply-queue}")`로 응답 수신,
      `type=="extract"`만 처리, `jobId`를 `Long.parseLong`, `ExtractResponse.candidates`를
      `List<ExtractedTerm>`으로 변환(매핑표 그대로 — `HomographCandidate`는 건너뛰고
      경고 로그)
- [ ] 변환 결과를 `DraftDictionaryExtractionExecutionService.complete(jobId, terms)` /
      `.fail(jobId, reason)`에 직접 전달(포트를 거치지 않는다 — 응답은 요청 호출자와
      다른 스레드/시점에서 온다)
- [ ] `TermExtractorPort` 인터페이스에서 동기 반환값이 필요 없어졌는지 확인하고 필요시
      시그니처 정리(요청 발행 전용 메서드로)

## 8b. 대조(Contrast/Check) 어댑터

- [ ] `draftdocument/infra/adapter/SqsTermCheckerAdapter.java` 신설 — `DocumentSnapshot`
      단일 문서를 `documents:[그 문서 하나]`로 감싸고 `TermSnapshot`을
      `ContrastRequest.dictionary[]`로 변환해 `lovebug-llm-request` 큐에 발행
      (`type` 와이어 값은 `"contrast"` — 백엔드 내부 명칭 "check"와 다름, 매핑표 참고)
- [ ] `draftdocument/infra/event/CheckReplyListener.java` 신설 — 동일 패턴,
      `type=="contrast"`만 처리, `ContrastResponse.suggestions`를
      `List<CheckSuggestion>`으로 변환(`anchor=TextRange(charStart,charEnd)`,
      `originTerm=foundForm`, `suggestionTerm=preferredForm`)
- [ ] `DraftDocumentCheckExecutionService.complete/fail`에 직접 연결

## 8c. 프로퍼티 전환

- [ ] `app.ai.extractor.mode`/`app.ai.checker.mode`에 `@ConditionalOnProperty(havingValue
      = "real")`로 두 어댑터 등록(기존 `TermExtractorStub`/`TermCheckerStub`는 그대로
      두고 `stub`이 여전히 기본값)
- [ ] 로컬에서 `AI_EXTRACTOR_MODE=real AI_CHECKER_MODE=real
      MESSAGING_MODE=sqs`로 기동해 SQS 어댑터가 뜨는지 확인(실제 AWS 큐 접근 필요 —
      자격증명 준비)

## 공통

- [ ] 두 응답 봉투 모두 `status=="PARTIAL"`을 `SUCCESS`로 취급하는지 확인
- [ ] `jobId` 문자열↔`Long` 변환 양방향 단위 테스트
- [ ] 리스너가 멱등한지 확인 — 같은 `jobId` 응답이 중복 수신돼도 `complete()`가
      이미 `SUCCEEDED`인 job에 재호출되지 않게 가드(`CheckJob`/`ExtractionJob`
      도메인 메서드의 상태 검증에 기대거나 명시적으로 확인)
- [ ] 기존 `RealSqsRoundTripITest` 패턴을 참고해 수동/선택 통합 테스트 작성(기본
      `./gradlew check`에는 포함하지 않음 — 실 AWS 비용)
- [ ] `./gradlew spotlessApply && ./gradlew check` 통과(stub 경로 기준)
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-t-int-8`, PR 생성(8a/8b/8c를 커밋 단위로 분리)
