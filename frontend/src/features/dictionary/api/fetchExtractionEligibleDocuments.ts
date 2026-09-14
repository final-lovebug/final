import { httpClient } from '../../../shared/api/httpClient'
import { type ExtractionEligibleDocument } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

interface DocumentListItem {
  documentId: number
  title: string
  currentVersionNo: number
  aligned: boolean
  edited: boolean
  dictionaryVersionNo: number | null
}

interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// 실제 백엔드 연동. "추출 대상 문서 목록" 전용 엔드포인트는 없다(docs/API.md 1500행 —
// 추출 요청 시점에 서버가 제출된 문서 중 자격 있는 것만 걸러낸다). 대신 문서 목록
// (docs/API.md "문서 목록 조회", GET /api/workspaces/{workspaceId}/documents)의 `aligned`
// 필드로 클라이언트에서 직접 고른다 — `aligned === true`가 곧 "사전집 기준에 맞춰져
// 있고 그 뒤로 편집되지 않은" 상태라 추출 대상이다(G-12, D-31).
//
// 페이지네이션: 지금은 최대 100개까지만 조회한다(size=100) — 문서가 그보다 많은
// 워크스페이스는 후속 과제(페이징 UI 또는 무한 스크롤)로 넘긴다.
export async function fetchExtractionEligibleDocuments(
  workspaceId: WorkspaceId,
): Promise<ExtractionEligibleDocument[]> {
  const response = await httpClient.get<PageResponse<DocumentListItem>>(
    `/api/workspaces/${workspaceId}/documents?page=0&size=100&sort=createdAt,desc`,
  )
  return response.content.map((doc) => ({
    documentId: String(doc.documentId),
    title: doc.title,
    eligible: doc.aligned,
    reason: doc.aligned ? undefined : doc.edited ? '직접 편집된 문서' : '사전집 기준과 정렬되지 않음',
  }))
}
