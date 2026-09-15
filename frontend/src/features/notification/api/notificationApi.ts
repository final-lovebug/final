import type { Notification, NotificationTargetType, NotificationType } from '../model/types'

// 알림 api 4개가 공유하는 실 응답 타입과 매핑(docs/API.md "Notification API").

export interface NotificationApiResponse {
  notificationId: number
  workspaceId: number
  type: NotificationType
  targetType: NotificationTargetType
  targetId: number
  title: string
  message: string
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export function toNotification(response: NotificationApiResponse): Notification {
  return {
    id: String(response.notificationId),
    workspaceId: String(response.workspaceId),
    type: response.type,
    targetType: response.targetType,
    targetId: String(response.targetId),
    title: response.title,
    message: response.message,
    read: response.read,
    readAt: response.readAt,
    createdAt: response.createdAt,
  }
}
