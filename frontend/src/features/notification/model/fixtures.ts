import type {
  Notification,
  NotificationChannelSummary,
  NotificationSettings,
  NotificationTypeSetting,
} from './types'

// docs/API.md "알림 목록 조회" 응답 예시 이식. 알림 패널(Topbar) 목업 데이터.
export const NOTIFICATION_FIXTURES: Notification[] = [
  {
    id: '42',
    workspaceId: '1',
    type: 'REVISED',
    targetType: 'REVIEW_REQUEST',
    targetId: '7',
    title: '기획 정책 정의서 개정안이 반영되었습니다',
    message: 'r5 → r6',
    channels: ['IN_APP'],
    read: false,
    readAt: null,
    createdAt: '2026-09-13T10:12:00Z',
  },
  {
    id: '41',
    workspaceId: '1',
    type: 'REVIEW_REQUEST_RECEIVED',
    targetType: 'REVIEW_REQUEST',
    targetId: '6',
    title: '용어 사전 리뷰 요청이 도착했습니다',
    message: '회원가입 정책 v2 리뷰를 요청받았습니다',
    channels: ['IN_APP'],
    read: false,
    readAt: null,
    createdAt: '2026-09-13T09:40:00Z',
  },
  {
    id: '40',
    workspaceId: '1',
    type: 'CHANGES_REQUESTED',
    targetType: 'REVIEW_REQUEST',
    targetId: '5',
    title: '리뷰어가 변경을 요청했습니다',
    message: '결제 정책 문서에 변경 요청이 등록되었습니다',
    channels: ['IN_APP'],
    read: true,
    readAt: '2026-09-12T15:02:00Z',
    createdAt: '2026-09-12T14:58:00Z',
  },
  {
    id: '39',
    workspaceId: '1',
    type: 'APPROVED',
    targetType: 'REVIEW_REQUEST',
    targetId: '4',
    title: '리뷰가 승인되었습니다',
    message: '온보딩 가이드 리뷰가 승인되었습니다',
    channels: ['IN_APP'],
    read: true,
    readAt: '2026-09-11T11:20:00Z',
    createdAt: '2026-09-11T11:10:00Z',
  },
  {
    id: '38',
    workspaceId: '1',
    type: 'CANCELED',
    targetType: 'REVIEW_REQUEST',
    targetId: '3',
    title: '리뷰 요청이 취소되었습니다',
    message: '결제 정책 문서 리뷰 요청이 취소되었습니다',
    channels: ['IN_APP'],
    read: true,
    readAt: '2026-09-10T09:05:00Z',
    createdAt: '2026-09-10T09:00:00Z',
  },
]

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
