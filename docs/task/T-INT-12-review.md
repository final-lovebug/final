# T-INT-12 — review(리뷰 요청) 실연동

상태: **백엔드 완료(2026-09-14), 프론트 대기** | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A · 2026-09-14 사용자 결정(`D-63`)
의존: 없음(다른 도메인 태스크와 독립)

`frontend/src/features/review/**`가 전부 목업이다. 백엔드 `reviewrequest` 도메인
(`ReviewRequest`·`Revision*`·`Reviewer`·`Review`·`Comment`)에 대응한다.

## 백엔드 생명주기(2026-09-14 소스 확인, 담당자가 바뀌어도 참고할 것)

1. **`POST /api/draft-documents/{draftDocumentId}/review-request` /
   `POST /api/draft-dictionaries/{draftDictionaryId}/review-request`** —
   `ReviewRequest` + `RevisionDocument`(또는 `RevisionDictionary`) 한 행을 한
   트랜잭션에서 생성(`D-44`). `status = PENDING_REVIEW`.
2. **`POST .../reviewers`** — `Reviewer`(지정, 알림용). 정족수 판정 자체는
   워크스페이스 룰셋의 `requiredReviewerCount`를 쓴다(`G-4`) — 지정 안 된
   참여자도 검토 가능.
3. **`POST .../reviews`** — 리뷰어의 검토 제출. **verdict(`APPROVED`/
   `CHANGES_REQUESTED`)와 코멘트 배열을 한 번에, 한 트랜잭션으로 받는다**:
   ```json
   { "targetRound": 2, "verdict": "CHANGES_REQUESTED",
     "comments": [{ "content": "...", "targetItemId": 41 }] }
   ```
   `targetItemId`는 사전 개정안이면 후보어(`CandidateTerm`) id, 문서 개정안이면
   `anchor`(본문 위치)를 쓴다. 제출과 동시에 정족수를 재계산해
   `ReviewRequest.status`를 갱신한다(`CHANGES_REQUESTED`가 하나라도 있으면 즉시
   전환, 없고 정족수 채우면 `APPROVED`).
4. **`POST /api/reviews/{reviewId}/comments`** — 이미 제출된 검토에 코멘트를
   "추가로" 더 달 때(답글 등). `CHANGES_REQUESTED`가 되면 요청자가
   `POST .../reexaminations`(재교정, 어떤 코멘트를 처리했는지 기록)를, 정족수를
   채우면 `POST .../revision`(반영)을 호출한다.

## 결정 사항(2026-09-14, `D-63`)

### ✅ 해결 — 코멘트는 검토 제출과 함께 보낸다(백엔드 변경 불필요)

