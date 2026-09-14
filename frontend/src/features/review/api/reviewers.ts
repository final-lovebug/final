import { httpClient } from '../../../shared/api/httpClient'
import type { Reviewer } from '../model/types'
import { toReviewer, type ReviewerApiResponse } from './reviewApi'

// 리뷰어 지정(`/api/review-requests/{reviewRequestId}/reviewers`).
//
// **지정은 알림 대상을 정하는 것이지 검토 자격을 정하는 게 아니다** — 정족수는 워크스페이스
// 룰셋의 `requiredReviewerCount`로 판정하고, 지정되지 않은 참여자도 검토를 제출할 수 있다(`G-4`).

export async function fetchReviewers(reviewRequestId: string): Promise<Reviewer[]> {
  const response = await httpClient.get<ReviewerApiResponse[]>(
    `/api/review-requests/${reviewRequestId}/reviewers`,
  )
  return response.map(toReviewer)
}

export async function assignReviewer(
  reviewRequestId: string,
  memberId: string,
): Promise<Reviewer> {
  const response = await httpClient.post<ReviewerApiResponse>(
    `/api/review-requests/${reviewRequestId}/reviewers`,
    { memberId: Number(memberId) },
  )
  return toReviewer(response)
}

export async function removeReviewer(
  reviewRequestId: string,
  reviewerId: string,
): Promise<void> {
  await httpClient.delete<void>(
    `/api/review-requests/${reviewRequestId}/reviewers/${reviewerId}`,
  )
}
