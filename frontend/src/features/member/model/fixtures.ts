import type { ParticipantPermission } from '../../workspace/model/types'
import type { MemberId, WorkspaceId } from '../../../shared/types/ids'

// ui/data.js MEMBERS 이전. Member(이메일/표시이름) + Participant(워크스페이스 권한/참여일)를
// 화면이 쓰는 모양 그대로 한 아이템으로 합쳤다 — 실제로는 두 도메인을 조인해서 나와야 하는
// 값이라 별도 뷰 타입(WorkspaceMemberListItem)으로 두고 Member/Participant 모델 자체는 건드리지 않는다.
export interface WorkspaceMemberListItem {
  id: MemberId
  workspaceId: WorkspaceId
  initial: string
  name: string
  email: string
  permission: ParticipantPermission
  joinedAt: string
  /** OWNER는 제외할 수 없음 (docs/DOMAIN.md에 명시적 규칙은 없지만 ui/ 프로토타입 동작을 따름) */
  removable: boolean
}

export const WORKSPACE_MEMBER_FIXTURES: WorkspaceMemberListItem[] = [
  { id: 'member-mock-owner', workspaceId: 'potenup_be', initial: '민', name: '민뱅', email: 'idabc1234@gmail.com', permission: 'OWNER', joinedAt: '2026-08-24', removable: false },
  { id: 'member-mock-kim-dev', workspaceId: 'potenup_be', initial: '김', name: '김개발', email: 'kim.dev@potenup.io', permission: 'ADMIN', joinedAt: '2026-08-24', removable: true },
  { id: 'member-mock-lee-be', workspaceId: 'potenup_be', initial: '이', name: '이백엔드', email: 'lee.be@potenup.io', permission: 'ADMIN', joinedAt: '2026-08-25', removable: true },
  { id: 'member-mock-park-pm', workspaceId: 'potenup_be', initial: '박', name: '박기획', email: 'park.pm@potenup.io', permission: 'REGULAR', joinedAt: '2026-08-26', removable: true },
  { id: 'member-mock-choi-mkt', workspaceId: 'potenup_be', initial: '최', name: '최마케팅', email: 'choi.mkt@potenup.io', permission: 'REGULAR', joinedAt: '2026-08-28', removable: true },
]

/** 워크스페이스 참여자 정원. docs/DOMAIN.md 정책: 참여자는 최대 5명. */
export const WORKSPACE_MEMBER_CAPACITY = 5
