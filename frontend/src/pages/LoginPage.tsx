import { Button, Card } from '../shared/ui'
import { API_BASE_URL } from '../shared/config/env'

// AppLayout(사이드바/탑바) 밖의 비인증 라우트. 버튼을 누르면 백엔드의 Google OAuth2 로그인
// 시작 엔드포인트로 전체 페이지 리다이렉트한다(docs/API.md "로그인 시작",
// GET /oauth2/authorization/google). 로그인 완료 후 백엔드가 /oauth/callback으로 되돌려보내면
// OAuthCallbackPage가 나머지(코드 교환·세션 시작)를 처리한다.
export function LoginPage() {
  function handleLogin() {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
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
          Google로 계속하기
        </Button>
      </Card>
    </div>
  )
}
