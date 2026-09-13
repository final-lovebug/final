import { delay } from '../../../shared/lib/delay'
import type { WorkspaceId } from '../../../shared/types/ids'
import { NOTIFICATION_FIXTURES } from '../model/fixtures'

// docs/API.md "미읽음 수 조회" 목업 구현체. 헤더 벨의 표시용 — 목록을 받아 세지 않는다.
export async function fetchUnreadCount(workspaceId: WorkspaceId): Promise<number> {
  await delay(150)
  return NOTIFICATION_FIXTURES.filter((n) => n.workspaceId === workspaceId && !n.read).length
}
