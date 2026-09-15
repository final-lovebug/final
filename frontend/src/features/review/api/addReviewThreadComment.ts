import { httpClient } from '../../../shared/api/httpClient'
import type { Comment } from '../model/types'
import type { TextRange } from '../../../shared/types/common'
import { toComment, type CommentApiResponse } from './reviewApi'

export interface AddReviewThreadCommentInput {
  /** 이미 제출된 검토의 id. 코멘트는 검토에 매달린다. */
  reviewId: string
  content: string
  anchor?: TextRange
  /** 사전 개정안이면 후보어 id. */
  targetItemId?: string
  /** 답글이면 부모 코멘트 id. */
  parentId?: string
}

/**
 * 이미 제출된 검토에 코멘트를 **추가로** 단다(`POST /api/reviews/{reviewId}/comments`).
 *
 * 처음 검토를 제출할 때의 코멘트는 여기가 아니라 `submitReview`가 verdict와 함께 한
 * 트랜잭션으로 보낸다(`D-63`). 이 함수는 답글이나 뒤늦은 보충 의견용이다.
 *
 * 작성자는 인증 주체에서 해석되므로 보내지 않는다.
 */
export async function addReviewThreadComment(
  input: AddReviewThreadCommentInput,
): Promise<Comment> {
  const response = await httpClient.post<CommentApiResponse>(
    `/api/reviews/${input.reviewId}/comments`,
    {
      content: input.content,
      anchor: input.anchor,
      targetItemId: input.targetItemId === undefined ? undefined : Number(input.targetItemId),
      parentId: input.parentId === undefined ? undefined : Number(input.parentId),
    },
  )
  return toComment(response)
}
