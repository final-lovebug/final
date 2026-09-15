# T-INT-21 — 프론트엔드 디자인 정합(`ui/` 기준) + 잔여 API 연동

상태: **코드 작성 완료(QA 대기, 2026-09-14)** | 담당자: (세션 진행)
근거: 사용자 지시("frontend를 ui/ 의 디자인에 맞게 전부 수정해. 수정하면서 연동해야할
API가 있으면 연동해")
의존: 트랙 A 전체 + `T-INT-17`(완료)

`ui/`는 Claude Design 핸드오프를 바닐라 HTML/CSS/JS로 구현한 **디자인 원본**이다. React
구현은 거기서 옮겨 왔지만 화면마다 옮겨진 정도가 달랐다 — 토큰(`index.css @theme`)은 처음부터
정확했는데, `ui/style.css`의 **컴포넌트 클래스**(`.dtable`·`.toolbar`·`.banner`·
`.timeline-item`·`.comment-card`…)는 각 화면이 손으로 다시 쓰고 있어 여백·구분선·글자 크기가
조금씩 어긋났다. 프로토타입에 있던 상호작용 일부도 빠져 있었다.

## 1. `shared/ui`에 컴포넌트 클래스를 이식했다

화면마다 표 마크업을 다시 쓰는 대신 한 벌로 모았다. 매핑표는
`frontend/docs/ARCHITECTURE.md` 「디자인 시스템 컴포넌트 매핑」에 있다.

- [x] `DataTable`·`Th`·`Td`·`Tr` (`table.dtable`) — 목록 6개 화면이 공유
- [x] `Toolbar`·`ToolbarSpacer`·`FilterChip`·`Segmented` (`.toolbar` 계열)
- [x] `Banner` (`.banner-accent`·`.banner-neutral`)
- [x] `FieldLabel`·`TextInput`·`TextArea` (`.field-label`·`.input-box`·`.req-mark`)
- [x] `Checkbox`·`RadioDot`·`Toggle` — **프로토타입의 정적 div를 실제 컨트롤로.**
      `onChange`를 받으면 버튼, 없으면 읽기 전용 표시다(서버가 값을 정하는 자리와 사용자가
      바꾸는 자리를 같은 컴포넌트로 쓰기 위함)
- [x] `ScreenTitle`·`ScreenSubtitle`·`TwoCol`·`ColFlex`·`DetailPanel`·`QuoteBlock`
- [x] `TabRow` (`.tab-row`·`.settings-tabs`)
- [x] `Modal` (`.modal-overlay`·`.modal-box`) — 프로토타입에 없던 ESC·오버레이 닫기 추가
- [x] `PrThread`·`CommentCard` (`.pr-thread`·`.comment-card`)
- [x] `TimelineItem` (`.timeline-item`)
- [x] `LogoMark` (`.logo-mark`)

## 2. 화면별 정합

- [x] **앱 셸** — 사이드바 접이식 그룹(chevron) 복원, 머리말의 워크스페이스 이름과 발치의
      사용자·권한을 실 데이터로(하드코딩 "워크스페이스"·"민뱅"·"OWNER" 제거).
      탑바의 「사전집 r7」→ 활성 사전집 버전, 아바타 3개 → 실제 참여자.
      **로그아웃·회원 탈퇴는 사이드바 사용자 메뉴로 이동**(`D-85`)
- [x] **로그인 / 닉네임 온보딩** — `ui`의 login-card 규격(44px 로고, social-btn)
- [x] **워크스페이스 선택** — topbar + 1040px 본문 + 3열 카드(사전집 배지·멤버/문서 수).
      목록 API가 그 셋을 안 주므로 워크스페이스마다 세 조회를 더해 채운다
      (`fetchWorkspaceOverviews`)
- [x] **문서 목록** — toolbar(목록/카드 전환·라벨 필터·검색) + `.dtable`. 셋 다 실제로 동작
- [x] **문서 업로드** — 드롭존이 실제로 파일을 읽고(.md/.txt) 본문·문서명을 채운다.
      라벨 칩 선택 + 새 이름 입력이 생성 요청의 `labels`로 나간다
- [x] **문서 상세** — 본문 + 속성 패널(320px) 2단. 「최신 사전집으로 갱신」을 실제 대조 작업
      접수로 연결(작업 id를 쿼리로 넘겨 검토 화면이 폴링을 이어받는다)
- [x] **문서 버전 이력** — 타임라인 + 버전 비교. **본문 diff를 화면에서 계산한다**(`D-84`)
- [x] **문서 검토** — toolbar·doc-body·popover·tab-row를 `ui` 규격으로. 로직은 `T-INT-17` 그대로
- [x] **사전집** — 버전 드롭다운을 실제 버전 목록으로(보관 버전 열람 가능),
      「승인 대기 중인 변경 N건」 배너를 진행 중 개정안에서 계산
- [x] **사전집 초안** — master-detail(420px), 「직접 입력」 표준어 옵션, 유형·검색 필터,
      일괄 판정, **「개정안 제출」을 실 API로**(교정완료 → 리뷰 요청)
- [x] **사전집 리비전 이력** — 목업 타임라인·diff를 **버전 목록 + 버전별 용어**로 대체(`D-84`)
- [x] **문서/사전집 개정안 리뷰** — `Toolbar`·`TwoCol`·`PrThread`·`CommentCard`로 정리
- [x] **알림 패널** — 400px, 미읽음 점 표시, 읽은 항목 흐리게, 「인앱」 배지, 발치 안내 + 설정 링크
- [x] **설정 4탭** — 멤버(제외·역할 변경 실연동), 승인 규칙(정족수 상한을 실제 참여자 수로),
      알림(읽기 전용), 라벨(생성 목업 제거)
- [x] **초안·개정안 목록**(프로토타입에 없던 화면) — 다른 목록과 같은 `.dtable` 규격으로

## 3. 새로 연동한 API

- [x] `GET /api/workspaces/{id}` — 사이드바 이름·내 권한
- [x] `GET /api/workspaces/{id}/participants` (+ `GET /api/members?ids=`) — 탑바 아바타,
      멤버 표의 `participantId`
- [x] `DELETE /api/workspaces/{id}/participants/{participantId}` — 멤버 제외
- [x] `PATCH /api/workspaces/{id}/participants/{participantId}/permission` — 역할 변경
- [x] `GET /api/workspaces/{id}/dictionary/versions` — 버전 드롭다운·리비전 타임라인
- [x] `GET /api/workspaces/{id}/dictionary/versions/{versionNo}` — 보관 버전 열람 + 용어 diff
- [x] `GET /api/workspaces/{id}/documents/{documentId}/versions/{versionNo}` — 본문 비교
- [x] `POST /api/draft-dictionaries/{id}/examine-completion` +
      `POST /api/draft-dictionaries/{id}/review-request` — 「개정안 제출」
- [x] `GET /api/draft-dictionaries/{id}/examine-progress` — 제출 가능 여부·미판정 건수
- [x] `POST /api/workspaces/{id}/documents`의 `labels` — 라벨 생성 경로

## 4. 걷어낸 목업

- [x] `fetchDictionaryRevisionTimeline` + `useDictionaryRevisionTimeline` +
      `dictionary/model/versionFixtures.ts` — 실 버전 API로 대체
- [x] `fetchNotificationSettings`·`updateNotificationSettings` + 두 훅 — 대응 API가 없어
      화면을 읽기 전용으로 바꾸고 제거(`D-82`)
- [x] `createLabel` + `useCreateLabel` — 독립 라벨 생성 API가 없다
- [x] `document/model/suggestionFixtures.ts`(`T-INT-17`에서), `document/model/versionFixtures.ts`,
      `workspace/model/fixtures.ts`, `pages/PagePlaceholder.tsx`
- [x] 죽은 픽스처 상수 11개(`DOCUMENT_FIXTURES`·`TERM_FIXTURES`·`WORKSPACE_FIXTURES` …) —
      `model/fixtures.ts`에는 **뷰 타입과 고정 정책 상수만** 남았다
- [x] `shared/lib/delay.ts` 참조 0곳

## 5. 바로잡은 것

- [x] **`docs/API.md`의 활성 사전집·버전 목록 응답 예시가 실제 DTO와 달랐다** —
      `dictionaryId`·`workspaceId` 누락, `publishedBy` → 실제는 `createdBy`
      (`CONFLICTS.md` `Y-34`, `T-INT-17`에서 발견해 함께 수정)
- [x] **용어 추출 라우트를 워크스페이스 스코프로 이동**(`D-83`) —
      `/documents/:documentId/extract` → `/dictionary/extract`
- [x] `frontend/CLAUDE.md`의 「백엔드 연동 상태(2026-09-10)」가 통째로 낡아 있었다
      (문서·사전집·리뷰·알림에 "API 자체가 없다", "백엔드에 CORS 설정이 아직 없다") — 갱신

## 검증

- [x] `npx tsc -b --force` 통과
- [x] `npm run build` 통과
- [x] `npm run lint` — 새 경고 0건(남은 1건은 `OAuthCallbackPage`의 기존 패턴)
- [ ] 수동 QA — **로컬 백엔드 기동 후 사용자가 직접 확인 필요.** 추출·대조의 실제 결과는
      FastAPI 워커가 있어야 보인다(이 저장소 밖)
- [ ] 커밋·PR — **사용자 지시 시 진행**
