import { httpClient } from '../../../shared/api/httpClient'
import type { Review, ReviewProgress, ReviewVerdict } from '../model/types'
import type { TextRange } from '../../../shared/types/common'
import { toReview, type ReviewApiResponse, type ReviewProgressApiResponse } from './reviewApi'

/** 제출 전까지 프론트 로컬 상태에만 쌓아 두는 코멘트 초안. */
export interface DraftComment {
  content: string
  anchor?: TextRange
  /** 사전 개정안이면 후보어 id. */
  targetItemId?: string
  parentId?: string
}

export interface SubmitReviewInput {
  reviewRequestId: string
  /** 어느 재교정 회차를 보고 검토했는지. 개정안의 `reexamineRound`를 그대로 넘긴다. */
  targetRound: number
  verdict: ReviewVerdict
  comments?: DraftComment[]
}

/**
 * 검토 제출(`POST /api/review-requests/{reviewRequestId}/reviews`).
 *
 * **verdict와 코멘트를 한 번에, 한 트랜잭션으로 보낸다**(`D-63`). 리뷰어가 화면에서
 * 단어별·위치별 코멘트를 다는 동안은 서버에 아무것도 보내지 않고 프론트 로컬 상태에만
 * 쌓아 둔다(GitHub의 "pending review"를 프론트가 들고 있는 셈), Approve / Change request를
 * 고르는 순간 여기서 한 번에 제출한다.
 *
 * 제출과 동시에 백엔드가 정족수를 재계산해 `ReviewRequest.status`를 갱신한다 —
 * `CHANGES_REQUESTED`가 하나라도 있으면 즉시 전환되고, 없이 정족수를 채우면 `APPROVED`다.
 */
export async function submitReview(input: SubmitReviewInput): Promise<Review> {
  const response = await httpClient.post<ReviewApiResponse>(
    `/api/review-requests/${input.reviewRequestId}/reviews`,
    {
      targetRound: input.targetRound,
      verdict: input.verdict,
      comments: (input.comments ?? []).map((comment) => ({
        content: comment.content,
        anchor: comment.anchor,
        targetItemId: comment.targetItemId === undefined ? undefined : Number(comment.targetItemId),
        parentId: comment.parentId === undefined ? undefined : Number(comment.parentId),
      })),
    },
  )
  return toReview(response)
}

/** 이미 제출된 검토 목록(`GET /api/review-requests/{id}/reviews`). */
export async function fetchReviews(reviewRequestId: string): Promise<Review[]> {
  const response = await httpClient.get<ReviewApiResponse[]>(
    `/api/review-requests/${reviewRequestId}/reviews`,
  )
  return response.map(toReview)
}

/**
 * 정족수 진행 상황(`GET /api/review-requests/{id}/review-progress`).
 *
 * `requiredReviewerCount`는 지정 리뷰어 수가 아니라 워크스페이스 룰셋 값이다(`G-4`).
 * `reviseEligible`이 true면 반영(`performRevise`)을 부를 수 있다.
 */
export async function fetchReviewProgress(reviewRequestId: string): Promise<ReviewProgress> {
  return httpClient.get<ReviewProgressApiResponse>(
    `/api/review-requests/${reviewRequestId}/review-progress`,
  )
}
