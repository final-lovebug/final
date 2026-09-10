// docs/DOMAIN.md "Workspace" 섹션(Workspace/Participant/RuleSet/Settings) 이식.
import type { NotificationChannel } from '../../../shared/types/common'
import type {
  MemberId,
  ParticipantId,
  RuleSetId,
  SettingsId,
  WorkspaceId,
} from '../../../shared/types/ids'

export interface Workspace {
  id: WorkspaceId
  name: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

// 소유자(Owner)/관리자(Admin)/사용자(Regular). docs/DOMAIN.md 정책: Owner와 Admin은 사실상 동급 권한.
export type ParticipantPermission = 'OWNER' | 'ADMIN' | 'REGULAR'

export interface Participant {
  id: ParticipantId
  workspaceId: WorkspaceId
  memberId: MemberId
  permission: ParticipantPermission
  joinedAt: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface RuleSet {
  id: RuleSetId
  workspaceId: WorkspaceId
  /** 승인에 필요한 최소 인원. 0~참여자 수(현재 정원 5) */
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

// docs/DOMAIN.md 표에는 workspaceId 필드가 없다(원문 그대로 유지). 실제로 워크스페이스에
// 어떻게 연결되는지는 백엔드 확정 후 갱신한다.
export interface WorkspaceSettings {
  id: SettingsId
  defaultChannels: NotificationChannel[]
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}
