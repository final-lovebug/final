import { httpClient } from '../../../shared/api/httpClient'
import type { WorkspaceId } from '../../../shared/types/ids'

interface UnreadCountApiResponse {
  unreadCount: number
}

// 실제 백엔드 연동(docs/API.md "미읽음 수 조회"). 헤더 벨의 표시용 — 목록을 받아 세지 않는다.
export async function fetchUnreadCount(workspaceId: WorkspaceId): Promise<number> {
  const response = await httpClient.get<UnreadCountApiResponse>(
    `/api/workspaces/${workspaceId}/notifications/unread-count`,
  )
  return response.unreadCount
}
