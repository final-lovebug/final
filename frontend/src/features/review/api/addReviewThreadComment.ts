import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_REVIEW_THREAD_COMMENTS, type CommentListItem } from '../model/fixtures'

export interface AddReviewThreadCommentInput {
  reviewId: string
  authorId: string
  authorName: string
  authorInitial: string
  content: string
}

export async function addReviewThreadComment(
  input: AddReviewThreadCommentInput,
): Promise<CommentListItem> {
  await delay(200)

  const now = new Date().toISOString()
  const created: CommentListItem = {
    id: `comment-${crypto.randomUUID()}`,
    reviewId: input.reviewId,
    authorId: input.authorId,
    authorName: input.authorName,
    authorInitial: input.authorInitial,
    authorTone: 'accent',
    mine: true,
    content: input.content,
    resolved: false,
    createdAt: now,
    createdBy: input.authorId,
    updatedAt: now,
  }

  DOCUMENT_REVIEW_THREAD_COMMENTS.push(created)
  return created
}
