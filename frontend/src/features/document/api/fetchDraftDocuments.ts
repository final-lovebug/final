import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
import type { Page } from '../../../shared/types/common'
import {
  toDraftDocument,
  type DraftDocumentApiResponse,
} from './draftDocumentApi'
import { fetchDocuments } from './fetchDocuments'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface DraftDocumentListItem {
  documentId: string
  draftDocumentId: string
  title: string
  pendingCount: number
  updatedAt: string
}

interface ExamineProgressApiResponse {
  total: number
  pending: number
  keptOrigin: number
  appliedSuggestion: number
  previewBody: string
}

/**
 * "문서 > 초안" 목록: 아직 교정 중(`EXAMINING`)인 문서 초안을 모은다.
 *
 * **워크스페이스 필터를 클라이언트에서 하는 이유** — `GET /api/draft-documents`는
 * `documentId`·`status`로만 거르고 `workspaceId` 파라미터가 없다(요청자가 참여한
 * 워크스페이스 전체의 초안이 돌아온다). 그래서 그 워크스페이스의 문서 목록과 교집합을
 * 취한다 — 제목도 그 목록에서 얻으므로 어차피 필요한 조회다.
 *
 * 미해결 건수는 초안마다 `examine-progress`를 한 번씩 부른다(N+1). 한 워크스페이스에서
 * 동시에 교정 중인 초안은 많아야 몇 건이라(같은 문서에 진행 중 초안은 하나뿐이다)
 * 감수한다 — 건수만 주는 배치 엔드포인트가 생기면 그때 바꾼다.
 */
export async function fetchDraftDocuments(
  workspaceId: WorkspaceId,
): Promise<DraftDocumentListItem[]> {
  const [draftResponses, documents] = await Promise.all([
    fetchAllPages<DraftDocumentApiResponse>((page, size) =>
      httpClient.get<Page<DraftDocumentApiResponse>>(
        `/api/draft-documents?status=EXAMINING&page=${page}&size=${size}&sort=updatedAt,desc`,
      ),
    ),
    fetchDocuments(workspaceId),
  ])

  const titleByDocumentId = new Map(documents.map((doc) => [doc.id, doc.title]))
  const drafts = draftResponses
    .map(toDraftDocument)
    .filter((draft) => titleByDocumentId.has(draft.documentId))

  const progresses = await Promise.all(
    drafts.map((draft) =>
      httpClient.get<ExamineProgressApiResponse>(
        `/api/draft-documents/${draft.id}/examine-progress`,
      ),
    ),
  )

  return drafts.map((draft, index) => ({
    documentId: draft.documentId,
    draftDocumentId: draft.id,
    title: titleByDocumentId.get(draft.documentId) ?? '—',
    pendingCount: progresses[index].pending,
    updatedAt: draft.updatedAt,
  }))
}
