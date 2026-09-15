# 사전집 개정안 리뷰 화면 수정 계획

**2026-09-15 신설.** 사전집 개정안 검토 화면(`DictionaryRevisionPage`)의 리뷰 흐름을 고치는 단발 태스크 계획이다.
도메인 전체 계획이 아니므로 7개 도메인 계획 문서의 13절 목차를 따르지 않는다.

함께 읽는다 — `docs/plan/REVIEW_REQUEST_PLAN.md`(특히 `D-1` 집계 규칙), `docs/plan/CONFLICTS.md`, `docs/API.md`.

UI 미리보기: **`docs/mockups/dictionary-revision-review.html`** — 실제 디자인 토큰으로 그린 정적 파일이고
브라우저로 바로 열린다. 아래 6~8절의 화면 구성은 이 목업이 기준이다.

---

## 1. 배경

사전집 개정안 검토 화면(`DictionaryRevisionPage`)에 네 가지 문제가 있다. 조사해 보니 **두 개는 프론트 전용, 하나는 백엔드 누락, 하나는 이미 동작하지만 화면에 피드백이 없어 안 되는 것처럼 보이는 것**이었다.

| 요청 | 실제 원인 |
| --- | --- |
| ① 같은 사람이 여러 번 리뷰 → 마지막 리뷰로 최종 처리 | **백엔드는 이미 그렇게 한다**(`LatestReviewAggregator` = 회원별 최신 1건). 틀린 쪽은 UI다 — `ReviewerPanel`이 `review.targetRound === currentRound`로 걸러, 회차가 올라가면 유효한 승인이 「대기」로 보인다 |
| ② 전체 코멘트가 UI에 안 보임 | `Review`에 body 필드가 없고, 전체 코멘트는 `targetItemId == null`인 `Comment`로 저장된다(`submitVerdict`가 그렇게 보낸다). 그런데 화면은 `comment.targetItemId === selected.candidateTermId`로만 필터해 **전체 코멘트를 전부 버린다** |
| ③ 요청자 본인이 본인 리뷰 가능 | **아무 데도 검증이 없다.** 팝오버 하단에 "본인이 올린 요청은 본인이 승인할 수 없습니다"라고 적혀 있지만 문구뿐이고, `ReviewService.submit`도 `requesterId`를 보지 않는다 |
| ④ 코멘트 없으면 리뷰가 안 됨 | 코드상 **막는 곳이 없다** — `comments`는 DTO에서 nullable이고 `List.of()`로 정규화된다. 실제 증상은 **제출해도 화면에 아무 흔적이 없다**(②의 결과 + 지정 리뷰어가 아니면 `ReviewerPanel`에도 안 뜸)는 쪽이다. ②를 고치면 해소되며 회귀 테스트로 잠근다 |

### 1-1. 확정한 결정

- **집계는 회차 무관, 회원별 최신 리뷰 1건.** 백엔드 현재 동작이자 `REVIEW_REQUEST_PLAN.md` `D-1`과 일치한다. 화면을 백엔드에 맞춘다
- **회차(`targetRound`)를 화면에 노출하지 않는다.** 내부 정합용 값으로만 쓰고 배지·라벨로 찍지 않는다
- **답글(`parentId`) UI와 기능을 만들지 않는다.** 코멘트는 평면이다
- **탭을 두지 않는다.** 리뷰 목록은 **가운데 용어 표 아래에 전체 폭 섹션**으로 크게 두고(사람별 묶음), **우측은 리뷰어 패널 + 선택한 용어의 코멘트**가 항상 보인다
- **본인 리뷰는 제출만 차단**한다(403 + 버튼 비활성). 정족수 계산식과 기존 저장 데이터는 건드리지 않는다

### 1-2. 목업으로 확인할 수 있는 것

`docs/mockups/dictionary-revision-review.html`에서 동작하는 것 — 시점 토글(리뷰어/요청자), 용어 행 선택,
리뷰 섹션의 용어 Pill 클릭, 「대체된 이전 리뷰」 접기·펴기, 「리뷰 마무리」 팝오버.

