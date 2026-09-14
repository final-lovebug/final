import { httpClient } from '../../../shared/api/httpClient'
import type { Comment } from '../model/types'
import { toComment, type CommentApiResponse } from './reviewApi'

export interface FetchReviewThreadCommentsOptions {
  resolved?: boolean
  /** 사전 개정안에서 후보어 하나에 달린 코멘트만 보고 싶을 때. */
  targetItemId?: string
}

/**
 * 코멘트 조회(`GET /api/review-requests/{reviewRequestId}/comments`).
 *
 * **인자가 reviewId가 아니라 reviewRequestId다.** 코멘트는 검토(Review)에 매달려 있지만
 * 조회는 리뷰 요청 단위다 — 한 리뷰 요청에 여러 리뷰어의 검토가 붙기 때문이다. 반대로
 * 작성은 reviewId 기준이다(`addReviewThreadComment` 참고).
 *
 * 응답은 답글이 `children`으로 중첩된 트리다.
 */
export async function fetchReviewThreadComments(
  reviewRequestId: string,
  options: FetchReviewThreadCommentsOptions = {},
): Promise<Comment[]> {
  const params = new URLSearchParams()
  if (options.resolved !== undefined) params.set('resolved', String(options.resolved))
  if (options.targetItemId !== undefined) params.set('targetItemId', options.targetItemId)
  const query = params.toString()

  const response = await httpClient.get<CommentApiResponse[]>(
    `/api/review-requests/${reviewRequestId}/comments${query ? `?${query}` : ''}`,
  )
  return response.map(toComment)
}
