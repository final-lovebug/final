import { httpClient } from '../../../shared/api/httpClient'
import type { ParticipantPermission } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

// 참여자 관리(docs/API.md «워크스페이스 참여자 관리»).
//
// **경로가 memberId가 아니라 participantId를 받는다** — 회원이 아니라 "이 워크스페이스의
// 참여자 행"을 지우는 것이기 때문이다. 그래서 `fetchWorkspaceMembers`가 participantId를
// 함께 들고 온다.
//
// 권한 규칙은 서버가 지킨다 — Owner는 내보낼 수 없고, Admin은 Regular만 내보낼 수 있으며,
// 권한 변경은 OWNER만 할 수 있다. 화면은 자기 권한으로 버튼을 감추기만 하고 최종 판정은
// 서버에 맡긴다(403이 올라오면 그대로 보여준다).

export interface RemoveParticipantInput {
  workspaceId: WorkspaceId
  participantId: string
}

export async function removeParticipant(input: RemoveParticipantInput): Promise<void> {
  await httpClient.delete(
    `/api/workspaces/${input.workspaceId}/participants/${input.participantId}`,
  )
}

export interface ChangeParticipantPermissionInput {
  workspaceId: WorkspaceId
  participantId: string
  /** `OWNER`는 이 엔드포인트로 줄 수 없다 — 소유권 이전은 별도 경로다. */
  permission: Extract<ParticipantPermission, 'ADMIN' | 'REGULAR'>
}

export async function changeParticipantPermission(
  input: ChangeParticipantPermissionInput,
): Promise<void> {
  await httpClient.patch(
    `/api/workspaces/${input.workspaceId}/participants/${input.participantId}/permission`,
    { permission: input.permission },
  )
}
