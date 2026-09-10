import { delay } from '../../../shared/lib/delay'
import {
  DOCUMENT_REVIEW_REQUEST_FIXTURES,
  DOCUMENT_REVISION_FIXTURES,
} from '../model/reviewRequestFixtures'
import type { ReviewRequest, Reviewer } from '../model/types'

export interface RequestDocumentReviewInput {
  workspaceId: string
  documentId: string
  title: string
  requesterId: string
  reviewerMemberIds: string[]
}

// "초안 완료 → 리뷰 요청 → 개정안" 전이를 흉내내는 목업 mutation. 제안이 전부 처리됐는지
// 검증하는 건 호출하는 쪽(DocumentReviewPage, useSuggestions 결과 기준)에서 이미 버튼을
// 막아뒀으므로 여기서 다시 하지 않는다 — 실제 백엔드라면 서버에서도 검증해야 한다
// (docs/DOMAIN.md "해당 문서가 리뷰 요청 중이면 초안을 생성할 수 없다"와 짝을 이루는 규칙).
export async function requestDocumentReview(
  input: RequestDocumentReviewInput,
): Promise<ReviewRequest> {
  await delay(300)

  const reviewRequestId = `reviewreq-${crypto.randomUUID()}`
  const revisionId = `revision-${crypto.randomUUID()}`
  const now = new Date().toISOString()

  const reviewers: Reviewer[] = input.reviewerMemberIds.map((memberId) => ({
    id: `reviewer-${crypto.randomUUID()}`,
    reviewRequestId,
    memberId,
    required: true,
    assignedAt: now,
    createdAt: now,
    createdBy: input.requesterId,
    updatedAt: now,
  }))

  const reviewRequest: ReviewRequest = {
    id: reviewRequestId,
    workspaceId: input.workspaceId,
    type: 'DOCUMENT',
    revisionId,
    title: input.title,
    requesterId: input.requesterId,
    reviewers,
    status: 'IN_REVIEW',
    createdAt: now,
    createdBy: input.requesterId,
    updatedAt: now,
  }

  DOCUMENT_REVIEW_REQUEST_FIXTURES.push(reviewRequest)
  DOCUMENT_REVISION_FIXTURES.push({
    id: revisionId,
    reviewRequestId,
    documentId: input.documentId,
    baseVersionNo: 0,
    draftDocumentId: `draft-${input.documentId}`,
    proposedBody: '',
    reexamineRound: 0,
    createdAt: now,
    createdBy: input.requesterId,
    updatedAt: now,
  })

  return reviewRequest
}
