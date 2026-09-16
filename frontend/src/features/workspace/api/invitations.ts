import { httpClient } from '../../../shared/api/httpClient'
import type {
  InvitablePermission,
  Invitation,
  InvitationStatus,
  Workspace,
} from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

// 워크스페이스 초대(docs/API.md «워크스페이스 초대»). 백엔드는 이미 다 있고
// (workspace/presentation/InvitationController), 여기서는 그 계약을 그대로 옮긴다.
//
// 발급·목록·취소는 ADMIN 이상이고, **수락만 워크스페이스 하위 경로가 아니다** — 링크를 받은
// 사람은 자기가 어느 워크스페이스에 초대됐는지 모른 채 토큰만 들고 들어오기 때문이다.

interface InvitationApiResponse {
  invitationId: number
  workspaceId: number
  inviteeEmail: string | null
  /** 발급(201) 응답에만 있다. 목록 응답에서는 `@JsonInclude(NON_NULL)`로 빠진다. */
  token?: string
  permission: InvitablePermission
  status: InvitationStatus
  expiresAt: string
}

interface WorkspaceApiResponse {
  workspaceId: number
  name: string
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  myPermission: string
  createdAt: string
}

function toInvitation(response: InvitationApiResponse): Invitation {
  return {
    id: String(response.invitationId),
    workspaceId: String(response.workspaceId),
    inviteeEmail: response.inviteeEmail,
    token: response.token,
    permission: response.permission,
    status: response.status,
    expiresAt: response.expiresAt,
  }
}

export interface IssueInvitationInput {
  workspaceId: WorkspaceId
  /** 선택. 링크 복사 방식에서는 생략한다 — 빈 문자열 대신 `undefined`로 보낸다. */
  inviteeEmail?: string
  permission: InvitablePermission
}

/** `201`. 응답에 링크 복사용 `token`이 실려 오는 **유일한** 호출이다. */
export async function issueInvitation(input: IssueInvitationInput): Promise<Invitation> {
  const response = await httpClient.post<InvitationApiResponse>(
    `/api/workspaces/${input.workspaceId}/invitations`,
    { inviteeEmail: input.inviteeEmail, permission: input.permission },
  )
  return toInvitation(response)
}

/** 목록은 페이징이 아니라 배열이다 — `fetchAllPages` 대상이 아니다. */
export async function fetchInvitations(
  workspaceId: WorkspaceId,
  status?: InvitationStatus,
): Promise<Invitation[]> {
  const query = status ? `?status=${status}` : ''
  const responses = await httpClient.get<InvitationApiResponse[]>(
    `/api/workspaces/${workspaceId}/invitations${query}`,
  )
  return responses.map(toInvitation)
}

export interface CancelInvitationInput {
  workspaceId: WorkspaceId
  invitationId: string
}

export async function cancelInvitation(input: CancelInvitationInput): Promise<void> {
  await httpClient.delete(
    `/api/workspaces/${input.workspaceId}/invitations/${input.invitationId}`,
  )
}

/**
 * 수락하면 참여자로 등록되고, 수락 직후 화면 진입에 필요한 `myPermission`이 실려 온다.
 * 정원(5명)·중복 참여·만료 판정은 전부 서버가 이 시점에 한다.
 */
export async function acceptInvitation(token: string): Promise<Workspace> {
  const response = await httpClient.post<WorkspaceApiResponse>(
    `/api/invitations/${encodeURIComponent(token)}/accept`,
  )
  return {
    id: String(response.workspaceId),
    name: response.name,
    createdAt: response.createdAt,
    myPermission: response.myPermission as Workspace['myPermission'],
  }
}
