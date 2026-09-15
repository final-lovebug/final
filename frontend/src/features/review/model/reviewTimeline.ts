import type { Comment, Review, ReviewVerdict } from './types'
import { flattenComments } from '../api/reviewApi'

/** 백엔드 LatestReviewAggregator와 같은 회원별 최신 리뷰 선택 규칙. */
export function latestReviewByMember(reviews: Review[]): Map<string, Review> {
  const latest = new Map<string, Review>()
  for (const review of reviews) {
    const previous = latest.get(review.memberId)
    if (
      previous === undefined ||
      review.submittedAt > previous.submittedAt ||
      (review.submittedAt === previous.submittedAt && review.id > previous.id)
    ) {
      latest.set(review.memberId, review)
    }
  }
  return latest
}

export interface ReviewEntry {
  review: Review
  summary?: Comment
  itemComments: Comment[]
  superseded: boolean
}

export interface ReviewGroup {
  memberId: string
  entries: ReviewEntry[]
}

function entryFor(review: Review, comments: Comment[], superseded: boolean): ReviewEntry {
  const reviewComments = flattenComments(comments).filter((comment) => comment.reviewId === review.id)
  const summaries = reviewComments.filter(
    (comment) => comment.targetItemId == null && comment.anchor == null,
  )
  return {
    review,
    summary: summaries[0],
    itemComments: reviewComments.filter((comment) => comment !== summaries[0]),
    superseded,
  }
}

/** 리뷰를 회원별로 묶고, 각 회원의 최신 리뷰를 먼저 반환한다. */
export function groupReviewsByMember(reviews: Review[], comments: Comment[]): ReviewGroup[] {
  const latest = latestReviewByMember(reviews)
  const grouped = new Map<string, ReviewEntry[]>()
  for (const review of reviews) {
    const entries = grouped.get(review.memberId) ?? []
    entries.push(entryFor(review, comments, latest.get(review.memberId)?.id !== review.id))
    grouped.set(review.memberId, entries)
  }
  return [...grouped.entries()]
    .map(([memberId, entries]) => ({
      memberId,
      entries: entries.sort((a, b) => b.review.submittedAt.localeCompare(a.review.submittedAt)),
    }))
    .sort((a, b) => b.entries[0].review.submittedAt.localeCompare(a.entries[0].review.submittedAt))
}

export function commentsForTerm(
  reviews: Review[],
  comments: Comment[],
  candidateTermId: string,
): { comment: Comment; verdict: ReviewVerdict; memberId: string }[] {
  const reviewById = new Map(reviews.map((review) => [review.id, review]))
  return flattenComments(comments)
    .filter((comment) => comment.targetItemId === candidateTermId)
    .map((comment) => {
      const review = reviewById.get(comment.reviewId)
      return review ? { comment, verdict: review.verdict, memberId: review.memberId } : undefined
    })
    .filter((item): item is { comment: Comment; verdict: ReviewVerdict; memberId: string } => item !== undefined)
    .sort((a, b) => a.comment.createdAt.localeCompare(b.comment.createdAt))
}
