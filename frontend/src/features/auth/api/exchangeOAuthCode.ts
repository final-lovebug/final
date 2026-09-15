import { httpClient } from '../../../shared/api/httpClient'
import type { MemberSiteRole } from '../../member/model'

export interface OAuthLoginResult {
  needsNickname: false
  accessToken: string
  role: MemberSiteRole
}

export interface OAuthRegistrationRequiredResult {
  needsNickname: true
  registrationToken: string
}

export type OAuthExchangeResult = OAuthLoginResult | OAuthRegistrationRequiredResult

interface OAuthExchangeResponse {
  accessToken: string | null
  role: MemberSiteRole | null
  needsNickname: boolean | null
  registrationToken: string | null
}

// Google 로그인 콜백에서 받은 1회용 code를 실제 토큰으로 교환한다
// (docs/API.md "콜백 및 토큰 교환", POST /api/auth/oauth/google/exchange).
// 이미 가입된 회원이면 로그인이 끝나고(OAuthLoginResult), 처음 보는 Google 계정이면
// 아직 회원이 아니라 등록 토큰만 온다(OAuthRegistrationRequiredResult) — 이 경우
// completeRegistration으로 닉네임을 받아야 로그인이 끝난다.
// refresh token은 Set-Cookie(HttpOnly)로만 내려오므로 응답 본문에는 없다.
export async function exchangeOAuthCode(code: string): Promise<OAuthExchangeResult> {
  const response = await httpClient.post<OAuthExchangeResponse>('/api/auth/oauth/google/exchange', { code })

  if (response.needsNickname) {
    return { needsNickname: true, registrationToken: response.registrationToken! }
  }
  return { needsNickname: false, accessToken: response.accessToken!, role: response.role! }
}
