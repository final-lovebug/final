# T-INT-17 — 추출/대조 결과 화면

상태: **코드 작성 완료(QA 대기, 2026-09-14)** | 담당자: (세션 진행)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track B
의존: **T-INT-8** — `WLSH-166`(AI 워커 전환)이 대체 구현으로 이미 끝냈다

`TermExtractionPage`의 "추출 실행" 버튼이 작업을 만들지 않고 목록으로 이동만 했고,
`DocumentReviewPage`는 대조 결과를 문서 2개에 하드코딩된 문단 조각으로 그렸다. 둘 다
실제 작업 접수·폴링·결과 표시로 채웠다.

## 착수 시 정정한 것 (README 「작업 원칙」에 따라 코드보다 먼저)

1. **의존 `T-INT-8`은 "대기"가 아니라 이미 완료였다** — 다만 이 파일이 쓰인 뒤 확정된
   `D-66`~`D-78`의 모양이라 클래스 이름이 전부 다르다. 자세한 대조는
   `T-INT-8-sqs-adapter.md` 참고.
2. **"이미 있는 `fetchSuggestions.ts`(T-INT-10에서 실연동)"는 사실이 아니었다** —
   `T-INT-10`은 대조 제안 클러스터 4개를 **보류**했고 그 파일은 목업이었다. 그래서 이
   태스크가 그 4개까지 함께 실연동했다(사용자 확인 후 범위 확정). `T-INT-10`의 보류 사유
   3가지는 전부 이번에 해소됐다.
3. **`docs/API.md`의 활성 사전집 응답 예시가 실제 DTO와 달랐다**(`dictionaryId` 누락,
   `publishedBy` → 실제는 `createdBy`). 추출 접수가 `dictionaryId`를 요구하는데 프론트가
   `dict-{workspaceId}`로 합성하고 있어 그대로는 연동이 불가능했다 — 문서와 프론트 매핑을
   함께 고쳤다(`CONFLICTS.md` `Y-34`).

## 체크리스트 — 추출(Extraction)

- [x] "추출 실행" 버튼을 `POST /api/draft-dictionaries/extractions`(DI-5) 호출로 교체 —
      `api/createExtractionJob.ts`. 대상은 화면이 이미 고르고 있던 `aligned` 문서들이고,
      `dictionaryId`는 활성 사전집에서 가져온다(없으면 첫 회차라 `null`)
- [x] `jobId` 폴링 훅 — `hooks/useExtractionJob.ts`. 공용
      `shared/api/useAsyncJobQuery.ts` + `shared/api/jobPolling.ts` 위에 얹었다
      (대조와 규격이 같아 훅을 한 벌만 둔다)
- [x] 진행 상태 UI + 완료 시 사전 초안 교정 화면으로 가는 링크
- [x] 실패 시 `failureReason` 표시. 접수 자체가 거절된 경우(`ApiError`)도 따로 표시 —
      진행 중인 초안·추출이 있거나 대상 문서가 없으면 백엔드가 거절한다

## 체크리스트 — 대조(Contrast/Check)

- [x] `DocumentReviewPage`에 "최신 사전집으로 갱신"(`POST /api/draft-documents/checks`,
      DD-5) 진입점 추가 — **초안이 아직 없는 문서에서만** 보인다
- [x] 대조 작업 폴링 — `hooks/useCheckJob.ts`(추출과 같은 공용 훅)
- [x] 결과를 **본문 하이라이트 + 우측 제안 목록 2단**으로 표시(`REQ-CHK-004`) —
      본문은 초안 본문(`draftBody`), 하이라이트는 제안어 `anchor` 기준
      (`model/suggestionSegments.ts`). 하드코딩 문단(`SUGGESTION_PARAGRAPHS`)과
      `model/suggestionFixtures.ts`를 삭제했다

### 함께 실연동한 것 — T-INT-10이 보류했던 제안 클러스터 4개

- [x] `api/fetchSuggestions.ts` — `documentId → draftDocumentId → suggestion-terms`
      두 홉. **초안 본문과 제안어를 함께 돌려준다**(anchor가 본문 오프셋이라 따로 오면
      하이라이트를 그릴 수 없다)
- [x] `api/resolveSuggestion.ts` — 상태 하나가 아니라 `POST .../acceptance` /
      `POST .../rejection`으로 갈린다. 후자는 사유가 필수(`@NotBlank`)라 화면에 입력란을
      두고 비어 있으면 버튼을 막는다(`D-81`)
- [x] `api/fetchSuggestionHistory.ts` — **전용 엔드포인트가 없다.** 판정이 끝난 제안어가
      곧 처리 내역이라 제안어 목록에서 파생한다
- [x] `api/fetchDraftDocuments.ts` — `GET /api/draft-documents?status=EXAMINING`과
      워크스페이스 문서 목록의 교집합(그 엔드포인트에 `workspaceId` 파라미터가 없다).
      미해결 건수는 초안마다 `examine-progress` 한 번씩(N+1, 건수가 적어 감수)
- [x] `api/draftDocumentApi.ts` 신설 — 위 넷이 공유하는 DTO·매핑. 제안어 상태 이름
      (`KEPT_ORIGIN`↔`KEEP_ORIGINAL`)과 `anchor`(`startOffset/endOffset`↔`start/end`)
      변환이 여기 모여 있다

## 공통

- [x] `docs/API.md`의 DraftDictionary·DraftDocument 절과 실제 DTO를 대조 — 위 3번 항목
      참고(사전집 응답 예시 하나를 고쳤다)
- [x] 폴링 정책 확정 — **2초 간격·5분 상한**(`D-79`). 백엔드 회수 스위퍼(기본 15분)보다
      짧아서, 상한에 걸린 것은 실패가 아니라 "그만 지켜봄"으로 표시한다
- [x] 워커가 없는 환경(`app.ai.dispatch.mode=in-process`, 로컬 기본값)에서는 대역이
      **빈 결과로 작업을 끝낸다** — "성공했는데 후보/제안 0건" 케이스의 문구를 양쪽 화면에
      따로 뒀다
- [x] `npx tsc -b`·`npm run lint`·`npm run build` 통과(새 경고 0건)
- [ ] 수동 QA — **로컬 백엔드 기동 후 사용자가 직접 확인 필요.** 실제 왕복까지 보려면
      FastAPI 워커가 필요하다(이 저장소 밖)
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-extraction-check-ui`, PR 생성 — **사용자 지시 시 진행**
