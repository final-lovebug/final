import { httpClient } from '../../../shared/api/httpClient'
import type { NotificationId, WorkspaceId } from '../../../shared/types/ids'
import type { Notification } from '../model/types'
import { toNotification, type NotificationApiResponse } from './notificationApi'

// 실제 백엔드 연동(docs/API.md "읽음 처리").
//
// 멱등하다 — 이미 읽은 알림을 다시 불러도 readAt이 바뀌지 않는다. 남의 알림에는 403이
// 아니라 404를 준다(존재를 숨긴다).
export async function markNotificationRead(
  workspaceId: WorkspaceId,
  notificationId: NotificationId,
): Promise<Notification> {
  const response = await httpClient.patch<NotificationApiResponse>(
    `/api/workspaces/${workspaceId}/notifications/${notificationId}/read`,
  )
  return toNotification(response)
}
