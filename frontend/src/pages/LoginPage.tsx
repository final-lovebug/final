import { Card, LogoMark } from '../shared/ui'
import { API_BASE_URL } from '../shared/config/env'

// ui/main.js renderLogin() 이식. AppLayout(사이드바/탑바) 밖의 비인증 라우트다.
// 버튼을 누르면 백엔드의 Google OAuth2 로그인 시작 엔드포인트로 전체 페이지 리다이렉트한다
// (docs/API.md "로그인 시작", GET /oauth2/authorization/google). 로그인 완료 후 백엔드가
// /oauth/callback으로 되돌려보내면 OAuthCallbackPage가 나머지(코드 교환·세션 시작)를 처리한다.
export function LoginPage() {
  function handleLogin() {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-bg to-bg-alt p-6">
      <Card className="flex w-[420px] max-w-full flex-col items-center gap-[22px] rounded-lg px-10 py-11 text-center shadow-[0_8px_30px_rgba(16,24,40,0.06)]">
        <LogoMark size={44} radius={11} />

        <div>
          <div className="mb-[6px] font-display text-xl font-bold">유비쿼터스</div>
          <div className="text-[13.5px] text-text-tertiary">
            팀이 쓰는 말을 하나로 맞춥니다
          </div>
        </div>

        <div className="mt-[6px] flex w-full flex-col gap-[10px]">
          <button
            type="button"
            onClick={handleLogin}
            className="flex w-full cursor-pointer items-center justify-center gap-[10px] rounded-[10px] border border-border-strong bg-surface p-3 text-[13.5px] font-semibold text-text hover:bg-bg"
          >
            <span className="inline-block h-4 w-4 rounded-[4px] bg-[#4285F4]" />
            Google로 계속하기
          </button>
        </div>

        <div className="text-[11.5px] text-text-quaternary">
          처음이시면 자동으로 가입됩니다
        </div>
      </Card>
    </div>
  )
}
