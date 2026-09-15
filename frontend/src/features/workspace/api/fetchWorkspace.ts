import { httpClient } from '../../../shared/api/httpClient'
import type { ParticipantPermission, Workspace } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

interface WorkspaceApiResponse {
  workspaceId: number
  name: string
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  myPermission: ParticipantPermission
  createdAt: string
}

/**
 * 워크스페이스 단건 조회(`GET /api/workspaces/{workspaceId}`).
 *
 * 사이드바 머리말의 이름과 발치의 내 권한 배지가 이 응답을 쓴다 — 예전에는 둘 다
 * 하드코딩("워크스페이스"·OWNER)이었다.
 */
export async function fetchWorkspace(workspaceId: WorkspaceId): Promise<Workspace> {
  const response = await httpClient.get<WorkspaceApiResponse>(`/api/workspaces/${workspaceId}`)
  return {
    id: String(response.workspaceId),
    name: response.name,
    createdAt: response.createdAt,
    myPermission: response.myPermission,
  }
}
