# T-INT-9 — workspace 실연동

상태: **코드 작성 완료, 커밋 전 QA 대기** | 담당자: (세션 진행, 2026-09-14)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A
의존: 없음(다른 도메인 태스크와 독립)

`frontend/src/features/workspace/**`가 전부 목업이다. `docs/API.md`의 Workspace API
절을 정본으로 삼아 `httpClient` 호출로 교체한다.

## 체크리스트

- [x] `features/workspace/api/fetchWorkspaces.ts` — `httpClient.get('/api/workspaces')`로
      교체(배열 그대로, 페이징 없음)
- [x] `features/workspace/api/fetchRuleSet.ts` — **별도 룰셋 엔드포인트가 없어서
      `GET /api/workspaces/{workspaceId}`(워크스페이스 상세)를 호출해 두 카운트만
      추려내는 방식으로 구현.** 참여자가 아니면 404가 예외로 던져진다(목업의
      `undefined` 반환과 다름 — 호출 쪽에서 에러 처리 확인 필요)
- [x] `features/workspace/api/updateRuleSet.ts` — `httpClient.patch('/api/workspaces/
      {workspaceId}/rule-set', input)`로 교체. **시그니처가 `(workspaceId, input)`으로
      바뀜**(목업은 workspaceId 없이 전역 fixture를 직접 변형했었다) —
      `hooks/useUpdateRuleSet.ts`에서 `mutationFn`을 래핑해 흡수, `SettingsRulesetPage.tsx`
      등 컴포넌트는 변경 없음
- [x] `features/workspace/model/types.ts` — `Workspace`에 `myPermission?` 추가,
      `createdBy?`/`updatedAt?`을 옵셔널로(실 API가 안 내려줌). `RuleSet`도
      `id?`/`createdAt?`/`createdBy?`/`updatedAt?` 옵셔널로(별도 엔티티가 아니라
      `WorkspaceResponse`에서 두 카운트만 옴)
- [x] **부수 발견 — `WorkspacesPage.tsx`가 컴파일 에러**: `workspace.updatedAt`을
      바로 `new Date()`에 넣고 있었는데 실 API는 이 필드가 없어 `string | undefined`가
      됨. `updatedAt`이 있으면 "최근 수정", 없으면 `createdAt`으로 "생성일" 표시하도록
      수정(작은 UI 문구 변경 — 사용자에게 묻지 않고 처리, 필요시 되돌리기 쉬움)
- [ ] `myPermission`이 실제로 내려오기 시작했지만 **권한 기반 UI 분기는 이번 범위에서
      하지 않음**(화면 컴포넌트를 최소로 건드리는 원칙 — 필요해지면 별도 태스크)
- [x] `features/workspace/model/fixtures.ts` — 그대로 둠(더 이상 참조하는 곳 없음)
- [x] `npx tsc -b` 통과, `npm run lint`(oxlint) 통과(기존에 있던 무관한 warning 3건 제외)
- [ ] 화면 확인: `/workspaces` 목록, `settings/ruleset` 화면에서 실제 백엔드 데이터
      표시 — **로컬 백엔드 기동 후 사용자가 직접 확인 필요**(이 세션은 백엔드를 띄우지
      않음)
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-workspace-real-api`, PR 생성 — **사용자 지시 시
      진행**(루트 CLAUDE.md git 금지 규칙)
