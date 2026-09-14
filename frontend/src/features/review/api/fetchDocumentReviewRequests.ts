import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_REVIEW_REQUEST_FIXTURES, DOCUMENT_REVISION_FIXTURES } from '../model/reviewRequestFixtures'
import type { ReviewRequestStatus } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface DocumentReviewRequestListItem {
  reviewRequestId: string
  documentId: string
  title: string
  status: ReviewRequestStatus
  reviewerCount: number
  createdAt: string
}

// review 도메인 데이터만 반환한다 — 문서 제목 등 document 도메인 정보와 합치는 건
// 여기서 하지 않는다(features 간 직접 참조 금지, frontend/docs/ARCHITECTURE.md 참고).
// 화면(페이지)에서 features/document의 useDocuments와 함께 조합해서 쓴다.
export async function fetchDocumentReviewRequests(
  workspaceId: WorkspaceId,
): Promise<DocumentReviewRequestListItem[]> {
  await delay()

  const revisionByRequestId = new Map(
    DOCUMENT_REVISION_FIXTURES.map((revision) => [revision.reviewRequestId, revision]),
  )

  return DOCUMENT_REVIEW_REQUEST_FIXTURES.filter(
    (request) => request.workspaceId === workspaceId && request.type === 'DOCUMENT',
  )
    .map((request) => {
      const revision = revisionByRequestId.get(request.id)
      return revision
        ? {
            reviewRequestId: request.id,
            documentId: revision.documentId,
            title: request.title,
            status: request.status,
            reviewerCount: request.reviewers.length,
            createdAt: request.createdAt,
          }
        : null
    })
    .filter((item): item is DocumentReviewRequestListItem => item !== null)
}
