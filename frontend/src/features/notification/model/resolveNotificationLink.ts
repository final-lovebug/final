import { routes } from '../../../shared/config/routes'
import type { WorkspaceId } from '../../../shared/types/ids'
import type { Notification } from './types'

// D-46: 응답에 버튼 문구·이동 경로가 없다 — targetType·targetId에서 화면이 파생한다.
//
// REVIEW_REQUEST는 documentReviewThread 라우트가 documentId까지 요구하는데(문서 리뷰와
// 사전집 리비전이 route 구조가 다르다), 알림은 reviewRequestId만 준다. 정확한 스레드로
// 바로 보내려면 document 도메인 조회가 하나 더 필요해 지금은 목록 화면으로 보낸다.
export function resolveNotificationLink(notification: Notification, workspaceId: WorkspaceId): string {
  switch (notification.targetType) {
    case 'DOCUMENT':
      return routes.documentDetail(workspaceId, notification.targetId)
    case 'DICTIONARY':
      return routes.dictionary(workspaceId)
    case 'REVIEW_REQUEST':
      return routes.documentReviewRequests(workspaceId)
    default:
      return routes.documents(workspaceId)
  }
}
