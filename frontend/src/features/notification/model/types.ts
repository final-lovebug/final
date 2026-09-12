// docs/DOMAIN.md "Notification" 섹션 이식.
import type { NotificationChannel } from '../../../shared/types/common'
import type {
  MemberId,
  NotificationId,
  WorkspaceId,
} from '../../../shared/types/ids'

// 리뷰요청도착 / 코멘트등록 / 승인 / 반려 / 대조완료 / 추출완료
export type NotificationType =
  | 'REVIEW_REQUEST_RECEIVED'
  | 'COMMENT_ADDED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CONTRAST_COMPLETED'
  | 'EXTRACTION_COMPLETED'

// docs/DOMAIN.md에 목록만 있고 값 정의는 없어(423줄 "대상 targetType, targetId Enum, UUID"),
// 눌렀을 때 이동 가능한 화면 도메인 기준으로 추정했다. 백엔드 확정 시 갱신 필요.
export type NotificationTargetType = 'DOCUMENT' | 'DICTIONARY' | 'REVIEW_REQUEST'

export interface Notification {
  id: NotificationId
  recipientId: MemberId
  workspaceId: WorkspaceId
  type: NotificationType
  targetType: NotificationTargetType
  targetId: string
  message: string
  channels: NotificationChannel[]
  /** 없으면 안 읽음 */
  readAt?: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}
