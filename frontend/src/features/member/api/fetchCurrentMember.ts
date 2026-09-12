import { httpClient } from '../../../shared/api/httpClient'
import type { Member, MemberSiteRole, MemberStatus } from '../model/types'

interface MemberMeResponse {
  memberId: number
  email: string
  displayName: string
  status: MemberStatus
  role: MemberSiteRole
}

// 실제 백엔드 연동(docs/API.md "내 정보 조회", GET /api/members/me). 로그인 직후
// OAuthCallbackPage가 호출해 currentMember를 채운다. memberId는 숫자(Long, PK)라
// 문자열 MemberId로 바꿔 담고, provider/providerId/createdAt/updatedAt은 이 엔드포인트가
// 내려주지 않으므로 비운다(model/types.ts에서 옵셔널 처리).
export async function fetchCurrentMember(): Promise<Member> {
  const response = await httpClient.get<MemberMeResponse>('/api/members/me')
  return {
    id: String(response.memberId),
    email: response.email,
    displayName: response.displayName,
    status: response.status,
    role: response.role,
  }
}
