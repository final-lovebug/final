import { useNavigate } from 'react-router-dom'
import { Card, Pill } from '../shared/ui'
import type { WorkspaceId } from '../shared/types/ids'
import { useNotifications } from '../features/notification/hooks/useNotifications'
import { useMarkNotificationRead } from '../features/notification/hooks/useMarkNotificationRead'
import { useMarkAllNotificationsRead } from '../features/notification/hooks/useMarkAllNotificationsRead'
import { resolveNotificationLink } from '../features/notification/model/resolveNotificationLink'
import type { Notification } from '../features/notification/model/types'

interface NotificationPanelProps {
  workspaceId: WorkspaceId
  onNavigate: () => void
}

// Topbar 벨을 누르면 뜨는 패널. docs/API.md "알림 목록 조회"·"읽음 처리"·"모두 읽음"을 그대로
// 반영한다 — 버튼 문구·이동 경로는 서버가 안 주므로(D-46) resolveNotificationLink가 type과
// targetType·targetId에서 화면을 파생한다.
export function NotificationPanel({ workspaceId, onNavigate }: NotificationPanelProps) {
  const navigate = useNavigate()
  const { data, isLoading } = useNotifications(workspaceId, { size: 10 })
  const markRead = useMarkNotificationRead(workspaceId)
  const markAllRead = useMarkAllNotificationsRead(workspaceId)

  function handleClick(notification: Notification) {
    if (!notification.read) {
      markRead.mutate(notification.id)
    }
    navigate(resolveNotificationLink(notification, workspaceId))
    onNavigate()
  }

  const notifications = data?.content ?? []
  const hasUnread = notifications.some((n) => !n.read)

  return (
    <Card className="absolute right-0 top-[calc(100%+8px)] z-20 w-[360px] p-0 shadow-card-hover">
      <div className="flex items-center justify-between border-b border-border-soft px-4 py-3">
        <span className="text-[13px] font-bold">알림</span>
        <button
          type="button"
          className="text-[11.5px] font-semibold text-accent-strong disabled:cursor-not-allowed disabled:text-text-faint"
          onClick={() => markAllRead.mutate()}
          disabled={!hasUnread || markAllRead.isPending}
        >
          모두 읽음
        </button>
      </div>

      <div className="max-h-[400px] overflow-auto">
        {isLoading && <p className="p-4 text-[12.5px] text-text-tertiary">불러오는 중…</p>}
        {!isLoading && notifications.length === 0 && (
          <p className="p-4 text-[12.5px] text-text-tertiary">알림이 없습니다.</p>
        )}
        {notifications.map((notification) => (
          <button
            key={notification.id}
            type="button"
            onClick={() => handleClick(notification)}
            className={`flex w-full flex-col gap-1 border-b border-border-faint px-4 py-3 text-left last:border-b-0 hover:bg-bg ${
              notification.read ? '' : 'bg-accent-bg/40'
            }`}
          >
            <div className="flex items-start justify-between gap-2">
              <span className="text-[12.5px] font-bold text-text">{notification.title}</span>
              {!notification.read && <Pill tone="accent">NEW</Pill>}
            </div>
            <span className="text-[11.5px] text-text-tertiary">{notification.message}</span>
            <span className="text-[10.5px] text-text-quaternary">
              {new Date(notification.createdAt).toLocaleString('ko-KR')}
            </span>
          </button>
        ))}
      </div>
    </Card>
  )
}
