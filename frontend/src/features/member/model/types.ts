// docs/DOMAIN.md "Member" 섹션 이식.
import type { MemberId } from '../../../shared/types/ids'

export type MemberStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN' // 가입대기/활성/정지/탈퇴
export type MemberSiteRole = 'REGULAR' | 'ADMIN' // 사이트 단위 권한. 워크스페이스 단위 권한은 Participant.permission
export type SocialProvider = 'GOOGLE' // MVP1은 구글만. 카카오/네이버는 MVP2 확장 예약

export interface Member {
  id: MemberId
  email: string
  displayName: string
  status: MemberStatus
  role: MemberSiteRole
  provider: SocialProvider
  providerId: string
  createdAt: string
  updatedAt: string
}