---

## 2. 백엔드 — 요청자 본인 리뷰 차단

파일: `backend/src/main/java/com/ubidict/backend/reviewrequest/`

1. `exception/ReviewRequestErrorCode.java` — 추가
   ```java
   REVIEW_REQUEST_SELF_REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인이 올린 리뷰 요청은 본인이 검토할 수 없습니다."),
   ```
2. 기존 `implement/ApprovalAuthorityValidator`에 **`validateNotRequester(ReviewRequest, Long actorId)`를 추가**한다 — 이미 `validateRequesterOrAdmin`·`validateRevise`가 여기 있으므로 새 클래스를 만들지 않는다.
3. `service/ReviewService.submit(...)` — `validateParticipant` 직후, `validateReviewable` 앞에 호출
   ```java
   workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), command.memberId());
   approvalAuthorityValidator.validateNotRequester(reviewRequest, command.memberId());
   ```
4. `service/ReviewerService`의 지정 경로에도 같은 검증 — 요청자를 리뷰어로 지정할 수 없어야 화면(후보 제외)과 서버가 일치한다.

**집계는 건드리지 않는다.** `LatestReviewAggregator`·`ReviseEligibilityCalculator`·`ReviewRequestStatusPolicy` 그대로.

## 3. 백엔드 — 회귀 테스트

기존 `ReviewSubmitServiceTest`(이미 `submit_resubmitBySameMember`가 있다)에 이어 추가한다.

- `submit_byRequester_throwsForbidden` — 요청자가 제출하면 `REVIEW_REQUEST_SELF_REVIEW_NOT_ALLOWED`
- `submit_withoutComments_succeeds` — `comments`가 `null`일 때와 `[]`일 때 모두 201, 두 verdict 모두. **④를 잠그는 테스트**
- `aggregate_usesLatestReviewAcrossRounds` — 같은 회원이 `targetRound=0`에 `CHANGES_REQUESTED`, `targetRound=1`에 `APPROVED`를 내면 `changesRequestedCount == 0`
- 컨트롤러 테스트에 `comments` 필드 누락 케이스 하나
- `ReviewerService` 테스트에 요청자 지정 거절 케이스 하나

## 4. 프론트 — 공통 선택 로직

`frontend/src/features/review/model/reviewTimeline.ts` (신규, 순수 함수 + 단위 테스트)

```ts
/** 회원별 최신 리뷰 1건. 백엔드 LatestReviewAggregator.later() 와 같은 규칙 — submittedAt, 동률이면 id. */
export function latestReviewByMember(reviews: Review[]): Map<string, Review>

export interface ReviewEntry {
  review: Review
  summary?: Comment      // targetItemId·anchor 없는 코멘트 = 전체 코멘트
  itemComments: Comment[] // targetItemId(또는 anchor) 있는 코멘트
  superseded: boolean     // 같은 회원의 더 최신 리뷰가 있으면 true
}
/** 사람별로 묶고, 각 사람 안에서 최신 리뷰가 먼저. */
export function groupReviewsByMember(reviews, comments): { memberId: string; entries: ReviewEntry[] }[]

/** 선택한 용어에 달린 코멘트를 시간순으로. 각 항목에 그 리뷰의 verdict를 함께 준다. */
export function commentsForTerm(reviews, comments, candidateTermId): { comment: Comment; verdict: ReviewVerdict; memberId: string }[]
```

- **전체 코멘트 판별**: `targetItemId == null && anchor == null`. 여러 건이면 첫 건이 `summary`, 나머지는 `itemComments`에 대상 없이 남겨 유실을 막는다
- **`parentId`는 읽지 않는다** — 답글 개념을 쓰지 않는다. 기존 데이터에 `children`이 있어도 `flattenComments`로 펴서 평면으로 다룬다
- 코멘트→리뷰 연결은 `comment.reviewId`(`CommentResponse`가 준다)

## 5. 프론트 — 리뷰어 패널