원래 "코멘트를 달려면 검토(`Review`)가 먼저 있어야 한다"를 구조적 결함으로
봤으나, **의도된 흐름이 정확히 그 모양이었다**: 리뷰어가 화면에서 단어별/위치별
코멘트를 작성하는 동안은 **프론트 로컬 상태**에만 쌓아 두고(GitHub의 "pending
review"를 서버가 아니라 프론트가 들고 있는 것), approve/change request를 고르는
순간 코멘트 배열 + verdict를 위 3단계 `POST .../reviews` 한 번으로 제출한다.
**백엔드 스키마·엔드포인트를 바꿀 필요가 없다** — 프론트에 리뷰 작성 화면(코멘트
컴포저 + 로컬 draft 상태 + 최종 제출)을 새로 만드는 것으로 끝난다.

### ✅ 결정 — 목록 응답에 필요한 속성을 백엔드가 추가한다

`ReviewRequestResponse`에 대상 문서/사전집 id와 `reviewerCount`를 직접 포함시킨다
(프론트 N+1 호출 대신). `ReviewRequest` 엔티티엔 대상 id가 없어
`RevisionDocument`/`RevisionDictionary`를 조인해야 하고, `reviewerCount`는
`Reviewer` 테이블 집계가 필요 — 목록 조회 서비스에 조인·집계 쿼리 추가가
필요하다(사소한 필드 추가 이상의 작업).

### ✅ 결정 — 사전 개정안 "삭제" 행은 이번엔 만들지 않는다

이전 버전과 비교해야 판별 가능한데 그 비교 API가 없다. **사용자 결정(2026-09-14):
"삭제" 분류 없이 진행** — 프론트는 후보어 목록의 `origin`/상태만으로 "추가"·
"정의수정"만 표시한다. 이전 버전과 비교해 사라진 표준어를 찾는 기능은 범위 밖으로
남긴다(필요해지면 별도 태스크).

## 체크리스트

### 백엔드 — 완료
- [x] `ReviewRequestResponse`/`ReviewRequestResult`에 `targetId`(대상 문서/사전집
      id)·`reviewerCount` 추가
- [x] `RevisionDocumentReader`/`RevisionDictionaryReader`에
      `readLatestByReviewRequestIds`(배치, 재교정 회차 중 최신만) 추가,
      `RevisionDocumentRepository`/`RevisionDictionaryRepository`에
      `findAllByReviewRequestIdIn` 추가
- [x] `ReviewerReader.countsByReviewRequestIds`(배치 집계) +
      `ReviewerRepository.countByReviewRequestIdIn`(`@Query` group by,
      `ReviewerCount` 프로젝션) 추가
- [x] `ReviewRequestService.search()`/`read()`가 위 배치 조회로 enrichment
      (N+1 없이 한 번씩만 조회). `create()`/`update()`/`cancel()`은 미enrichment로
      유지(대상 id 없어도 되는 경로 — 필요해지면 후속)
- [x] 테스트 — `DraftReviewRequestServiceTest.search_includesTargetIdAndReviewerCount`
      (실제 리뷰 요청+개정안+리뷰어로 `targetId`/`reviewerCount` 검증),
      `read_dictionaryTargetIdNullOnFirstVersion`(첫 사전집은 targetId null)
- [x] `docs/API.md` ReviewRequest 절에 상세 응답 JSON 예시 + `targetId`/
      `reviewerCount` 필드 설명 추가(기존에 예시 자체가 없었음)
- [x] `docs/plan/CONFLICTS.md`에 `D-63`으로 등재(이 파일의 "결정 사항" 절 요약)
- [x] `./gradlew check` 통과 확인(2026-09-14, `BUILD SUCCESSFUL` — WLSH-171 세션에서 재실행)

### 프론트
- [ ] `api/fetchDocumentReviewRequests.ts` (리뷰 요청 목록) — 위 백엔드 작업
      완료 후 필드 그대로 매핑
- [ ] `api/requestDocumentReview.ts` (리뷰 요청 생성) — `POST /api/draft-documents/
      {id}/review-request` 등 T-INT-5 진입점 사용
- [ ] **신규 — 리뷰 작성 화면**(코멘트 컴포저 + 로컬 draft 상태 + approve/change
      request 제출). `DictionaryRevisionPage.tsx`·`DocumentReviewThreadPage.tsx`의
      Approve/Change request 버튼이 지금 `onClick` 없이 미연결 — 이 작업으로 채움.
      사전 개정안은 후보어(`targetItemId`)별 코멘트, 문서 개정안은 `anchor` 기반
- [ ] `api/fetchDictionaryRevision.ts` (사전 개정안 조회 — 후보어 목록 표시.
      "삭제" 행은 만들지 않음, "추가"·"정의수정"만 `origin`/상태로 판별)
- [ ] `api/fetchReviewThreadComments.ts` / `addReviewThreadComment.ts` /
      `addRevisionComment.ts` — 코멘트 조회·답글
- [ ] `model/types.ts` — 실제 API 응답에 맞춰 조정
- [ ] `model/fixtures.ts`·`model/reviewRequestFixtures.ts` — 화면이 더 이상
      참조하지 않으면 그대로 둬도 무방
- [ ] 화면 확인: 리뷰 요청 목록, 리뷰 작성/제출, 코멘트, 사전 개정안에서 실
      데이터 표시 QA(정족수 판정 등 `RuleSet` 관련 화면 있으면 함께 확인)
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-review-real-api`, PR 생성
