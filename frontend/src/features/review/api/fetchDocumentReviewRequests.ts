import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
import type { ReviewRequestStatus } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'
import type { PageResponse, ReviewRequestApiResponse } from './reviewApi'

export interface DocumentReviewRequestListItem {
  reviewRequestId: string
  /** 대상 문서 id. 개정안을 조인해 응답이 직접 준다(`D-63`) — 별도 조회가 필요 없다. */
  documentId: string
  title: string
  status: ReviewRequestStatus
  reviewerCount: number
  createdAt: string
}

// 실제 백엔드 연동(`GET /api/review-requests?workspaceId=&type=DOCUMENT`).
//
// review 도메인 데이터만 반환한다 — 문서 제목 등 document 도메인 정보와 합치는 건 여기서
// 하지 않는다(features 간 직접 참조 금지, frontend/docs/ARCHITECTURE.md). 다만 `title`은
// ReviewRequest 자체의 속성이라 응답에 들어 있다.
//
// 페이지네이션: 화면에 페이징 UI가 없어 전체가 필요하다. 상한(100)에 맞춰 한 번만
// 부르던 동안은 101번째 이후 리뷰 요청이 목록에서 빠졌다.
export async function fetchDocumentReviewRequests(
  workspaceId: WorkspaceId,
): Promise<DocumentReviewRequestListItem[]> {
  const requests = await fetchAllPages<ReviewRequestApiResponse>((page, size) =>
    httpClient.get<PageResponse<ReviewRequestApiResponse>>(
      `/api/review-requests?workspaceId=${workspaceId}&type=DOCUMENT&page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  )
  return requests
    .filter((request) => request.targetId !== null)
    .map((request) => ({
      reviewRequestId: String(request.reviewRequestId),
      documentId: String(request.targetId),
      title: request.title,
      status: request.status,
      reviewerCount: request.reviewerCount,
      createdAt: request.createdAt,
    }))
}
