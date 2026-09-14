import { useEffect, useRef, useState } from 'react'
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
//
// hasStartedRef로 실제 요청을 1회로 제한한다 — React 18 StrictMode(개발 모드)는 effect를
// mount→cleanup→remount로 두 번 실행하는데, 이전엔 cancelled 플래그로 "상태 갱신"만 막고
// 네트워크 호출 자체는 두 번 나가고 있었다. refresh token은 재발급마다 rotation되고 동시
// 요청 두 개가 각각 다른 새 토큰을 발급받다 보니, 브라우저에 최종 저장된 쿠키와 서버의
// current/grace 상태가 어긋나 "새로고침을 반복하면 로그아웃되는" 버그로 이어졌었다
// (OAuthCallbackPage.tsx의 requestedRef와 동일한 이유·패턴).
export function useSessionBootstrap(): boolean {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const login = useAuthStore((state) => state.login)
  const setCurrentMember = useAuthStore((state) => state.setCurrentMember)
  const [isBootstrapping, setIsBootstrapping] = useState(!isAuthenticated)
  const hasStartedRef = useRef(false)

  useEffect(() => {
    if (isAuthenticated) {
      setIsBootstrapping(false)
      return
    }
    if (hasStartedRef.current) return
    hasStartedRef.current = true

    void (async () => {
      try {
        const { accessToken } = await httpClient.post<RefreshResponse>('/api/auth/refresh')
        login(accessToken)
        const member = await fetchCurrentMember()
        setCurrentMember(member)
      } catch {
        // refresh token이 없거나 유효하지 않음 — 중간에 login()까지 갔을 수 있으니
        // 확실히 비로그인 상태로 되돌린다(인증됐지만 currentMember가 없는 상태 방지).
        useAuthStore.getState().logout()
      } finally {
        setIsBootstrapping(false)
      }
    })()
    // 최초 마운트(= 페이지 로드) 시 한 번만 시도한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return isBootstrapping
}
