import type { ParticipantPermission } from '../../workspace/model/types'
import type { MemberId, WorkspaceId } from '../../../shared/types/ids'

// 목업 데이터는 걷어냈다(2026-09-14). Member(이메일/표시이름) + Participant(워크스페이스
// 권한/참여일)는 서로 다른 엔드포인트에서 오므로 화면이 쓰는 모양으로 합친 뷰 타입을 둔다 —
// Member/Participant 모델 자체는 건드리지 않는다.
export interface WorkspaceMemberListItem {
  id: MemberId
  workspaceId: WorkspaceId
  /** 참여자 행 id. 내보내기·권한 변경 엔드포인트가 memberId가 아니라 이 값을 받는다. */
  participantId?: string
  initial: string
  name: string
  email: string
  permission: ParticipantPermission
  joinedAt: string
  /** Owner는 내보낼 수 없다(docs/API.md «워크스페이스 참여자 관리»). */
  removable: boolean
}

/** 워크스페이스 참여자 정원. docs/DOMAIN.md 정책: 참여자는 최대 5명. */
export const WORKSPACE_MEMBER_CAPACITY = 5
