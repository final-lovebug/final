import { httpClient } from '../../../shared/api/httpClient'
import type { MemberSiteRole } from '../../member/model'

export interface OAuthExchangeResult {
  accessToken: string
  role: MemberSiteRole
}

// Google 로그인 콜백에서 받은 1회용 code를 실제 토큰으로 교환한다
// (docs/API.md "콜백 및 토큰 교환", POST /api/auth/oauth/google/exchange).
// refresh token은 Set-Cookie(HttpOnly)로만 내려오므로 응답 본문에는 없다 — 브라우저가
// 자동으로 들고 있고, 이후 요청은 shared/api/httpClient가 credentials: 'include'로 함께 보낸다.
export async function exchangeOAuthCode(code: string): Promise<OAuthExchangeResult> {
  return httpClient.post<OAuthExchangeResult>('/api/auth/oauth/google/exchange', { code })
}
