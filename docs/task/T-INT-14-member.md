# T-INT-14 — member 잔여 실연동

상태: **완료(2026-09-14)** | 담당자: (세션 진행)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A · 2026-09-14 사용자 결정
의존: **T-INT-18**(완료)

`member`는 `fetchCurrentMember`·`withdrawMember`가 이미 실연동돼 있고
`fetchWorkspaceMembers.ts` 하나만 목업이다.

## 보류 사유(2026-09-14 조사)

- `GET /api/workspaces/{workspaceId}/participants` 응답은 `{participantId, workspaceId,
  memberId, permission, joinedAt}`뿐이다(`backend/.../workspace/presentation/dto/
  ParticipantResponse.java` 확인) — **email/displayName이 없다.**
- Member API(`docs/API.md` 301행)는 `/api/members/me`(본인 전용)뿐, 다른 회원을 id로
  조회하는 엔드포인트가 없다.
- 즉 프론트가 참여자 목록 화면(`WorkspaceMemberListItem`의 `name`/`email`/`initial`)을
  채울 방법이 지금은 없다.
- 사용자 결정: **T-INT-14를 보류하고, 백엔드 보강을 `T-INT-18`로 새로 만들어 넘긴다.**

## 설계 확정(2026-09-14, `T-INT-18` 참고)

`T-INT-18`에서 **회원 배치 조회(`GET /api/members?ids=`) + 단건 조회
(`GET /api/members/{memberId}`) API 신설**로 확정됐다
(`docs/task/T-INT-18-participant-member-info.md`). 이 태스크는 그 완료 후 아래
순서로 재개한다.

## 체크리스트

- [x] `api/fetchWorkspaceMembers.ts` 구현 — 두 호출을 합침:
      1. `GET /api/workspaces/{workspaceId}/participants`로 참여자 목록
         (`memberId`·`permission`·`joinedAt`) 조회. 참여자가 0명이면 두 번째 호출
         없이 바로 빈 배열 반환
      2. 그 `memberId` 배열을 `GET /api/members?ids=1,2,3`(T-INT-18)에 넘겨
         이름/이메일 조회
      3. 두 결과를 `memberId` 기준으로 join. 배치 조회에서 빠진 회원(탈퇴 등)은
         이름/이메일을 "—"로 표시
- [x] `model/fixtures.ts`의 `WORKSPACE_MEMBER_FIXTURES` — 더 이상 참조하는 곳
      없어졌지만 그대로 둠(관례상 억지로 안 지움)
- [x] 부수 수정: `SettingsMembersPage.tsx`의 `joinedAt` 표시를
      `new Date(...).toLocaleDateString('ko-KR')`로 포맷(실 API가 전체
      ISO 타임스탬프를 주므로 그대로 찍으면 지저분해 보임)
- [x] `npx tsc -b`·`npm run lint` 통과
- [ ] 화면 확인: `settings/members` 화면에서 실 참여자 목록·권한 표시 QA —
      **로컬 백엔드 기동 후 사용자가 직접 확인 필요**
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-member-real-api`, PR — **사용자 지시 시 진행**
