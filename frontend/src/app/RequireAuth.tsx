import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../shared/stores/authStore'

// Phase 3에서 남겨둔 인증 가드를 여기서 채운다. 지금은 Zustand 목업 상태만 확인하고,
// 실제 토큰 검증(만료/재발급 등, docs/API.md 참고)은 백엔드 연동 시 추가한다.
export function RequireAuth() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
