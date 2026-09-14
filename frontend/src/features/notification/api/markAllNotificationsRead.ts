import { httpClient } from '../../../shared/api/httpClient'
import type { WorkspaceId } from '../../../shared/types/ids'

interface MarkAllReadApiResponse {
  updated: number
}

// 실제 백엔드 연동(docs/API.md "모두 읽음"). 응답은 갱신된 개수 하나뿐이다.
export async function markAllNotificationsRead(workspaceId: WorkspaceId): Promise<number> {
  const response = await httpClient.patch<MarkAllReadApiResponse>(
    `/api/workspaces/${workspaceId}/notifications/read-all`,
  )
  return response.updated
}
