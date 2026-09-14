import { httpClient } from '../../../shared/api/httpClient'
import type { MemberSiteRole } from '../../member/model'

export interface CompleteRegistrationResult {
  accessToken: string
  role: MemberSiteRole
}

// 닉네임 온보딩을 마무리한다(docs/API.md "닉네임 등록 완료",
// POST /api/auth/oauth/google/complete-registration). 이 호출이 성공해야 비로소
// 회원이 생성되고 로그인이 끝난다 — exchangeOAuthCode가 needsNickname을 응답했을 때만 쓴다.
export async function completeRegistration(
  registrationToken: string,
  displayName: string,
): Promise<CompleteRegistrationResult> {
  return httpClient.post<CompleteRegistrationResult>('/api/auth/oauth/google/complete-registration', {
    registrationToken,
    displayName,
  })
}
