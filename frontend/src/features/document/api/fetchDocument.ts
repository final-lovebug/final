import { httpClient } from '../../../shared/api/httpClient'
import type { DocumentListItem } from '../model/fixtures'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

interface DocumentApiResponse {
  documentId: number
  workspaceId: number
  title: string
  content: string
  currentVersionNo: number
  aligned: boolean
  edited: boolean
  dictionaryVersionNo: number | null
  labels: string[]
  uploaderId: number
  createdAt: string
  updatedAt: string
}

// 실제 백엔드 연동(docs/API.md "문서 상세 조회", GET /api/workspaces/{workspaceId}/documents/
// {documentId}). 참여자가 아니거나 삭제된 문서는 404(DOCUMENT_NOT_FOUND)가 예외로 던져진다.
//
// **시그니처가 바뀌었다** — 실 엔드포인트가 workspaceId도 요구해서 `(documentId)`에서
// `(workspaceId, documentId)`로 변경(목업은 documentId만으로 전체 픽스처를 뒤졌었다).
// `useDocument` 훅과 호출부(`DocumentDetailPage`·`DocumentReviewThreadPage`)도 함께 수정.
//
// **알려진 제약(T-INT-18)**: ownerName/updaterName은 항상 undefined — "—"로 표시.
export async function fetchDocument(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
): Promise<DocumentListItem> {
  const response = await httpClient.get<DocumentApiResponse>(
    `/api/workspaces/${workspaceId}/documents/${documentId}`,
  )
  return mapDocument(response)
}

function mapDocument(response: DocumentApiResponse): DocumentListItem {
  return {
    id: String(response.documentId),
    workspaceId: String(response.workspaceId) as WorkspaceId,
    title: response.title,
    content: response.content,
    currentVersionNo: response.currentVersionNo,
    aligned: response.aligned,
    edited: response.edited,
    dictionaryVersionNo: response.dictionaryVersionNo,
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
    label: response.labels[0] ? { id: response.labels[0], name: response.labels[0] } : undefined,
    badge: response.edited ? 'danger' : response.aligned ? undefined : 'warn',
  }
}
