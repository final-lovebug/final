import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_REVIEW_THREAD_COMMENTS, type CommentListItem } from '../model/fixtures'

export async function fetchReviewThreadComments(
  reviewId: string,
): Promise<CommentListItem[]> {
  await delay()
  return DOCUMENT_REVIEW_THREAD_COMMENTS.filter((comment) => comment.reviewId === reviewId)
}
