# T-INT-17 — 추출/대조 결과 화면

상태: 대기 | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track B
의존: **T-INT-8**(SQS real 어댑터) — 그 전에 붙여도 항상 빈 결과만 나온다. 화면
골격만 먼저 만들고 싶다면 T-INT-8 이전에 시작해도 되지만, "실제로 동작 확인"은
T-INT-8 완료 후에만 가능하다.

현재 `TermExtractionPage`의 "추출 실행" 버튼은 작업을 만들지 않고 목록 화면으로
이동만 한다. `DocumentReviewPage`도 대조(DictionaryContrast) 결과를 아직 다루지
않는다(코드 주석이 "MVP1 제외"라 명시). 이 태스크에서 둘 다 실제 작업 접수·폴링·결과
표시로 채운다.

## 체크리스트 — 추출(Extraction)

- [ ] `TermExtractionPage`(`/documents/:documentId/extract`)의 "추출 실행" 버튼을
      `POST /api/draft-dictionaries/extractions`(DI-5) 호출로 교체 — 응답의
      `extractionJobId`를 받는다
- [ ] `jobId`로 `GET /api/draft-dictionaries/extractions/{jobId}`를 폴링하는 훅 신설
      (예: `useExtractionJobPolling`) — 상태(`PENDING`/`RUNNING`/`SUCCEEDED`/`FAILED`)에
      따라 화면 갱신, 완료되면 폴링 중단
- [ ] 진행 상태 UI(스피너/진행 표시) + 완료 시 결과(후보어 목록)를 보여주고 사전 초안
      교정 화면으로 이동하는 링크 제공
- [ ] 실패 시 에러 메시지 표시

## 체크리스트 — 대조(Contrast/Check)

- [ ] `DocumentReviewPage`(또는 적절한 화면)에 "최신 사전집으로 갱신"
      (`POST /api/draft-documents/checks`, DD-5) 진입점 추가
- [ ] 동일한 폴링 훅(또는 대조 전용 훅) 신설, `GET /api/draft-documents/checks/{jobId}`
      폴링
- [ ] 완료 시 결과(대조 제안 목록)를 문서 본문 하이라이트 + 우측 제안 목록 2단 구성으로
      표시(`REQ-CHK-004`) — 이미 있는 `fetchSuggestions.ts`(T-INT-10에서 실연동)와
      연결

## 공통

- [ ] `docs/API.md`의 DraftDictionary·DraftDocument 절에서 정확한 엔드포인트·응답
      shape 재확인
- [ ] 폴링 간격·최대 재시도(타임아웃) 정책 결정(예: 2초 간격, 5분 타임아웃) — 과한
      요청이 되지 않게 backoff 고려
- [ ] **T-INT-8 완료 전**이라면 스텁이 항상 빈 리스트를 돌려주므로 "성공했지만 후보/제안
      0건" 케이스의 UI도 자연스러운지 확인해 둔다(스텁 상태에서도 화면이 깨지지 않아야
      함)
- [ ] 수동 QA(T-INT-8 완료 후): 실제 문서로 추출·대조를 돌려 결과가 화면에 반영되는지
      확인
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-extraction-check-ui`, PR 생성
