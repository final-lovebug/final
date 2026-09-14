# T-INT-10 — document 실연동

상태: 진행중(2026-09-14, 5/10 완료 — 아래 참고) | 담당자: (세션 진행)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A
의존: 없음(다른 도메인 태스크와 독립). 이름 표시만 `T-INT-18`(범위 확장됨) 영향

`frontend/src/features/document/**`가 전부 목업이다. 파일이 10개로 가장 많다 —
필요하면 여러 명이 파일 단위로 나눠 가져가도 된다(단, `model/types.ts`는 마지막에
한 번에 정리하는 편이 충돌이 적다).

## 체크리스트 — api 파일별 (`httpClient`로 교체 + `docs/API.md` Document 절과 대조)

- [x] `api/fetchDocuments.ts` (목록) — 완료. `ownerName`/`updaterName`은 실 API에
      없어(T-INT-18) "—" 표시로 대체, `labels[]`는 첫 번째만 `label`로, `badge`는
      `aligned`/`edited`에서 유도. **T-INT-18 완료 후 보강**: `uploaderId`를
      `GET /api/members?ids=`(배치 조회)에 모아서 한 번에 넘기고 이름으로 교체 —
      파일 구조는 그대로 두고 매핑 부분만 고치면 됨(막혀 있던 게 아니라 후속 보강)
- [x] `api/fetchDocument.ts` (상세) — 완료. **시그니처가 `(workspaceId, documentId)`로
      바뀜**(실 엔드포인트가 workspace-scoped라 documentId만으론 부족) — `useDocument`
      훅과 3개 호출부(`DocumentDetailPage`·`DocumentReviewPage`·`DocumentReviewThreadPage`)
      모두 수정. **T-INT-18 완료 후 보강**: `uploaderId` → `GET /api/members/{id}`
      (단건 조회)로 이름 교체
- [x] `api/fetchDocumentVersions.ts` (버전 이력) — 완료. 마찬가지로
      `(workspaceId, documentId)`로 시그니처 변경. 목록 응답엔 본문이 없어(단건 조회
      전용) `body`를 옵셔널로 바꾸고 화면은 "—" 표시
- [x] `api/createDocument.ts` (업로드/생성) — 완료. 생성자는 요청자 본인이라
      ownerName/updaterName은 조회 없이 입력값을 그대로 씀(T-INT-18 영향 없음). 업로드
      폼에 라벨 입력이 없어 `labels`는 항상 빈 배열로 전송
- [x] ~~`api/createLabel.ts`~~ — **목업 유지(2026-09-14 사용자 결정).** 백엔드에 독립
      라벨 생성 API가 없다 — 라벨은 문서 생성/수정 시(`POST`/`PATCH .../documents{,/​{id}}`
      의 `labels` 필드) 함께 붙여야만 생긴다(`docs/API.md` 778행 "워크스페이스 라벨 목록
      조회" 절 — 목록 조회만 있고 생성 엔드포인트가 없음을 명시). `SettingsLabelsPage`의
      "생성" 버튼은 계속 목업으로 동작하되, 실제 라벨 생성은 문서 작성 화면에서 한다는
      점을 화면에 안내 문구로 남긴다
- [x] `api/fetchLabels.ts` — 완료(`GET /api/workspaces/{workspaceId}/labels`, `documentCount`는
      응답에 없어 0 고정)

### 대조 제안 클러스터(4개) — 보류(2026-09-14, 아래 참고)

- [ ] ~~`api/fetchDraftDocuments.ts`~~ / ~~`api/fetchSuggestions.ts`~~ /
      ~~`api/fetchSuggestionHistory.ts`~~ / ~~`api/resolveSuggestion.ts`~~

**보류 사유**: 이 4개는 API만 바꾼다고 끝나지 않는다.
1. **id 체인이 한 겹 더 있다** — `fetchSuggestions(documentId)`류는 실제로는
   `draftDocumentId`가 있어야 호출 가능한 엔드포인트들이다. 다행히 이건 진짜 막힌
   건 아니다 — `GET /api/draft-documents?documentId={id}`로 조회 가능(T-INT-11/T-INT-14
   때와 달리 lookup 자체는 있음).
2. **더 근본적인 문제 — `DocumentReviewPage.tsx`의 본문 렌더링이 특정 문서 2개
   (`doc-plan`/`doc-retention`)에 대해서만 하드코딩된 문단 조각(`SUGGESTION_PARAGRAPHS`)
   을 쓴다.** 코드 주석에 이미 "실제 하이라이트는 백엔드의 대조 결과 위치(anchor)가
   있어야 재현 가능해서 지금은 생략했다"고 적혀 있다. 즉 이 4개 api를 실연동해서 실제
   문서의 제안 데이터를 받아와도, **그 데이터를 문서 본문 위에 표시하는 로직 자체가
   아직 데이터 기반이 아니라서** 임의의 실제 문서에서는 화면이 깨지거나 아무것도
   안 보인다.
3. `resolveSuggestion`도 실제로는 상태 하나(`PATCH`)가 아니라 `POST .../acceptance`
   (적용) / `POST .../rejection`(유지, `rejectReason` 필요)으로 엔드포인트가 나뉜다 —
   지금 `ResolveSuggestionInput`엔 `rejectReason`이 없다.

**따라서 이 4개는 API 연동보다 `DocumentReviewPage.tsx`의 본문·anchor 렌더링을 먼저
데이터 기반으로 다시 짜는 게 선행돼야 값어치가 있다** — 별도 세션 권장.

## 나머지

- [x] `model/types.ts` — `Document`/`DocumentVersion`의 실 API 미제공 필드 옵셔널화,
      `aligned`/`edited`/`dictionaryVersionNo` 추가
- [x] `model/labelFixtures.ts`·`model/suggestionFixtures.ts`·`model/versionFixtures.ts`·
      `model/fixtures.ts` — 그대로 둠(제안 클러스터가 아직 참조 중)
- [x] `npx tsc -b`·`npm run lint` 통과
- [ ] 본문 직접 편집(`PATCH .../documents/{id}/content`, `G-9`) 화면 — 이번 범위에
      확인된 화면 없음(후속 확인)
- [ ] 화면 확인: 문서 목록·상세·업로드·버전이력·라벨에서 실 데이터 표시 QA — **로컬
      백엔드 기동 후 사용자가 직접 확인 필요**. 초안 목록·대조 제안은 보류라 대상 아님
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-document-real-api`, PR 생성 — **사용자 지시 시
      진행**
