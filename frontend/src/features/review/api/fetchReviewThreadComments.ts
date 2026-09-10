import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_REVIEW_THREAD_COMMENTS, type CommentListItem } from '../model/fixtures'

// reviewId는 지금 목데이터 1건뿐이라 실제로 구분해 조회하지 않는다. 실 API 연동 시
// reviewId 기준으로 필터링하도록 바꾸면 된다.
export async function fetchReviewThreadComments(
  _reviewId: string,
): Promise<CommentListItem[]> {
  await delay()
  return DOCUMENT_REVIEW_THREAD_COMMENTS
}
