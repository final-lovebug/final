import type { NotificationChannel } from '../../../shared/types/common'

// ui/data.js NOTIF_CHANNELS/NOTIF_MATRIX 이전. WorkspaceSettings.defaultChannels는
// 단순 열거형 배열이라 desc/on/badge 같은 화면 정보를 못 담아 별도 뷰 타입으로 뒀다.
export interface NotificationChannelSetting {
  channel: NotificationChannel | 'KAKAO'
  name: string
  desc: string | null
  on: boolean
  /** MVP2/MVP3처럼 아직 지원하지 않는 채널 표시. 없으면 지금 바로 쓸 수 있는 채널 */
  badge?: 'MVP2' | 'MVP3'
}

export const NOTIFICATION_CHANNEL_FIXTURES: NotificationChannelSetting[] = [
  { channel: 'IN_APP', name: '인앱 알림', desc: '헤더 벨 아이콘에 표시됩니다', on: true },
  { channel: 'SLACK', name: 'Slack', desc: '워크스페이스별 웹훅 URL을 등록하면 채널로 발송됩니다 · 구현은 URL 하나에 POST하는 것이 전부입니다', on: false, badge: 'MVP2' },
  { channel: 'EMAIL', name: '메일', desc: null, on: false, badge: 'MVP2' },
  { channel: 'KAKAO', name: '웹 push', desc: '브라우저를 닫아도 받습니다. 브라우저 알림 권한이 필요합니다', on: false, badge: 'MVP3' },
]

export interface NotificationMatrixRow {
  trigger: string
}

export const NOTIFICATION_MATRIX_FIXTURES: NotificationMatrixRow[] = [
  { trigger: '대조 완료' },
  { trigger: '사전집 발행' },
  { trigger: '사전집 차이' },
]
