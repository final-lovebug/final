import type {
  Comment,
  Review,
  ReviewProgress,
  ReviewRequest,
  ReviewRequestStatus,
  ReviewRequestType,
  ReviewVerdict,
  Reviewer,
} from '../model/types'
import type { TextRange } from '../../../shared/types/common'

// review 도메인 api가 공유하는 실 응답 타입과 매핑.
// 백엔드 컨트롤러가 7개로 갈려 있어(ReviewRequest·DraftReviewRequest·Reviewer·Review·
// Comment·Revision·Reexamine·Revise) 엔드포인트별 경로는 각 파일에 두고, 응답 모양만 여기 모은다.

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ReviewRequestApiResponse {
  reviewRequestId: number
  workspaceId: number
  type: ReviewRequestType
  title: string
  description: string | null
  requesterId: number
  status: ReviewRequestStatus
  approvedAt: string | null
  revisedAt: string | null
  createdAt: string
  updatedAt: string
  targetId: number | null
  reviewerCount: number
}

export interface RevisionApiResponse {
  id: number
  reviewRequestId: number
  targetId: number | null
  baseVersionNo: number
  /** 문서면 draftDocumentId, 사전집이면 draftDictionaryId. */
  draftId: number
  proposedBody: string | null
  reexamineRound: number
}

export interface CommentApiResponse {
  commentId: number
  reviewId: number
  authorId: number
  content: string
  anchor: TextRange | null
  targetItemId: number | null
  parentId: number | null
  resolved: boolean
  createdAt: string
  updatedAt: string
  children: CommentApiResponse[]
}

export interface ReviewApiResponse {
  reviewId: number
  reviewRequestId: number
  memberId: number
  targetRound: number
  verdict: ReviewVerdict
  submittedAt: string
  createdAt: string
}

export interface ReviewerApiResponse {
  reviewerId: number
  reviewRequestId: number
  memberId: number
  assignedAt: string
  createdBy: number
}

export function toReviewRequest(response: ReviewRequestApiResponse): ReviewRequest {
  return {
    id: String(response.reviewRequestId),
    workspaceId: String(response.workspaceId),
    type: response.type,
    title: response.title,
    requesterId: String(response.requesterId),
    targetId: response.targetId === null ? undefined : String(response.targetId),
    reviewerCount: response.reviewerCount,
    status: response.status,
    approvedAt: response.approvedAt ?? undefined,
    revisedAt: response.revisedAt ?? undefined,
    description: response.description ?? undefined,
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}

export function toComment(response: CommentApiResponse): Comment {
  return {
    id: String(response.commentId),
    reviewId: String(response.reviewId),
    authorId: String(response.authorId),
    content: response.content,
    anchor: response.anchor ?? undefined,
    targetItemId: response.targetItemId === null ? undefined : String(response.targetItemId),
    parentId: response.parentId === null ? undefined : String(response.parentId),
    resolved: response.resolved,
    children: response.children.map(toComment),
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}

export function toReview(response: ReviewApiResponse): Review {
  return {
    id: String(response.reviewId),
    reviewRequestId: String(response.reviewRequestId),
    memberId: String(response.memberId),
    targetRound: response.targetRound,
    verdict: response.verdict,
    submittedAt: response.submittedAt,
    createdAt: response.createdAt,
  }
}

export function toReviewer(response: ReviewerApiResponse): Reviewer {
  return {
    id: String(response.reviewerId),
    reviewRequestId: String(response.reviewRequestId),
    memberId: String(response.memberId),
    assignedAt: response.assignedAt,
    createdBy: String(response.createdBy),
  }
}

export type ReviewProgressApiResponse = ReviewProgress

/** 코멘트 트리를 평평하게 편다 — 개수 세기·targetItemId 그룹핑처럼 트리가 방해되는 곳에서 쓴다. */
export function flattenComments(comments: Comment[]): Comment[] {
  return comments.flatMap((comment) => [comment, ...flattenComments(comment.children ?? [])])
}
