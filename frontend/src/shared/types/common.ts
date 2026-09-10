// 여러 도메인이 함께 쓰는 값 객체. docs/UBIQUITOUS_LANGUAGE.md 4장(Document) "위치(TextRange)" 참고.
export interface TextRange {
  start: number
  end: number
}

// docs/DOMAIN.md의 Settings.defaultChannels / Notification.channels가 공유하는 열거형.
// 두 도메인(workspace, notification) 모두에서 쓰이므로 shared에 둔다.
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SLACK'
