import { httpClient } from '../../../shared/api/httpClient'
import type { WorkspaceMemberListItem } from '../model/fixtures'
import type { ParticipantPermission } from '../../workspace/model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

interface ParticipantApiItem {
  participantId: number
  workspaceId: number
  memberId: number
  permission: ParticipantPermission
  joinedAt: string
}

interface MemberSummaryApiItem {
  memberId: number
  displayName: string
  email: string
}

// 실제 백엔드 연동. 참여자 정보(권한·참여일)와 회원 정보(이름·이메일)가 서로 다른
// 엔드포인트에서 온다 — 두 호출을 합쳐서(join) 화면이 쓰는 모양을 만든다.
//
// 1. GET /api/workspaces/{workspaceId}/participants(docs/API.md "워크스페이스 참여자
//    관리") — memberId·permission·joinedAt
// 2. GET /api/members?ids=1,2,3(T-INT-18, docs/API.md "다른 회원 조회") — displayName·
//    email. 존재하지 않는 id는 조용히 빠지므로(탈퇴 등) 그런 경우 이름/이메일을 "—"로
//    표시한다.
export async function fetchWorkspaceMembers(
  workspaceId: WorkspaceId,
): Promise<WorkspaceMemberListItem[]> {
  const participants = await httpClient.get<ParticipantApiItem[]>(
    `/api/workspaces/${workspaceId}/participants`,
  )
  if (participants.length === 0) return []

  const ids = participants.map((p) => p.memberId).join(',')
  const summaries = await httpClient.get<MemberSummaryApiItem[]>(`/api/members?ids=${ids}`)
  const summaryByMemberId = new Map(summaries.map((s) => [s.memberId, s]))

  return participants.map((participant) => {
    const summary = summaryByMemberId.get(participant.memberId)
    const name = summary?.displayName ?? '—'
    return {
      id: String(participant.memberId),
      participantId: String(participant.participantId),
      workspaceId,
      initial: name.charAt(0),
      name,
      email: summary?.email ?? '—',
      permission: participant.permission,
      joinedAt: participant.joinedAt,
      removable: participant.permission !== 'OWNER',
    }
  })
}
