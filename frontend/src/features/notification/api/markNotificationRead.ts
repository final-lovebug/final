import { delay } from '../../../shared/lib/delay'
import type { NotificationId, WorkspaceId } from '../../../shared/types/ids'
import { NOTIFICATION_FIXTURES } from '../model/fixtures'
import type { Notification } from '../model/types'

// docs/API.md "읽음 처리" 목업 구현체. 멱등 — 이미 읽었으면 readAt을 바꾸지 않고 그대로 둔다.
export async function markNotificationRead(
  workspaceId: WorkspaceId,
  notificationId: NotificationId,
): Promise<Notification> {
  await delay(200)

  const notification = NOTIFICATION_FIXTURES.find(
    (n) => n.workspaceId === workspaceId && n.id === notificationId,
  )
  if (!notification) {
    throw new Error('NOTIFICATION_NOT_FOUND')
  }
  if (!notification.read) {
    notification.read = true
    notification.readAt = new Date().toISOString()
  }
  return notification
}
