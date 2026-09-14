import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { completeRegistration } from '../../features/auth/api/completeRegistration'
import { fetchCurrentMember } from '../../features/member/api/fetchCurrentMember'
import { useAuthStore } from '../../shared/stores/authStore'
import { routes } from '../../shared/config/routes'
import { Button, Card } from '../../shared/ui'

interface LocationState {
  registrationToken?: string
}

// Google 실명 대신 사용자가 직접 닉네임을 정하는 최초 로그인 온보딩 화면
// (REQ-USR-001 9/11 번복, docs/API.md "닉네임 등록 완료"). OAuthCallbackPage가
// exchangeOAuthCode 응답의 needsNickname==true일 때만 등록 토큰을 들고 이 화면으로
// 보낸다 — 새로고침 등으로 등록 토큰 없이 직접 들어오면 다시 로그인하게 한다.
export function NicknameOnboardingPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)
  const setCurrentMember = useAuthStore((state) => state.setCurrentMember)

  const registrationToken = (location.state as LocationState | null)?.registrationToken
  const [displayName, setDisplayName] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  if (!registrationToken) {
    return <Navigate to={routes.login()} replace />
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!displayName.trim() || isSubmitting) return

    setIsSubmitting(true)
    setErrorMessage(null)
    try {
      const { accessToken } = await completeRegistration(registrationToken!, displayName.trim())
      login(accessToken)
      const member = await fetchCurrentMember()
      setCurrentMember(member)
      navigate(routes.workspaces(), { replace: true })
    } catch {
      setErrorMessage('닉네임 등록 중 문제가 발생했습니다. 다시 시도해 주세요.')
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-bg to-bg-alt p-6">
      <Card className="flex w-[420px] flex-col gap-[22px] px-10 py-11 text-center shadow-card">
        <div>
          <h1 className="font-display text-lg font-bold text-text">닉네임을 정해주세요</h1>
          <p className="mt-1 text-xs text-text-tertiary">
            팀원들에게 표시될 이름입니다. 나중에 설정에서 바꿀 수 있어요.
          </p>
        </div>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <label className="flex flex-col gap-2 text-left">
            <span className="text-xs font-bold text-text">닉네임</span>
            <input
              className="rounded-sm border border-border-strong px-3 py-[9px] text-[13px] text-text"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={100}
              autoFocus
              required
            />
          </label>
          {errorMessage && <p className="text-xs text-danger">{errorMessage}</p>}
          <Button
            type="submit"
            variant="primary"
            className="w-full"
            disabled={!displayName.trim() || isSubmitting}
          >
            {isSubmitting ? '등록 중...' : '시작하기'}
          </Button>
        </form>
      </Card>
    </div>
  )
}
