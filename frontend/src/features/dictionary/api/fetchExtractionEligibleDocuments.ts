import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
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
// 페이지네이션: 자격 판정을 문서 전체에 대해 해야 하므로 상한 크기로 끝까지 페이징한다.
// 상한(100)에 맞춰 한 번만 부르던 동안은 101번째 이후 문서가 추출 대상 후보에서 아예
// 빠졌다.
export async function fetchExtractionEligibleDocuments(
  workspaceId: WorkspaceId,
): Promise<ExtractionEligibleDocument[]> {
  const documents = await fetchAllPages<DocumentListItem>((page, size) =>
    httpClient.get<PageResponse<DocumentListItem>>(
      `/api/workspaces/${workspaceId}/documents?page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  )
  return documents.map((doc) => ({
    documentId: String(doc.documentId),
    title: doc.title,
    eligible: doc.aligned,
    reason: doc.aligned ? undefined : doc.edited ? '직접 편집된 문서' : '사전집 기준과 정렬되지 않음',
  }))
}
