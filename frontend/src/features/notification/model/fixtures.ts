import type {
  NotificationChannelSummary,
  NotificationSettings,
  NotificationTypeSetting,
} from './types'

// 알림 **목록**은 실연동됐다(T-INT-13a) — 여기 남은 건 아직 대응 백엔드가 없는
// 알림 **설정** 화면의 목업뿐이다(`D-54`로 채널 개념이 제거돼 되살릴 API가 없다).

// docs/API.md "알림 설정 조회" 기본값 이식 — 설정이 없는 워크스페이스는 5개 유형 ×
// ["IN_APP"]로 시작한다.
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

export const NOTIFICATION_SETTINGS_FIXTURE: NotificationSettings = {
  channels: NOTIFICATION_CHANNEL_SUMMARY_FIXTURES,
  settings: NOTIFICATION_TYPE_SETTING_FIXTURES,
}

// 화면 표시용 메타 — 서버 응답(NotificationChannel enum)에는 없는 이름·설명·MVP 배지.
// 카카오톡(웹 push)은 서버 enum에 아예 없는 화면 전용 항목이라(docs/API.md, backend
// NotificationChannel 주석 "네 번째 항목은 화면 전용 키") 별도로 뒀다 — 토글 자체가 잠겨있다.
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