`features/review/components/ReviewerPanel.tsx`

- `currentRound` prop과 `.filter((review) => review.targetRound === currentRound)` **제거**
- `latestReviewByMember(reviews)`로 판정을 정한다(4절 재사용). 회차 배지는 **붙이지 않는다**
- 클래스 주석의 「지난 회차의 검토는 재교정으로 무효」를 반대 내용으로 고친다(`D-1` 인용)
- `DictionaryRevisionPage`·`DocumentReviewThreadPage`에서 `currentRound` 전달 제거

## 6. 프론트 — 리뷰 섹션 (핵심, 가운데 표 아래)

`features/review/components/ReviewList.tsx` (신규)

`TwoCol`의 **좌측 `ColFlex` 안, 용어 표 카드 바로 아래**에 카드 하나로 놓는다. 목업(`docs/mockups/dictionary-revision-review.html`)의 `.card > .section-head + .reviewer-group*` 구조 그대로다.

**사람 하나당 묶음 하나이고, 묶음 본문이 곧 그 사람의 최신 리뷰다** — 최신 리뷰를 박스로 한 겹 더 감싸지 않는다.

- **섹션 헤더** — `리뷰` + `N건 · M명` Pill + 우측에 `최신 리뷰만 정족수에 셉니다`
- **묶음 헤더** — 28px 아바타 + 이름 + 최신 판정 Pill(`success`=승인 / `warn`=변경 요청 / `neutral`=대기) + 우측 상대시간
- **묶음 본문 = 최신 리뷰**
  - 전체 코멘트가 있으면 본문(`13px`), 없으면 `코멘트 없이 {판정}했습니다.`를 `text-text-quaternary` 이탤릭으로 — **④가 화면에 드러나는 자리**
  - 딸린 용어 코멘트는 구분선 아래 `용어 코멘트 N` 라벨 + `↳ [용어 Pill] 내용` 한 줄씩. **용어 Pill을 누르면 우측 코멘트 패널이 그 용어로 바뀐다**(표 선택과 같은 setter)
- **대체된 이전 리뷰는 기본으로 접는다** — `<details>` + pill 모양 `<summary>`(`▶ 대체된 이전 리뷰 N건`). 펼치면 점선 테두리 카드로 나오고, 각 카드는 판정 Pill + `대체됨` + 시간 + 전체 코멘트 + 용어 코멘트를 갖는다. `N === 0`이면 토글 자체를 렌더하지 않는다
- **리뷰 0건인 지정 리뷰어**도 묶음으로 보여 준다(`대기` Pill + `아직 리뷰를 제출하지 않았습니다.`)
- 회차는 **어디에도 표시하지 않는다**

## 7. 프론트 — 우측 용어 코멘트 (탭 없이 상시 표시)

기존 우측 코멘트 영역을 `commentsForTerm(...)` 기반으로 바꾼다(4절). 리뷰어 패널 아래에 그대로 둔다.

- 헤더: `{용어} · 코멘트 {n}`
- 카드마다 작성자 + **그 코멘트가 속한 리뷰의 판정 Pill** + 상대시간 → 어떤 판정 맥락에서 나온 말인지 보인다
- 시간 오름차순
- 미제출(pending) 코멘트는 점선 테두리 + `미제출`로 기존과 동일하게 유지
- 하단 작성 박스(`담기`)와 「리뷰 마무리 할 때 함께 제출됩니다」 안내 유지

## 8. 프론트 — 페이지 조립과 본인 리뷰 차단

`pages/review/DictionaryRevisionPage.tsx`

- `useReviews(reviewRequestId)`를 페이지에서도 사용(이미 `useSubmitReview.ts`에 있다)
- 레이아웃:
  ```
  <TwoCol>
    <ColFlex>          ← 가운데
      <Card> 용어 DataTable </Card>
      <ReviewList … />                       ← 6절, 새로 들어가는 자리
    </ColFlex>
    <PrThread>         ← 우측 (sticky)
      <ReviewerPanel … />
      선택 용어 코멘트 + 작성 박스             ← 7절
    </PrThread>
  </TwoCol>
  ```
