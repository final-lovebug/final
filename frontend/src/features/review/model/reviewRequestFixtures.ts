import type { ReviewRequest, Reviewer, RevisionDocument } from './types'

// doc-plan이 "초안 완료 → 리뷰 요청" 단계를 이미 거쳐 "개정안" 단계에 있다는 것을 나타내는
// 픽스처. features/document/model/suggestionFixtures.ts에서 doc-plan의 제안 7건을 전부
// 처리 완료 상태로 둔 것과 짝을 이룬다. features/review/api/requestDocumentReview.ts
// (mutation)가 다른 문서(예: doc-retention)에 대해 여기에 새 항목을 추가한다.
export const DOCUMENT_REVIEW_REQUEST_ID = 'reviewreq-doc-plan-1'
const REVISION_DOCUMENT_ID = 'revision-doc-plan-1'

export const REVIEWER_FIXTURES: Reviewer[] = [
  { id: 'reviewer-1', reviewRequestId: DOCUMENT_REVIEW_REQUEST_ID, memberId: 'member-mock-kim-dev', required: true, assignedAt: '2026-09-01T00:00:00.000Z', createdAt: '2026-09-01T00:00:00.000Z', createdBy: 'member-mock-park-pm', updatedAt: '2026-09-01T00:00:00.000Z' },
  { id: 'reviewer-2', reviewRequestId: DOCUMENT_REVIEW_REQUEST_ID, memberId: 'member-mock-lee-be', required: true, assignedAt: '2026-09-01T00:00:00.000Z', createdAt: '2026-09-01T00:00:00.000Z', createdBy: 'member-mock-park-pm', updatedAt: '2026-09-01T00:00:00.000Z' },
]

export const DOCUMENT_REVIEW_REQUEST_FIXTURES: ReviewRequest[] = [
  {
    id: DOCUMENT_REVIEW_REQUEST_ID,
    workspaceId: 'potenup_be',
    type: 'DOCUMENT',
    revisionId: REVISION_DOCUMENT_ID,
    title: '이용자→구독자 등 치환 12건 반영',
    requesterId: 'member-mock-park-pm',
    reviewers: REVIEWER_FIXTURES,
    status: 'IN_REVIEW',
    createdAt: '2026-09-01T00:00:00.000Z',
    createdBy: 'member-mock-park-pm',
    updatedAt: '2026-09-01T02:00:00.000Z',
  },
]

export const DOCUMENT_REVISION_FIXTURES: RevisionDocument[] = [
  {
    id: REVISION_DOCUMENT_ID,
    reviewRequestId: DOCUMENT_REVIEW_REQUEST_ID,
    documentId: 'doc-plan',
    baseVersionNo: 3,
    draftDocumentId: 'draft-doc-plan',
    proposedBody: '이용자→구독자 등 치환 12건이 반영된 본문(목데이터 단계라 실제 diff 본문은 생략)',
    reexamineRound: 0,
    createdAt: '2026-09-01T00:00:00.000Z',
    createdBy: 'member-mock-park-pm',
    updatedAt: '2026-09-01T00:00:00.000Z',
  },
]
