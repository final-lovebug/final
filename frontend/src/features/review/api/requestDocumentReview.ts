import { httpClient } from '../../../shared/api/httpClient'
import type { ReviewRequest } from '../model/types'
import { toReviewRequest, type PageResponse, type ReviewRequestApiResponse } from './reviewApi'

export interface RequestDocumentReviewInput {
  documentId: string
  title: string
  description?: string
  /** 지정할 리뷰어. 비어 있어도 리뷰는 진행된다(정족수는 룰셋 기준). */
  reviewerMemberIds?: string[]
}

interface DraftDocumentApiResponse {
  draftDocumentId: number
  documentId: number
  status: string
}

/**
 * 문서 리뷰 요청 생성(`POST /api/draft-documents/{draftDocumentId}/review-request`).
 *
 * **documentId로는 부를 수 없다** — 리뷰 대상은 문서가 아니라 그 문서의 초안이다. 화면은
 * documentId만 알고 있어 `GET /api/draft-documents?documentId=`로 초안을 먼저 찾는다.
 *
 * 요청자는 인증 주체에서 해석되므로 보내지 않는다. 리뷰어는 생성 요청 본문의
 * `reviewerMemberIds`로 함께 지정된다 — 나중에 더하거나 빼는 것만
 * `POST/DELETE /api/review-requests/{id}/reviewers`를 쓴다.
 */
export async function requestDocumentReview(
  input: RequestDocumentReviewInput,
): Promise<ReviewRequest> {
  const drafts = await httpClient.get<PageResponse<DraftDocumentApiResponse>>(
    `/api/draft-documents?documentId=${input.documentId}&page=0&size=1&sort=createdAt,desc`,
  )
  const draft = drafts.content[0]
  if (!draft) {
    throw new Error('이 문서에는 리뷰를 요청할 초안이 없습니다.')
  }

  const created = await httpClient.post<ReviewRequestApiResponse>(
    `/api/draft-documents/${draft.draftDocumentId}/review-request`,
    {
      title: input.title,
      description: input.description,
      reviewerMemberIds: (input.reviewerMemberIds ?? []).map(Number),
    },
  )
  return toReviewRequest(created)
}