- 선택 용어는 페이지 state 하나(`selectedTermId`)로 두고 **표 행 클릭과 리뷰 섹션의 용어 Pill 클릭이 같은 setter**를 부른다
- `shared/ui/Tabs.tsx`는 쓰지 않는다 — 탭 없는 구성이다
- `const isRequester = currentMember?.id === revision.requesterId`
  `<ReviewSubmitPopover disabled={isRequester} disabledReason="본인이 올린 요청은 본인이 검토할 수 없습니다" …/>` — **prop이 이미 있고 아무도 안 넘기고 있다**
- 팝오버 푸터의 안내 문구는 중복이므로 제거(버튼 `title`이 대신한다)
- `DocumentReviewThreadPage`의 `Approve`/`Change request`에도 같은 조건

## 9. 문서 갱신

- `docs/plan/CONFLICTS.md` — 새 결정을 `D-*`로: 본인 리뷰 차단(에러코드 포함), 리뷰 목록을 가운데 표 아래 사람별 묶음으로 두고 우측은 용어 코멘트 상시 표시, **답글 미구현 확정**, 회차 비노출
- `docs/plan/REVIEW_REQUEST_PLAN.md` 4절 — 제출 검증 목록에 요청자 차단 추가. `Comment.parentId`는 스키마에 남기되 미사용임을 명시
- `docs/API.md` — `POST /api/review-requests/{id}/reviews`의 403 사유와 `comments`가 선택임을 명시

---

## 10. 검증

```bash
# 백엔드
cd backend
./gradlew test --tests '*ReviewSubmitServiceTest' --tests '*ReviewerServiceTest' --tests '*ReviewControllerTest'
./gradlew spotlessApply && ./gradlew check

# 프론트
cd frontend && npm run lint && npm run build
```

수동 확인 (`SPRING_PROFILES_ACTIVE=local,dev`로 루트 compose 기동, `/api/auth/dev/login`으로 두 계정 토큰 확보):

1. A가 개정안 리뷰 요청 → A로 열면 **「리뷰 마무리」가 비활성**이고 툴팁이 뜬다. curl로 직접 POST 하면 403 `REVIEW_REQUEST_SELF_REVIEW_NOT_ALLOWED`
2. B가 **코멘트 없이 승인** → 201, 표 아래 리뷰 섹션의 B 묶음에 `코멘트 없이 승인했습니다.`가 즉시 나타난다
3. B가 전체 코멘트 + 용어별 코멘트 2건으로 변경 요청 → B 묶음 본문이 새 리뷰로 바뀌고 **이전 승인은 「대체된 이전 리뷰 1건」 토글 안으로 접힌다.** 상태는 `CHANGES_REQUESTED`
4. B가 다시 승인 → 토글이 `2건`이 되고, `review-progress`의 `approvedCount`가 리뷰어 패널 판정과 일치
5. 재교정으로 회차가 올라간 뒤에도 **리뷰어 패널이 「대기」로 되돌아가지 않는다**. 화면 어디에도 회차 배지가 없다
6. 용어 행을 바꿔 가며 → 우측 코멘트가 그 용어의 코멘트만 시간순으로, 각 카드에 판정 Pill과 함께 보인다. 리뷰 섹션의 용어 Pill을 눌러도 같은 전환이 일어난다

---

## 11. 건드리지 않는 것

- `LatestReviewAggregator` / `ReviseEligibilityCalculator` / `ReviewRequestStatusPolicy` — 집계 규칙은 이미 맞다
- `Review` 엔티티에 `body` 컬럼을 추가하지 않는다 — 전체 코멘트는 `targetItemId == null`인 `Comment`로 이미 저장되고 있다
- `Comment.parentId` 컬럼과 `CommentResponse.children` — 스키마·API는 두되 **화면에서 쓰지 않는다**
- Flyway 마이그레이션 없음
