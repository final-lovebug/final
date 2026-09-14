# FE↔BE 통합 1차 보고서 — 트랙 A 진행 결과

작성일: 2026-09-14(최초) · 갱신: 2026-09-14(T-INT-18 설계 확정 반영) · 대상: 팀 전체
(백엔드·프론트) · 근거: `docs/plan/INTEGRATION_PLAN.md`, `docs/task/` 전체

## 왜 이 문서인가

`docs/plan/INTEGRATION_PLAN.md`의 트랙 A(프론트 목업 → 실 API 연동, T-INT-9~16)를
실제로 진행하면서, **단순히 목업을 `httpClient` 호출로 바꾸는 것을 넘어서는 문제**가
다수 발견됐다. 크게 세 종류다.

1. 백엔드에 **없는 조회 수단**(id로만 조회 가능하고 "이 워크스페이스 기준으로 찾기"가
   없음) — 여러 도메인이 같은 패턴을 반복
2. 백엔드 도메인 모델과 프론트 화면 모델 사이의 **구조적 불일치**(단일 API 연동으로
   못 고침, 설계 재검토 필요)
3. 프론트 화면 자체가 **아직 데이터 기반으로 안 짜여 있어서** 실 데이터를 연결해도
   의미가 없는 지점

각 항목은 `docs/task/T-INT-*.md`에 더 자세한 근거·코드 위치가 있다.

---

## 해결해야 할 문제 — 현황판

| # | 문제 | 상태 | 결정된 내용 / 남은 결정 | 후속 태스크 |
|---|---|---|---|---|
| A | 회원 이름/이메일을 id로 조회할 방법이 없다 | ✅ **해결 완료**(2026-09-14) | `GET /api/members/{id}`(단건) + `GET /api/members?ids=`(배치) 구현·테스트·문서화 완료 | `T-INT-18` |
| B | 워크스페이스→"진행 중 사전 초안" lookup이 없다 | ✅ **해결 완료** | `GET /api/draft-dictionaries?workspaceId=&status=` 구현·테스트·문서화 완료(`D-64`) | `T-INT-20` |
| C | review 도메인 — 목록 N+1·코멘트 흐름·개정 diff | ✅ **백엔드 완료**(`D-63`) | 코멘트-검토 결합은 **오진단이었음**(백엔드 변경 불필요). 목록 N+1은 `targetId`/`reviewerCount` 추가로 해결. "삭제" diff는 안 만들기로 확정. **프론트(리뷰 작성 화면 등)만 남음** | `T-INT-12` |
| D | `DocumentReviewPage` 본문 렌더링이 특정 문서 2개에 하드코딩 | 🟡 결정 필요 | 재작업 필요하다는 것만 확인, 구체 방식(anchor 좌표 기반 재작업 범위 등) 미정 | `T-INT-10`(보류분) |
| E | 사전집 개정 이력(RevisionLog) 조회 API 미착수 | 🟡 결정 필요(우선순위) | phase-2 착수 여부/시점 미정 | `T-INT-11`(보류분) |
| F | 알림 "설정" API가 없다 | ⚪ 결정 불요 | `D-54`로 이미 의도적 제외된 설계 — 액션 없음 | — |

🟢 결정 완료(착수만 하면 됨) · 🟡 팀 논의로 방향을 정해야 함 · ⚪ 참고용, 액션 불필요

---

## 진행 결과 요약(트랙 A)

| 태스크 | 상태 |
|---|---|
| T-INT-9 workspace | ✅ 완료(QA 대기) |
| T-INT-10 document | 🟡 5/10 완료, 나머지 4개는 **D** 때문에 보류 |
| T-INT-11 dictionary | 🟡 범위 축소 완료(후보어 3개는 **B**, 개정 이력 1개는 **E** 때문에 보류) |
| T-INT-12 review | 🟢 백엔드 완료, 프론트 대기(**C**) |
| T-INT-13 notification | 🔴 전체 보류(**F** — 액션 불필요) |
| T-INT-14 member | ✅ **완료**(2026-09-14, `T-INT-18` 완료 후 재개해 마무리) |
| T-INT-15 auth guard | ✅ 완료(주석 정정만) |
| T-INT-16 env 설정 | ✅ 완료 |

코드 변경 27개 파일, `tsc -b`/`oxlint` 통과. **커밋·PR은 아직 안 만들었다**(사용자
지시 대기).

---

## A. [✅ 해결 완료] 회원 이름/이메일을 id로 조회할 방법이 없다

**영향 도메인 5개(코드로 확인 완료)**:

| 도메인 | 위치 | 필드 |
|---|---|---|
| workspace | `ParticipantResponse.java:8` | `memberId` |
| document | `DocumentResponse.java:17`, `DocumentSummaryResponse.java:18` | `uploaderId` |
| document(버전) | `DocumentVersionResponse.java:12`, `DocumentVersionSummaryResponse.java:7` | `publishedBy` |
| dictionary | `GET /api/workspaces/{id}/dictionary` 응답(`docs/API.md:862`) | `publishedBy` |
| draftdictionary | `CandidateTermResponse.java:19,38` | `handledBy` |
| reviewrequest | `ReviewRequestResponse.java:14,28`, `ReviewerResponse` | `requesterId`, `memberId` |

