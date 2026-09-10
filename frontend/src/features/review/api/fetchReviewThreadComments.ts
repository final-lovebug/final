import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_REVIEW_THREAD_COMMENTS, type CommentListItem } from '../model/fixtures'
import { DOCUMENT_REVIEW_REQUEST_ID } from '../model/reviewRequestFixtures'

// 지금은 미리 만들어둔 doc-plan 리뷰 요청 하나에만 실제 댓글 목데이터가 있다. 새로 생성된
// 리뷰 요청(requestDocumentReview mutation)은 아직 댓글이 없는 게 맞으므로 빈 배열을 준다.
export async function fetchReviewThreadComments(
  reviewId: string,
): Promise<CommentListItem[]> {
  await delay()
  return reviewId === DOCUMENT_REVIEW_REQUEST_ID ? DOCUMENT_REVIEW_THREAD_COMMENTS : []
}
