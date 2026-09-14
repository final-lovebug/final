import { Link, useNavigate } from 'react-router-dom'
import { Card, Pill } from '../shared/ui'
import { cx } from '../shared/lib/cx'
import { routes } from '../shared/config/routes'
import { toRelativeTime } from '../shared/lib/relativeTime'
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

// ui/main.js renderNotifPanel() 이식. Topbar 벨을 누르면 뜨는 패널이다.
//
// docs/API.md "알림 목록 조회"·"읽음 처리"·"모두 읽음"을 그대로 반영한다 — 이동 경로는
// 서버가 안 주므로 resolveNotificationLink가 type과 targetType·targetId에서 파생한다.
//
// **(2026-09-14 디자인 정합)** 프로토타입의 모양을 되살렸다 — 읽지 않은 항목의 점 표시,
// 읽은 항목의 흐린 처리, 「인앱」 배지, 항목마다의 액션 줄, 그리고 발치의 안내 + 알림 설정
// 링크. 폭도 400px로 맞췄다.
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
  const hasUnread = notifications.some((notification) => !notification.read)

  return (
    <Card className="absolute right-0 top-[calc(100%+8px)] z-30 w-[400px] p-0 shadow-pop">
      <div className="flex items-center justify-between border-b border-border-soft px-4 py-[14px]">
        <div className="flex items-center gap-2">
          <span className="font-display text-[14px] font-bold">알림</span>
          <Pill tone="neutral">인앱</Pill>
        </div>
        <button
          type="button"
          className="cursor-pointer text-[11.5px] font-semibold text-accent-strong disabled:cursor-not-allowed disabled:text-text-faint"
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
            className={cx(
              'flex w-full cursor-pointer gap-[10px] border-b border-border-faint px-4 py-[13px] text-left last:border-b-0 hover:bg-surface-muted',
              notification.read && 'opacity-60',
            )}
          >
            <span
              className={cx(
                'mt-[6px] h-[6px] w-[6px] shrink-0 rounded-full',
                notification.read ? 'bg-transparent' : 'bg-accent',
              )}
            />
            <span className="flex-1">
              <span
                className={cx(
                  'block text-[12.5px] leading-[1.5] text-text',
                  notification.read ? 'font-semibold' : 'font-bold',
                )}
              >
                {notification.title}
              </span>
              <span className="my-[3px] block text-[11.5px] text-text-tertiary">
                {notification.message} · {toRelativeTime(notification.createdAt)}
              </span>
              <span className="block text-[11px] font-semibold text-accent-strong">
                확인하기 →
              </span>
            </span>
          </button>
        ))}
      </div>

      <div className="border-t border-border-soft px-4 py-[11px] text-[11.5px] text-text-quaternary">
        용어 추가만 있는 리비전은 알림을 보내지 않습니다 ·{' '}
        <Link
          to={routes.settingsNotifications(workspaceId)}
          onClick={onNavigate}
          className="underline"
        >
          알림 설정
        </Link>
      </div>
    </Card>
  )
}
