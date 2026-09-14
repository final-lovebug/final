import { httpClient } from '../../../shared/api/httpClient'

// 리뷰 요청의 나머지 생명주기 — 재교정·반영·취소.

export interface PerformReexamineInput {
  reviewRequestId: string
  /** 문서 개정안이면 고친 본문. 사전 개정안은 후보어를 직접 고치므로 비운다. */
  proposedBody?: string
  /** 이번 재교정에서 처리한 코멘트들. 무엇을 반영했는지 기록으로 남는다. */
  addressedCommentIds: string[]
}

export interface ReexamineResult {
  round: number
  performedAt: string
}

export interface ReviseResult {
  resultVersionNo: number
  performedAt: string
}

/**
 * 재교정(`POST /api/review-requests/{id}/reexaminations`).
 *
 * `CHANGES_REQUESTED`가 된 뒤 요청자가 코멘트를 반영했다고 알리는 단계다. 회차가 하나
 * 올라가고, 리뷰어는 새 회차(`targetRound`)를 기준으로 다시 검토한다.
 */
export async function performReexamine(input: PerformReexamineInput): Promise<ReexamineResult> {
  return httpClient.post<ReexamineResult>(
    `/api/review-requests/${input.reviewRequestId}/reexaminations`,
    {
      proposedBody: input.proposedBody,
      addressedCommentIds: input.addressedCommentIds.map(Number),
    },
  )
}

/**
 * 반영(`POST /api/review-requests/{id}/revision`).
 *
 * 정족수를 채운 뒤 실제로 문서·사전집에 반영해 새 버전을 발행한다. 응답의
 * `resultVersionNo`가 새로 발행된 버전 번호다. 자격은 `fetchReviewProgress`의
 * `reviseEligible`로 미리 확인할 수 있다.
 */
export async function performRevise(reviewRequestId: string): Promise<ReviseResult> {
  return httpClient.post<ReviseResult>(`/api/review-requests/${reviewRequestId}/revision`)
}

/** 리뷰 요청 취소(`POST /api/review-requests/{id}/cancellation`). */
export async function cancelReviewRequest(reviewRequestId: string): Promise<void> {
  await httpClient.post<void>(`/api/review-requests/${reviewRequestId}/cancellation`)
}
