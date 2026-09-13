# T-INT-14 — member 잔여 실연동

상태: **보류(2026-09-14, 백엔드 보강 필요 — 아래 참고)** | 담당자: (미정)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A · 2026-09-14 사용자 결정
의존: **T-INT-18**(백엔드: 참여자 응답에 이름/이메일 포함)

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

## 체크리스트(T-INT-18 완료 후 진행)

- [ ] `api/fetchWorkspaceMembers.ts` 구현 — 두 호출을 합친다:
      1. `GET /api/workspaces/{workspaceId}/participants`(`docs/API.md` 423행)로
         참여자 목록(`memberId`·`permission`·`joinedAt`) 조회
      2. 그 `memberId` 배열을 `GET /api/members?ids=1,2,3`(T-INT-18 산출물)에 넘겨
         이름/이메일 조회
      3. 두 결과를 `memberId` 기준으로 join해 `WorkspaceMemberListItem` 완성
      (`ParticipantResponse`의 정확한 필드는 `backend/.../workspace/presentation/
      dto/ParticipantResponse.java`로 이미 확인됨: `{participantId, workspaceId,
      memberId, permission, joinedAt}`)
- [ ] `model/fixtures.ts`의 `WORKSPACE_MEMBER_FIXTURES` — 화면이 더 이상 참조하지
      않으면 그대로 둬도 무방
- [ ] 화면 확인: `settings/members` 화면에서 실 참여자 목록·권한 표시 QA
- [ ] 커밋 브랜치 `feat/WLSH-{티켓}-fe-member-real-api`, PR 생성
