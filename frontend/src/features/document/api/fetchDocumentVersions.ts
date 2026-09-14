import { httpClient } from '../../../shared/api/httpClient'
import type { DocumentVersion } from '../model/types'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

interface VersionApiItem {
  versionNo: number
  publishedAt: string
  dictionaryVersionNo: number | null
  edited: boolean
  publishedBy: number
}

interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// 실제 백엔드 연동(docs/API.md "버전 이력 조회", GET /api/workspaces/{workspaceId}/
// documents/{documentId}/versions). **시그니처가 바뀌었다** — 실 엔드포인트가
// workspaceId도 요구한다(목업은 documentId만 받았다). `useDocumentVersions` 훅과
// `DocumentHistoryPage` 호출부도 함께 수정.
//
// **알려진 제약**: 목록 응답엔 본문(`body`)이 없다(단건 조회에만 있음) — `body`는 항상
// undefined이고 화면은 "—"로 표시한다. 최대 50개까지만 조회한다(size=50, 페이징 UI 없음).
export async function fetchDocumentVersions(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
): Promise<DocumentVersion[]> {
  const response = await httpClient.get<PageResponse<VersionApiItem>>(
    `/api/workspaces/${workspaceId}/documents/${documentId}/versions?page=0&size=50`,
  )
  return response.content.map((version) => ({
    id: `${documentId}-v${version.versionNo}`,
    documentId,
    versionNo: version.versionNo,
    publishedAt: version.publishedAt,
    dictionaryVersionNo: version.dictionaryVersionNo ?? undefined,
  }))
}
