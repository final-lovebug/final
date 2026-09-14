# T-INT-12 — review(리뷰 요청) 실연동

상태: **코드 작성 완료(QA 대기, 2026-09-14 WLSH-171)** | 담당자: (WLSH-171 세션)
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
- [x] `api/fetchDocumentReviewRequests.ts` — `GET /api/review-requests?workspaceId=&type=DOCUMENT`.
      `targetId`·`reviewerCount`를 그대로 매핑(N+1 없음). 공통 응답 타입·매핑은
      `api/reviewApi.ts`로 분리
- [x] `api/requestDocumentReview.ts` — `GET /api/draft-documents?documentId=`로 초안을 먼저
      찾고 `POST /api/draft-documents/{draftDocumentId}/review-request`. 리뷰어는 생성 본문의
      `reviewerMemberIds`로 함께 지정된다(별도 POST 불필요). 요청자·workspaceId는 제거
      (인증 주체·초안에서 해석)
- [x] **리뷰 작성 화면** — 두 페이지 모두 코멘트를 로컬 draft(`pending`)에 쌓고 verdict를
      고르는 순간 `POST .../reviews`로 한 번에 제출(`D-63`). 미제출 코멘트는 점선 카드로
      구분 표시한다. 사전 개정안은 후보어별(`targetItemId`), 문서 개정안은 문서 전체 단위
      (anchor 기반 위치 지정은 `T-INT-10` 제안 클러스터·`T-INT-17`과 함께 해야 해서 제외)
- [x] `api/submitReview.ts` 신설 — 제출 + `fetchReviews` + `fetchReviewProgress`.
      `targetRound`는 개정안의 `reexamineRound`에서 읽는다(하드코딩 아님)
- [x] `api/fetchDictionaryRevision.ts` — **세 번 호출한다**(개정안 + 후보어 + 코멘트) +
      리뷰 요청 단건. 개정안 응답에 후보어가 없어 `draftId`로 따로 조회한다.
      라우트의 `'current'` sentinel을 진행 중 사전 리뷰 요청 조회로 푼다.
      변경 유형은 결정 5대로 판별
- [x] `api/fetchReviewThreadComments.ts` — **reviewRequestId** 기준으로 시그니처 변경,
      `resolved`·`targetItemId` 필터 지원, 답글은 `children` 트리로 온다
- [x] `api/addReviewThreadComment.ts` — `POST /api/reviews/{reviewId}/comments`.
      최초 제출 코멘트가 아니라 **이미 제출된 검토에 덧붙이는 답글·보충용**
- [x] ~~`api/addRevisionComment.ts`~~ — **제거.** 같은 엔드포인트라 중복이었다
      (`useAddRevisionComment` 훅도 함께 제거)
- [x] `model/types.ts` — `ReviewRequestStatus`를 백엔드와 1:1로 교정(`PENDING`→`PENDING_REVIEW`,
      `MERGED`→`REVISED`, `CANCELLED`→`CANCELED`, `REJECTED` 제거), `ReviewVerdict`도 교정
      (`APPROVE`→`APPROVED` 등, `REJECT` 제거). `ReviewRequest`에서 `reviewers[]`를 빼고
      `reviewerCount`·`targetId`로, `Comment`에 `children` 추가, `ReviewProgress` 신설
- [x] `model/fixtures.ts` — 목업 배열을 전부 걷어내고 화면 전용 타입만 남김
      (`RevisionDictionaryTermRow`에 `candidateTermId` 추가). `CURRENT_DICTIONARY_REVISION_ID`는
      라우팅 sentinel이라 유지
- [x] ~~`model/reviewRequestFixtures.ts`~~ — 파일째 제거(참조 없음)
- [x] **범위 C 추가분**(결정 7) — `api/reviewers.ts`·`api/reviewLifecycle.ts`·
      `api/fetchReviewRequest.ts`·`api/fetchDocumentRevision.ts` 신설, 훅 5개 추가,
      `components/ReviewerPanel.tsx` 신설(참여자 목록은 prop으로 받아 features 간 참조 회피),
      두 페이지에 재교정·반영 버튼 연결
