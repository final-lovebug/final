import type { NotificationChannelSummary, NotificationTypeSetting } from './types'

// 알림 **목록**은 실연동됐다(T-INT-13a). 여기 남은 건 알림 **설정** 화면이 보여 주는
// **고정 정책**이다 — `D-54`가 채널 개념을 MVP1에서 걷어내 조회할 API도 저장할 API도 없다.
// 그래서 목업 응답이 아니라 "지금 실제로 이렇게 동작한다"는 상수로 읽어야 한다
// (pages/settings/SettingsNotificationsPage.tsx 주석 참고).

// MVP1의 전달 수단은 인앱 하나뿐이고, 인앱은 DB에 행이 있는 것이 곧 전달이다.
export const NOTIFICATION_TYPE_SETTING_FIXTURES: NotificationTypeSetting[] = [
  { type: 'REVIEW_REQUEST_RECEIVED', channels: ['IN_APP'] },
  { type: 'APPROVED', channels: ['IN_APP'] },
  { type: 'CHANGES_REQUESTED', channels: ['IN_APP'] },
  { type: 'REVISED', channels: ['IN_APP'] },
  { type: 'CANCELED', channels: ['IN_APP'] },
]

export const NOTIFICATION_CHANNEL_SUMMARY_FIXTURES: NotificationChannelSummary[] = [
  { channel: 'IN_APP', enabled: true, supported: true },
  { channel: 'EMAIL', enabled: false, supported: false },
  { channel: 'SLACK', enabled: false, supported: false },
]

export interface NotificationChannelDisplayMeta {
  name: string
  desc: string | null
  badge?: 'MVP2' | 'MVP3'
}

export const NOTIFICATION_CHANNEL_DISPLAY_META: Record<
  NotificationChannelSummary['channel'],
  NotificationChannelDisplayMeta
> = {
  IN_APP: { name: '인앱 알림', desc: '헤더 벨 아이콘에 표시됩니다' },
  SLACK: {
    name: 'Slack',
    desc: '워크스페이스별 웹훅 URL을 등록하면 채널로 발송됩니다',
    badge: 'MVP2',
  },
  EMAIL: { name: '메일', desc: null, badge: 'MVP2' },
}

export const NOTIFICATION_TYPE_LABEL: Record<NotificationTypeSetting['type'], string> = {
  REVIEW_REQUEST_RECEIVED: '리뷰 요청 도착',
  APPROVED: '승인',
  CHANGES_REQUESTED: '변경 요청',
  REVISED: '개정안 반영',
  CANCELED: '리뷰 요청 취소',
}
