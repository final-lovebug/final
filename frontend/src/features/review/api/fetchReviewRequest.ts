import { httpClient } from '../../../shared/api/httpClient'
import type { ReviewRequest } from '../model/types'
import { toReviewRequest, type ReviewRequestApiResponse } from './reviewApi'

/** 리뷰 요청 단건(`GET /api/review-requests/{reviewRequestId}`). */
export async function fetchReviewRequest(reviewRequestId: string): Promise<ReviewRequest> {
  const response = await httpClient.get<ReviewRequestApiResponse>(
    `/api/review-requests/${reviewRequestId}`,
  )
  return toReviewRequest(response)
}
