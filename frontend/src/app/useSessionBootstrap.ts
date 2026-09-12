import { useEffect, useState } from 'react'
import { httpClient } from '../shared/api/httpClient'
import { fetchCurrentMember } from '../features/member/api/fetchCurrentMember'
import { useAuthStore } from '../shared/stores/authStore'

interface RefreshResponse {
  accessToken: string
}

// 페이지를 새로고침하면 accessToken은 메모리에서 사라지지만(authStore.ts — XSS 노출 방지를
// 위해 localStorage에 두지 않음), refresh token 쿠키(HttpOnly, 7일)는 그대로 남아있다.
// 앱이 처음 뜰 때 한 번 조용히 POST /api/auth/refresh를 시도해 세션을 복구한다 — 실패하면
// (쿠키가 없거나 만료) 그냥 비로그인 상태로 남겨 RequireAuth가 /login으로 보내게 둔다.
//
// 복구를 시도하는 동안 라우터를 그대로 렌더링하면 RequireAuth가 아직 isAuthenticated=false인
// 순간을 보고 /login으로 리다이렉트해버린 뒤 곧바로 다시 워크스페이스로 돌아오는 깜빡임이
// 생긴다 — 그래서 App이 이 훅의 결과를 보고 부트스트랩이 끝날 때까지 라우터 렌더링을 미룬다.
export function useSessionBootstrap(): boolean {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const login = useAuthStore((state) => state.login)
  const setCurrentMember = useAuthStore((state) => state.setCurrentMember)
  const [isBootstrapping, setIsBootstrapping] = useState(!isAuthenticated)

  useEffect(() => {
    if (isAuthenticated) {
      setIsBootstrapping(false)
      return
    }

    let cancelled = false

    void (async () => {
      try {
        const { accessToken } = await httpClient.post<RefreshResponse>('/api/auth/refresh')
        if (cancelled) return
        login(accessToken)
        const member = await fetchCurrentMember()
        if (cancelled) return
        setCurrentMember(member)
      } catch {
        // refresh token이 없거나 유효하지 않음 — 중간에 login()까지 갔을 수 있으니
        // 확실히 비로그인 상태로 되돌린다(인증됐지만 currentMember가 없는 상태 방지).
        if (!cancelled) useAuthStore.getState().logout()
      } finally {
        if (!cancelled) setIsBootstrapping(false)
      }
    })()

    return () => {
      cancelled = true
    }
    // 최초 마운트(= 페이지 로드) 시 한 번만 시도한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return isBootstrapping
}
