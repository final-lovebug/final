// docs/API.md "Notification API" 이식. 2026-09-13 Notification 도메인 세션(D-44~D-49)이
// 백엔드 계약을 확정해 예전 초안(코멘트등록/대조완료/추출완료 포함 6종)을 대체했다.
import type { NotificationChannel } from '../../../shared/types/common'
import type { NotificationId, WorkspaceId } from '../../../shared/types/ids'

// MVP1은 리뷰 라이프사이클 5종뿐이다(D-44). DOMAIN.md가 함께 적은 코멘트등록·대조완료·
// 추출완료는 정의하지 않는다 — 코멘트는 등록 이벤트가 없고, 추출·대조는 작업 테이블
// 폴링이라(D-34) 구독할 이벤트가 없다.
export type NotificationType =
  | 'REVIEW_REQUEST_RECEIVED'
  | 'APPROVED'
  | 'CHANGES_REQUESTED'
  | 'REVISED'
  | 'CANCELED'

export type NotificationTargetType = 'DOCUMENT' | 'DICTIONARY' | 'REVIEW_REQUEST'

/**
 * 버튼 문구와 이동 경로는 담지 않는다(D-49). `type`·`targetType`·`targetId`에서 화면이
 * 파생한다 — routes.ts와 함께 features/notification/model/resolveNotificationLink.ts 참고.
 *
 * **채널(`channels`)은 없다** — `D-54`로 MVP1에서 `NotificationChannel` 개념 자체가
 * 백엔드에서 제거됐다. 아래 `NotificationSettings` 계열은 **설정 화면이 현재 고정 정책을
 * 그려 내기 위한 화면 전용 타입**이다 — 조회·저장 엔드포인트가 없어서 서버 응답이 아니다
 * (`T-INT-13`·`pages/settings/SettingsNotificationsPage.tsx` 참고).
 */
export interface Notification {
  id: NotificationId
  workspaceId: WorkspaceId
  type: NotificationType
  targetType: NotificationTargetType
  targetId: string
  title: string
  message: string
  /** `readAt != null`과 항상 함께 움직인다(read == (readAt != null) 불변식). */
  read: boolean
  readAt: string | null
  createdAt: string
}

/**
 * 설정 화면 한 벌 — 채널 토글 요약과 유형 × 채널 매트릭스.
 *
 * **서버에서 오지 않는다.** 지금은 화면이 고정 정책(인앱만 동작)을 보여 주는 데만 쓴다.
 * 수신 설정(`REQ-NTF-009`)이 MVP2로 들어오면 이 모양이 응답 타입이 될 것으로 본다.
 */
export interface NotificationSettings {
  channels: NotificationChannelSummary[]
  settings: NotificationTypeSetting[]
}

export interface NotificationChannelSummary {
  channel: NotificationChannel
  /** 유형 하나라도 이 채널을 쓰면 true. */
  enabled: boolean
  /** false면 아직 발송 어댑터가 없는 채널이다(MVP1은 IN_APP만 supported). 화면이 흐리게 표시한다. */
  supported: boolean
}

export interface NotificationTypeSetting {
  type: NotificationType
  /** 빈 배열이면 이 유형은 알림을 만들지 않는다. */
  channels: NotificationChannel[]
}
