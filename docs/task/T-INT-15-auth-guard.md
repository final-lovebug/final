# T-INT-15 — 인증 가드(`RequireAuth`) 점검

상태: **완료(2026-09-14)** | 담당자: (세션 진행)
근거: `docs/plan/INTEGRATION_PLAN.md` 2절 Track A
의존: 없음

## 결과

예상대로 **코드는 이미 실제 인증 상태를 쓰고 있었고, 주석만 낡아 있었다.**
`RequireAuth.tsx`의 `isAuthenticated`는 실 로그인 흐름(OAuth 콜백 → `login()`,
`useSessionBootstrap`의 `/api/auth/refresh`)이 채우는 실제 Zustand 상태다 — "목업
상태만 확인"이라던 주석은 사실과 달랐다. 목업 요소(하드코딩된 조건 등)는 없었다.

## 체크리스트

- [x] `RequireAuth.tsx`가 실제로 `authStore.isAuthenticated`(실 상태)를 참조하는지 확인
      — 확인됨, 문제 없음
- [x] `router.tsx`의 "목업 인증 가드" 주석이 낡았는지 판단 — 낡음
- [x] `RequireAuth.tsx`·`router.tsx` 주석을 현재 상태에 맞게 정정
- [x] 목업 요소 없음 확인(별도 수정 불필요)
- [ ] 수동 QA: 로그아웃 상태로 보호된 라우트 직접 접근 시 `/login` 리다이렉트, 로그인 후
      정상 진입 — **사용자가 직접 확인 필요**(이 세션은 브라우저로 확인 불가)
- [ ] 커밋 브랜치 `chore/WLSH-{티켓}-fe-auth-guard-review`, PR — **사용자 지시 시 진행**
      (코드 변경은 주석 2줄뿐이라 다른 태스크 커밋에 묶어도 무방)
