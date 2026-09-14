import { httpClient } from '../../../shared/api/httpClient'
import type { RevisionApiResponse } from './reviewApi'

export interface DocumentRevisionDetail {
  revisionId: string
  documentId?: string
  draftDocumentId: string
  baseVersionNo: number
  proposedBody?: string
  /** 검토를 제출할 때 targetRound로 그대로 넘긴다. */
  reexamineRound: number
}

/**
 * 문서 개정안(`GET /api/review-requests/{reviewRequestId}/revision-documents`).
 *
 * 재교정이 돌면 회차마다 행이 쌓이므로 마지막 회차만 본다 — 리뷰어는 항상 최신 회차를
 * 기준으로 검토하고, 그 회차 번호가 검토 제출의 `targetRound`가 된다.
 */
export async function fetchDocumentRevision(
  reviewRequestId: string,
): Promise<DocumentRevisionDetail | null> {
  const revisions = await httpClient.get<RevisionApiResponse[]>(
    `/api/review-requests/${reviewRequestId}/revision-documents`,
  )
  const latest = revisions.reduce<RevisionApiResponse | undefined>(
    (best, current) =>
      best === undefined || current.reexamineRound > best.reexamineRound ? current : best,
    undefined,
  )
  if (!latest) return null

  return {
    revisionId: String(latest.id),
    documentId: latest.targetId === null ? undefined : String(latest.targetId),
    draftDocumentId: String(latest.draftId),
    baseVersionNo: latest.baseVersionNo,
    proposedBody: latest.proposedBody ?? undefined,
    reexamineRound: latest.reexamineRound,
  }
}
