import { delay } from '../../../shared/lib/delay'
import type { Page } from '../../../shared/types/common'
import type { WorkspaceId } from '../../../shared/types/ids'
import { NOTIFICATION_FIXTURES } from '../model/fixtures'
import type { Notification } from '../model/types'

export interface FetchNotificationsParams {
  unreadOnly?: boolean
  page?: number
  size?: number
}

// docs/API.md "알림 목록 조회" 목업 구현체. 실 연동 시 이 함수 내부만
// `GET /api/workspaces/{workspaceId}/notifications?memberId=...&unreadOnly=...&page=...&size=...`
// 호출로 바꾸면 된다 — 반환 타입(Page<Notification>)을 유지하는 한 hooks/화면은 손댈 필요가
// 없다. 정렬은 createdAt desc 고정(sort 화이트리스트가 그거 하나뿐이다).
export async function fetchNotifications(
  workspaceId: WorkspaceId,
  { unreadOnly = false, page = 0, size = 20 }: FetchNotificationsParams = {},
): Promise<Page<Notification>> {
  await delay()

  const all = NOTIFICATION_FIXTURES.filter((n) => n.workspaceId === workspaceId)
    .filter((n) => !unreadOnly || !n.read)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))

  const start = page * size
  const content = all.slice(start, start + size)

  return {
    content,
    page,
    size,
    totalElements: all.length,
    totalPages: Math.max(1, Math.ceil(all.length / size)),
  }
}