`Member API`가 `GET /api/members/me`(본인 전용)뿐이라 위 어떤 id도 이름으로 바꿀
방법이 없었다.

### 해결 내용(2026-09-14 구현 완료)

도메인별로 각자 `member`를 조인하는 대신 **공용 조회 API**를 신설했다.

| Method | Path | 용도 |
|---|---|---|
| GET | `/api/members/{memberId}` | 단건 조회 |
| GET | `/api/members?ids=1,2,3` | 배치 조회(존재하지 않는 id는 조용히 빠짐) |

응답: `{ memberId, displayName, email }` — `status`/`role`은 타인에게 공개할 이유가
없어 제외. 접근 범위는 **로그인한 회원이면 누구나 조회 가능**(스코프 제한 없음)으로
확정.

**구현하며 알게 된 것**: 정확히 이 용도의 내부 포트 `MemberDirectory`
(`getSummary`/`getSummaries`)가 이미 존재했지만 소비자가 0곳이었다 — 그 위에
REST 엔드포인트만 얹는 것으로 끝나 예상보다 작업이 가벼웠다. `docs/plan/
CONFLICTS.md`에 `D-62`로 등재, `docs/API.md` Member API 절에 문서화, 테스트 4건
추가 완료.

→ **`T-INT-18`**(`docs/task/T-INT-18-participant-member-info.md`) — 완료 후 각
태스크 재개 방법도 그 문서에 표로 정리돼 있다:
- **T-INT-14**: ✅ 완료(participants 목록 + 배치 조회 join)
- **T-INT-10**: 이미 끝난 작업의 보강(`uploaderId` → 이름 교체)
- **T-INT-11**: 영향 없음(현재 화면이 `publishedBy`를 안 씀)
- **T-INT-12**: A와는 무관하게 별도로 범위 확정됨(C 참고) — 백엔드 목록 응답 보강 +
  프론트 리뷰 작성 화면 신설

---

## B. [🟢 설계 확정] 워크스페이스 기준 "진행 중 사전 초안" 조회가 없다

**영향**: 사전 초안 교정 화면(후보어 목록/등록/수정)

`DraftDictionary API`는 `GET /api/draft-dictionaries/{draftDictionaryId}`처럼 **id로만**
조회 가능하다. "이 워크스페이스의 현재 진행 중인 사전 초안"을 찾는 목록/조회
엔드포인트가 없어, workspaceId만 갖고 시작하는 교정 화면이 후보어 목록에 접근할
방법이 없다.

**결정(2026-09-14)**: `GET /api/draft-dictionaries?workspaceId=&status=` 목록
엔드포인트 신설(`DraftDocument`의 `GET /api/draft-documents?documentId=&status=`와
동일 패턴)로 확정.

→ **`T-INT-20`**(`docs/task/T-INT-20-draft-dictionary-lookup.md`) — ✅ 완료

---

## C. [🟢 대부분 해결] review(리뷰 요청) 도메인

**영향**: 리뷰 요청 목록, 사전집 개정안 화면, 리뷰 코멘트 전체

백엔드 소스를 직접 확인해 구조적 문제로 봤던 것 중 하나는 **오진단**이었다.

1. **목록 응답에 `documentId`·`reviewerCount`가 없다** — `ReviewRequestResponse`는
   `{reviewRequestId, workspaceId, type, title, description, requesterId, status,
   approvedAt, revisedAt, createdAt, updatedAt}`뿐. **결정**: 백엔드가
   `RevisionDocument`/`RevisionDictionary`·`Reviewer` 조인·집계로 이 필드들을
   직접 추가한다(프론트 N+1 방지).
2. **사전 개정안 "삭제" 행 판별 불가** — 이전 사전집 버전과 비교해야 하는데 그 API가
   없다. **사용자 결정(2026-09-14): "삭제" 분류 없이 진행** — "추가"·"정의수정"만
   표시한다.
3. ~~**코멘트는 검토(verdict) 제출 후에만 가능하다**~~ — **오진단이었다.**
   `POST /api/review-requests/{id}/reviews`가 원래 `{verdict, comments[]}`를
   한 번에 받는 구조였다(`ReviewService.submit()`). 실제 의도된 흐름: 리뷰어가
   화면에서 단어별(`targetItemId`=후보어 id)·위치별(`anchor`) 코멘트를 작성하는
   동안은 **프론트 로컬 상태**에만 쌓아 두고, approve/change request를 고르는
   순간 한 번에 제출한다 — GitHub의 "pending review"와 같은 패턴을 프론트가
   맡으면 되는 것이었다. **백엔드 변경 불필요**, 프론트에 리뷰 작성 화면(코멘트
   컴포저+로컬 draft+제출)만 새로 만들면 된다.

`D-63`(`docs/plan/CONFLICTS.md`)로 등재 완료.

→ `docs/task/T-INT-12-review.md`(생명주기 설명 + 결정 사항 전문)

---

## D. [🟡 결정 필요] 문서 대조 제안 화면이 특정 문서에 하드코딩돼 있다

