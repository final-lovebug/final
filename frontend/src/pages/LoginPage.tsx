import { PagePlaceholder } from './PagePlaceholder'

// AppLayout(사이드바/탑바) 밖의 비인증 라우트. 실제 Google OAuth 목업 처리는 Phase 5에서 붙인다.
export function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-bg to-bg-alt p-6">
      <PagePlaceholder title="로그인" routeKey="login (pre-shell)" />
    </div>
  )
}
