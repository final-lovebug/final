import { delay } from '../../../shared/lib/delay'
import type { WorkspaceId } from '../../../shared/types/ids'
import { NOTIFICATION_FIXTURES } from '../model/fixtures'

// docs/API.md "모두 읽음" 목업 구현체. 응답은 갱신된 개수(updated) 하나뿐이다.
export async function markAllNotificationsRead(workspaceId: WorkspaceId): Promise<number> {
  await delay(200)

  const now = new Date().toISOString()
  let updated = 0
  for (const n of NOTIFICATION_FIXTURES) {
    if (n.workspaceId === workspaceId && !n.read) {
      n.read = true
      n.readAt = now
      updated += 1
    }
  }
  return updated
}