- [x] `shared/lib/relativeTime.ts` 신설 — `Intl.RelativeTimeFormat` 기반, 의존성 추가 없음(결정 6)
- [x] `npx tsc -b` 통과, `npx oxlint src` 통과(경고 4건은 전부 기존 것)
- [ ] 화면 확인: 리뷰 요청 목록, 리뷰 작성/제출, 코멘트, 사전 개정안에서 실
      데이터 표시 QA(정족수 판정 등 `RuleSet` 관련 화면 있으면 함께 확인)
- [x] 커밋 브랜치 — `feat/WLSH-171-frontend-integration`에 태스크별 커밋으로 담았다
      (T-INT-11과 한 브랜치. 지라 티켓이 WLSH-171 하나뿐이라 나누지 않았다 — `T-INT-18`이
      `fix/WLSH-164-fe-be-intgreation`에 얹은 것과 같은 방식)


---

## 2026-09-14 프론트 착수 사전 확인 (WLSH-171)

착수 전 체크리스트를 실제 컨트롤러 7개와 대조했다. **어긋나는 지점이 여럿이다.**

| 체크리스트 기대 | 실제 API |
|---|---|
| `fetchReviewThreadComments(reviewId)` | 조회는 **reviewRequestId** 기준(`GET /api/review-requests/{id}/comments`), 작성만 reviewId 기준(`POST /api/reviews/{reviewId}/comments`) — **읽기·쓰기 키가 다르다** |
| `fetchDictionaryRevision(revisionId)` | `GET /api/review-requests/{id}/revision-dictionaries`. `RevisionResponse`에 **후보어 목록이 없다** — `draftId`(draftDictionaryId)로 후보어를 또 조회해야 한다(T-INT-11에서 만든 조회 재사용) |
| `requestDocumentReview(documentId)` | `POST /api/draft-documents/**{draftDocumentId}**/review-request` — `GET /api/draft-documents?documentId=`로 초안을 먼저 찾아야 한다 |
| `ReviewRequest.reviewers[]` | 응답엔 `reviewerCount`만. 목록은 `GET /api/review-requests/{id}/reviewers` 별도 |
| `ReviewRequestStatus` | 프론트 `PENDING`·`MERGED`·`REJECTED`·`CANCELLED` ↔ 백엔드 `PENDING_REVIEW`·`REVISED`·(REJECTED 없음)·`CANCELED`. T-INT-11의 `CandidateTermStatus`와 같은 종류의 불일치 |
| `RevisionCommentListItem` | `{initial, name, tone, time:"40분 전", canConvert}` — id도 작성자 id도 없는 순수 뷰 타입. `CommentResponse` 기준으로 다시 짜야 한다 |

### 결정 (2026-09-14 사용자 결정)

**결정 5 — 개정안 표의 변경 유형은 `origin` + `status`로 판별한다.**
- `EXTRACTED` → **추가**
- `EXISTING` 이면서 `status !== 'KEPT'` → **정의 수정**
- `EXISTING` 이면서 `status === 'KEPT'` → **표에서 제외**(손대지 않은 승계분은 변경이 아니다)

`createExisting`이 승계분을 `KEPT`로 만들고 `edit()`은 상태를 바꾸지 않으므로, **승계 용어를
고친 뒤 등재 승인까지 해야 `REGISTRATION_APPROVED`가 되어 '정의 수정'으로 잡힌다.** 이전
버전과의 정의 비교 API가 없어 이보다 정확하게는 판별할 수 없다(「삭제」를 제외한 것과 같은 이유).

**결정 6 — 개정안 코멘트 뷰에서 `canConvert`를 제거하고 상대시간은 직접 구현한다.**
`tone`은 작성자 id로 결정하고, `time`("40분 전")은 `createdAt`을 상대시간으로 바꾸는 유틸을
`shared/lib`에 만든다. "후보어로 전환"(`canConvert`)은 대응 기능이 없어 제거한다.

**결정 7 — 리뷰 생명주기를 전부 연동한다(범위 C).**
리뷰 제출·반영뿐 아니라 **리뷰어 지정 화면을 새로 만들고** 재교정까지 넣는다. 화면 현황:
`Approve`·`Change request` 버튼은 두 페이지에 이미 있고 `onClick`만 비어 있다
(`DocumentReviewThreadPage.tsx` 11행 주석 "백엔드가 없어 아직 붙이지 않았다"),
`Approve · r8 발행`은 지금 네비게이션만 한다. **새로 만드는 화면은 리뷰어 지정 하나뿐이고
나머지는 기존 버튼 연결이다.**