**영향**: 문서 검토 화면(`DocumentReviewPage.tsx`)의 본문 하이라이트, 초안 사이드바

`DocumentReviewPage.tsx`가 문서 본문 위에 제안(치환 후보)을 표시하는 로직이
**`doc-plan`/`doc-retention` 두 목업 문서 id에 대해서만 하드코딩된 문단 조각
(`SUGGESTION_PARAGRAPHS`)을 그대로 쓴다.** `SuggestionTerm.anchor`도 지금은 전부
`{start:0, end:0}` 자리표시자다. 관련 api 4개(`fetchDraftDocuments`·
`fetchSuggestions`·`fetchSuggestionHistory`·`resolveSuggestion`)를 실 API로 바꿔도,
**그 데이터를 문서 본문 위에 표시하는 로직 자체가 데이터 기반이 아니라서** 실제
문서에서는 화면이 깨지거나 아무것도 안 보인다. (id 체인 자체는 문제 없음 —
`GET /api/draft-documents?documentId=`로 조회 가능 확인됨.)

**남은 결정**: `DocumentReviewPage.tsx`의 본문·anchor 렌더링을 실제 좌표 기반으로
다시 짜는 방식·범위가 아직 안 정해짐(별도 세션 권장, 프론트 단독 작업으로 보임).

→ `docs/task/T-INT-10-document.md`("대조 제안 클러스터" 절)

---

## E. [🟡 결정 필요(우선순위)] 사전집 개정 이력(RevisionLog) 조회 API 미착수

**영향**: 사전집 개정 이력 타임라인 화면

`revisionlog` 도메인은 `domain`/`infra`(엔티티·리포지토리)만 있고 서비스·컨트롤러·
조회 API가 아직 없다(phase-1은 모델만, `WLSH-159`). **막힌 게 아니라 단순히 아직
착수 안 한 것** — phase-2(조회 API)를 언제 넣을지 우선순위만 정하면 된다.

→ `docs/task/T-INT-11-dictionary.md`

---

## F. [⚪ 결정 불요] 알림 "설정" API는 의도적으로 없다

알림 채널/매트릭스 설정 기능은 `D-54` 결정으로 MVP1에서 완전히 제거됐다(MVP2로
연기, 버그나 누락이 아니라 의도된 설계). 목업 화면(`fetchNotificationSettings`)은
그대로 두고 건드리지 않았다. 참고로 알림 **목록/안읽음수/읽음처리** API는 실제로
있는데 프론트 연동 코드가 없다 — 필요해지면 별도 태스크로 새로 정의(지금 범위보다
크다).

---

## 그 외 자잘한 응답 스키마 차이 (이미 반영 완료, 참고용)

실연동하며 부딪힌 필드 차이. 코드에는 이미 반영(옵셔널 처리 + 화면에서 "—" 표시)
했지만, 백엔드가 나중에 필드를 보강할 수 있는 후보로 기록해 둔다. **결정이 필요한
문제는 아니다.**

| 도메인 | 없는 필드 | 비고 |
|---|---|---|
| Workspace | `updatedAt` | `createdAt`만 있음. 목록 화면 "최근 수정"을 "생성일"로 대체 |
| Dictionary | 사전집 자체의 `id`/`name`, 용어의 `definition` | `definition`은 `D-41`로 **의도적** 제외 |
| Document | 작성자/수정자 이름(A 참고), 라벨이 배열(최대 5개)인데 화면은 1개만 표시 | |
| Document 버전 | 목록엔 본문(`body`) 없음(단건 조회에만 있음) | |

---

## 다음 단계 제안

1. ~~`T-INT-18` 착수~~ — ✅ **완료**(2026-09-14). ~~`T-INT-14`~~ — ✅ **완료**(이어서 진행).
2. ~~`T-INT-20`(B)~~ — ✅ **완료**(2026-09-14, 백엔드 구현까지 끝남).
3. ~~review 도메인(C)~~ — ✅ **백엔드 완료**(2026-09-14, `targetId`/`reviewerCount`
   추가 + "삭제" 없이 진행 확정). **남은 건 프론트뿐** — 리뷰 요청 목록 연동 +
   리뷰 작성 화면(코멘트 컴포저+제출) 신규.
4. **`DocumentReviewPage` 렌더링(D)** — 별도 세션으로 범위 정하기.
5. **RevisionLog(E)** — 착수 시점만 우선순위 논의.
6. 위가 정리되는 대로 트랙 A 잔여(T-INT-10 나머지 4개, T-INT-11 후보어, T-INT-12,
   T-INT-20)마저 진행.
7. 완료된 부분(T-INT-9·11(부분)·10(부분)·14·15·16·18)은 로컬 백엔드로 QA 후 커밋/PR —
   사용자 지시 시 진행.

## 참고

- 결정·설계 근거: `docs/plan/INTEGRATION_PLAN.md`
- 실행 체크리스트 전체: `docs/task/README.md`
- 이 보고서가 인용한 개별 태스크: `T-INT-9`~`T-INT-18`, `T-INT-20`(모두 `docs/task/`)
