import { httpClient } from '../../../shared/api/httpClient'
import type { ParticipantPermission, Workspace } from '../model/types'

interface WorkspaceApiResponse {
  workspaceId: number
  name: string
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  myPermission: ParticipantPermission
  createdAt: string
}

export async function createWorkspace(name: string): Promise<Workspace> {
  const response = await httpClient.post<WorkspaceApiResponse>('/api/workspaces', { name })

  return {
    id: String(response.workspaceId),
    name: response.name,
    createdAt: response.createdAt,
    myPermission: response.myPermission,
  }
}
