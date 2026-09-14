import { httpClient } from '../../../shared/api/httpClient'
import type { DocumentListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

interface DocumentListApiItem {
  documentId: number
  title: string
  currentVersionNo: number
  aligned: boolean
  edited: boolean
  dictionaryVersionNo: number | null
  labels: string[]
  uploaderId: number
  createdAt: string
  updatedAt: string
}

interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// 실제 백엔드 연동(docs/API.md "문서 목록 조회", GET /api/workspaces/{workspaceId}/documents).
//
// **알려진 제약(2026-09-14, T-INT-18)**: 작성자·최종 수정자 이름을 조회할 API가 없다
// (`uploaderId`만 온다 — 작성자/최종수정자 구분도 없음). `ownerName`/`updaterName`은
// 항상 undefined로 남고 화면은 "—"로 표시한다. 라벨도 배열(최대 5개)인데 화면은 1개만
// 보여줘 첫 번째만 쓴다. `badge`는 `aligned`/`edited`에서 유도한다.
//
// 페이지네이션: 지금은 최대 100개까지만 조회한다(size=100, 화면에 페이징 UI 없음).
export async function fetchDocuments(workspaceId: WorkspaceId): Promise<DocumentListItem[]> {
  const response = await httpClient.get<PageResponse<DocumentListApiItem>>(
    `/api/workspaces/${workspaceId}/documents?page=0&size=100&sort=createdAt,desc`,
  )
  return response.content.map((doc) => ({
    id: String(doc.documentId),
    workspaceId,
    title: doc.title,
    content: '', // 목록 응답엔 본문이 없다(docs/API.md 664행) — 상세 조회에서만 채워진다.
    currentVersionNo: doc.currentVersionNo,
    aligned: doc.aligned,
    edited: doc.edited,
    dictionaryVersionNo: doc.dictionaryVersionNo,
    createdAt: doc.createdAt,
    updatedAt: doc.updatedAt,
    label: doc.labels[0] ? { id: doc.labels[0], name: doc.labels[0] } : undefined,
    badge: doc.edited ? 'danger' : doc.aligned ? undefined : 'warn',
  }))
}
