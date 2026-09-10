import { useNavigate } from 'react-router-dom'
import { Button, Card } from '../shared/ui'
import { useAuthStore } from '../shared/stores/authStore'
import { routes } from '../shared/config/routes'

// AppLayout(사이드바/탑바) 밖의 비인증 라우트. 실제 Google OAuth 연동은 이번 작업 범위 밖
// (frontend/docs/SPEC.md "백엔드 연동 범위" 참고) — 지금은 Zustand 목업 로그인만 있다.
export function LoginPage() {
  const navigate = useNavigate()
  const loginAsMock = useAuthStore((state) => state.loginAsMock)

  function handleLogin() {
    loginAsMock()
    navigate(routes.workspaces())
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-bg to-bg-alt p-6">
      <Card className="flex w-[420px] flex-col items-center gap-[22px] px-10 py-11 text-center shadow-card">
        <div className="flex h-9 w-9 items-center justify-center rounded-[7px] bg-accent font-display font-bold text-white">
          U
        </div>
        <div>
          <h1 className="font-display text-lg font-bold text-text">
            유비쿼터스 언어 사전
          </h1>
          <p className="mt-1 text-xs text-text-tertiary">
            팀 문서의 유비쿼터스 언어 정합성을 맞춰주는 협업 서비스
          </p>
        </div>
        <Button variant="outline" className="w-full" onClick={handleLogin}>
          Google로 계속하기 (목업)
        </Button>
      </Card>
    </div>
  )
}
