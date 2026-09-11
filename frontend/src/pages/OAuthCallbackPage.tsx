import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { exchangeOAuthCode } from '../features/auth/api/exchangeOAuthCode'
import { fetchCurrentMember } from '../features/member/api/fetchCurrentMember'
import { useAuthStore } from '../shared/stores/authStore'
import { routes } from '../shared/config/routes'

// 백엔드가 Google 로그인 완료 후 리다이렉트하는 목적지(app.oauth.frontend-redirect-uri,
// backend/CLAUDE.md 환경변수 표). docs/API.md "콜백 및 토큰 교환" 참고 — 쿼리로 1회용
// code(성공) 또는 error=oauth_failed(실패)를 받는다. code를 실제 토큰으로 교환하고,
// 내 정보를 조회해 authStore를 채운 뒤 워크스페이스 화면으로 이동한다.
export function OAuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)
  const setCurrentMember = useAuthStore((state) => state.setCurrentMember)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  // StrictMode 이중 렌더/의존성 변화로 교환 코드(1회용)를 두 번 소비하지 않도록 막는다.
  const requestedRef = useRef(false)

  useEffect(() => {
    if (requestedRef.current) return
    requestedRef.current = true

    const code = searchParams.get('code')
    const oauthError = searchParams.get('error')

    if (oauthError) {
      navigate(routes.login(), { replace: true })
      return
    }

    if (!code) {
      setErrorMessage('로그인 코드가 없습니다. 다시 시도해 주세요.')
      return
    }

    void (async () => {
      try {
        const { accessToken } = await exchangeOAuthCode(code)
        login(accessToken)
        const member = await fetchCurrentMember()
        setCurrentMember(member)
        navigate(routes.workspaces(), { replace: true })
      } catch {
        setErrorMessage('로그인 처리 중 문제가 발생했습니다. 다시 시도해 주세요.')
      }
    })()
  }, [searchParams, navigate, login, setCurrentMember])

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-bg to-bg-alt p-6">
      <p className="text-sm text-text-tertiary">
        {errorMessage ?? '로그인 처리 중입니다...'}
      </p>
    </div>
  )
}
