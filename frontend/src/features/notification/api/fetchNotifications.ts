import { httpClient } from '../../../shared/api/httpClient'
import type { Page } from '../../../shared/types/common'
import type { WorkspaceId } from '../../../shared/types/ids'
import type { Notification } from '../model/types'
import { toNotification, type NotificationApiResponse, type PageResponse } from './notificationApi'

export interface FetchNotificationsParams {
  unreadOnly?: boolean
  page?: number
  size?: number
}

// 실제 백엔드 연동(docs/API.md "알림 목록 조회",
// GET /api/workspaces/{workspaceId}/notifications).
//
// 목록은 언제나 요청자 본인의 것이다 — 수신자를 파라미터로 받지 않는다(인증 주체에서
// 해석한다). 정렬은 createdAt desc 고정이다(sort 화이트리스트가 그거 하나뿐).
export async function fetchNotifications(
  workspaceId: WorkspaceId,
  { unreadOnly = false, page = 0, size = 20 }: FetchNotificationsParams = {},
): Promise<Page<Notification>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: 'createdAt,desc',
  })
  if (unreadOnly) params.set('unreadOnly', 'true')

  const response = await httpClient.get<PageResponse<NotificationApiResponse>>(
    `/api/workspaces/${workspaceId}/notifications?${params.toString()}`,
  )
  return {
    content: response.content.map(toNotification),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  }
}
