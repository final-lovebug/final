// 여러 도메인이 함께 쓰는 값 객체. docs/UBIQUITOUS_LANGUAGE.md 4장(Document) "위치(TextRange)" 참고.
export interface TextRange {
  start: number
  end: number
}

// docs/DOMAIN.md의 Settings.defaultChannels / Notification.channels가 공유하는 열거형.
// 두 도메인(workspace, notification) 모두에서 쓰이므로 shared에 둔다.
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SLACK'

// docs/API.md "페이징·정렬 규격" 공통 응답 형태. 백엔드 PageResponse<T>(5개 키 고정)와
// 그대로 대응한다 — 목록 API가 배열을 그대로 반환하지 않는 한 이 형태를 쓴다
// (notification이 이 형태를 쓰는 첫 번째 프론트 도메인이라 shared로 올렸다).
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
