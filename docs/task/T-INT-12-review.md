# T-INT-12 — review(리뷰 요청) 실연동

상태: **보류(2026-09-14, 구조적 재설계 필요 — 아래 참고)** | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A · 2026-09-14 사용자 결정
의존: 없음(다른 도메인 태스크와 독립)

`frontend/src/features/review/**`가 전부 목업이다. 백엔드 `reviewrequest` 도메인
(`ReviewRequest`·`Revision*`·`Reviewer`·코멘트)에 대응한다.

## 보류 사유(2026-09-14, 백엔드 소스 확인 완료)

단순 필드 매핑 문제가 아니라 **프론트 화면 모델과 백엔드 도메인 모델 사이의 구조적
차이 3가지**가 나왔다. 백엔드 컨트롤러/DTO 소스
(`backend/src/main/java/com/ubidict/backend/reviewrequest/presentation/`) 직접 확인 결과:

1. **목록 응답에 `documentId`·`reviewerCount`가 없다.** `ReviewRequestResponse`
   (`ReviewRequestResponse.java`)는 `{reviewRequestId, workspaceId, type, title,
   description, requesterId, status, approvedAt, revisedAt, createdAt, updatedAt}`뿐이다.
   `DocumentReviewRequestListPage`가 필요로 하는 `documentId`는
   `GET .../revision-documents`(→`RevisionResponse.targetId`)를, `reviewerCount`는
   `GET .../reviewers`를 **항목마다 추가 호출**해야 얻을 수 있다(N+1).
2. **사전 개정안 "추가/정의수정/삭제" 행에 대응하는 diff API가 없다.** 후보어 목록
   (`origin=EXTRACTED`→신규, `EXISTING`+수정→정의수정)까지는 만들 수 있어도, **"삭제"
   (이전 버전엔 있었는데 이번 후보 목록에 없는 용어)는 이전 사전집 버전과의 비교가
   있어야 판별 가능**한데 그런 API가 없다.
3. **코멘트 작성 엔드포인트(`POST /api/reviews/{reviewId}/comments`)의 `reviewId`는
   `Review`(리뷰어의 검토/verdict 제출 기록) 단위다.** 즉 **누군가 먼저 검토를 제출해야
   코멘트를 달 자리(`reviewId`)가 생긴다.** 그런데 프론트에는 검토 제출(Approve/Change
   request) UI 자체가 없다 — `DictionaryRevisionPage.tsx`·`DocumentReviewThreadPage.tsx`
   둘 다 그 버튼이 이미 `onClick` 없이 미연결 상태였다(코드 주석에도 "Approve/Change
   request 액션은 백엔드가 없어 아직 붙이지 않았다"고 적혀 있었음). 코멘트 스레드
   화면 자체가 "검토 제출 뒤에 코멘트를 단다"는 모델을 전제하지 않고 만들어졌다.

이 셋은 백엔드 보강 하나로 끝나지 않고 **프론트 화면 흐름 자체를 검토(제출 → 코멘트,
또는 코멘트를 먼저 달 수 있는 별도 스레드 개념 도입)해야 하는 재설계**에 가깝다.
사용자 결정: **이번 트랙 A에서는 review 도메인 전체를 보류하고 별도 세션에서
설계부터 다시 잡는다.**

## 체크리스트(재설계 세션에서 다시 씀 — 지금은 진행하지 않음)

## 체크리스트 — api 파일별

- [ ] `api/fetchDocumentReviewRequests.ts` (리뷰 요청 목록)
- [ ] `api/requestDocumentReview.ts` (리뷰 요청 생성 — `T-INT-5`가 만든
      `POST /api/draft-documents/{id}/review-request` 등 진입점 사용)
- [ ] `api/fetchDictionaryRevision.ts` (사전 개정안 조회)
- [ ] `api/fetchReviewThreadComments.ts`
- [ ] `api/addReviewThreadComment.ts`
- [ ] `api/addRevisionComment.ts`

## 나머지

- [ ] `model/types.ts` — 실제 API 응답에 맞춰 조정
- [ ] `model/fixtures.ts`·`model/reviewRequestFixtures.ts` — 화면이 더 이상 참조하지
      않으면 그대로 둬도 무방
- [ ] 화면 확인: 리뷰 요청 목록(`documents/reviews`), 리뷰 스레드
      (`documents/:id/review`, `.../review/:reviewId`), 사전 개정안(`dictionary/
      revisions/:revisionId`)에서 실 데이터 표시 QA
- [ ] 승인/변경요청/코멘트 작성이 실제로 백엔드에 반영되는지 확인(정족수 판정 등
      `RuleSet` 관련 화면 있으면 함께 확인)
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-review-real-api`, PR 생성
