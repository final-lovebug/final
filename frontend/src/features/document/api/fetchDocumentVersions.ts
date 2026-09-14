import { httpClient } from '../../../shared/api/httpClient'
import { fetchMemberNames } from '../../../shared/api/memberNames'
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
// **목록 응답엔 본문(`body`)이 없다** — 단건 조회에만 있어서 버전 비교는
// `fetchDocumentVersionBody`로 따로 읽는다(아래). 최대 50개까지만 조회한다(페이징 UI 없음).
// 발행자 이름은 `GET /api/members?ids=`(T-INT-18) 배치 조회로 한 번에 해석한다.
export async function fetchDocumentVersions(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
): Promise<DocumentVersion[]> {
  const response = await httpClient.get<PageResponse<VersionApiItem>>(
    `/api/workspaces/${workspaceId}/documents/${documentId}/versions?page=0&size=50`,
  )
  if (response.content.length === 0) return []

  const nameByMemberId = await fetchMemberNames(response.content.map((v) => v.publishedBy))
  return response.content.map((version) => ({
    id: `${documentId}-v${version.versionNo}`,
    documentId,
    versionNo: version.versionNo,
    publishedAt: version.publishedAt,
    dictionaryVersionNo: version.dictionaryVersionNo ?? undefined,
    edited: version.edited,
    publishedByName: nameByMemberId.get(version.publishedBy) ?? '—',
  }))
}

interface VersionDetailApiResponse extends VersionApiItem {
  body: string
}

/**
 * 특정 버전의 본문(`GET .../versions/{versionNo}`).
 *
 * 목록 응답에는 본문이 없어 버전 비교를 하려면 이 단건 조회가 필요하다 — 이력 화면이
 * 두 버전을 골라 각각 한 번씩 부른다.
 */
export async function fetchDocumentVersionBody(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
  versionNo: number,
): Promise<string> {
  const response = await httpClient.get<VersionDetailApiResponse>(
    `/api/workspaces/${workspaceId}/documents/${documentId}/versions/${versionNo}`,
  )
  return response.body
}
