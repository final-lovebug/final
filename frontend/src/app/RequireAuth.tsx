import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../shared/stores/authStore'

// Phase 3에서 남겨둔 인증 가드. `isAuthenticated`는 실 로그인 흐름(OAuth 콜백,
// useSessionBootstrap의 /api/auth/refresh)이 채우는 실제 상태다 — 주석이
// "목업 상태만 확인"이라 돼 있던 건 낡은 서술이라 정정한다(T-INT-15, 2026-09-14).
// 토큰 만료 시 재발급은 shared/api/httpClient.ts가 401(AUTH_TOKEN_EXPIRED) 응답마다
// 처리한다.
export function RequireAuth() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
