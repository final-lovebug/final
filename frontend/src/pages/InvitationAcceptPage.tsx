import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, LogoMark } from '../shared/ui'
import { ApiError } from '../shared/api/httpClient'
import { routes } from '../shared/config/routes'
import { API_BASE_URL } from '../shared/config/env'
import { savePendingInvitationToken } from '../shared/lib/pendingInvitation'
import { useAuthStore } from '../shared/stores/authStore'
import { useAcceptInvitation } from '../features/workspace/hooks/useInvitations'

// 초대 링크(`/invitations/{token}`)의 착지점. 설정 › 멤버에서 발급한 링크를 받은 사람이
// 여기로 들어온다.
//
// **RequireAuth로 감싸지 않는다.** 감싸면 로그아웃 상태에서 /login으로 튕기면서 토큰이
// 사라진다 — 링크를 받은 사람은 대개 아직 로그인하지 않은 상태다. 대신 이 화면이 직접
// 인증 여부를 보고, 없으면 토큰을 sessionStorage에 맡긴 뒤 로그인으로 보낸다
// (OAuthCallbackPage가 로그인 직후 그 토큰을 집어 여기로 되돌려보낸다).
export function InvitationAcceptPage() {
  const { token = '' } = useParams<{ token: string }>()
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const navigate = useNavigate()
  const { mutate: accept } = useAcceptInvitation()
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  // StrictMode 이중 렌더로 수락을 두 번 보내지 않도록 막는다 — 두 번째는
  // INVITATION_ALREADY_PARTICIPANT로 실패해 성공을 실패처럼 보이게 한다.
  const requestedRef = useRef(false)

  useEffect(() => {
    if (!isAuthenticated || !token || requestedRef.current) return
    requestedRef.current = true

    accept(token, {
      onSuccess: (workspace) => navigate(routes.documents(workspace.id), { replace: true }),
      onError: (caught) =>
        setErrorMessage(
          caught instanceof ApiError ? caught.message : '초대를 수락하지 못했습니다.',
        ),
    })
  }, [isAuthenticated, token, accept, navigate])

  function handleLogin() {
    savePendingInvitationToken(token)
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-bg to-bg-alt p-6">
      <Card className="flex w-[420px] max-w-full flex-col items-center gap-[22px] rounded-lg px-10 py-11 text-center shadow-[0_8px_30px_rgba(16,24,40,0.06)]">
        <LogoMark size={44} radius={11} />

        {!token ? (
          <p className="text-[13.5px] text-danger">초대 링크가 올바르지 않습니다.</p>
        ) : !isAuthenticated ? (
          <>
            <div>
              <div className="mb-[6px] font-display text-xl font-bold">워크스페이스 초대</div>
              <div className="text-[13.5px] text-text-tertiary">
                초대를 수락하려면 먼저 로그인해 주세요.
              </div>
            </div>
            <button
              type="button"
              onClick={handleLogin}
              className="flex w-full cursor-pointer items-center justify-center gap-[10px] rounded-[10px] border border-border-strong bg-surface p-3 text-[13.5px] font-semibold text-text hover:bg-bg"
            >
              <span className="inline-block h-4 w-4 rounded-[4px] bg-[#4285F4]" />
              Google로 계속하기
            </button>
            <div className="text-[11.5px] text-text-quaternary">
              로그인하면 초대가 이어서 수락됩니다
            </div>
          </>
        ) : errorMessage ? (
          <>
            <p className="text-[13.5px] text-danger">{errorMessage}</p>
            <Button variant="outline" onClick={() => navigate(routes.workspaces())}>
              워크스페이스 목록으로
            </Button>
          </>
        ) : (
          <p className="text-[13.5px] text-text-tertiary">초대를 수락하는 중입니다...</p>
        )}
      </Card>
    </div>
  )
}
