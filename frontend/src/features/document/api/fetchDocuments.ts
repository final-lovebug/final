import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
import { fetchMemberNames } from '../../../shared/api/memberNames'
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
// 작성자 이름은 `GET /api/members?ids=`(T-INT-18) 배치 조회로 한 번에 해석한다 — 문서마다
// 단건 조회하면 N+1이 된다.
//
// **`updaterName`은 여전히 채우지 않는다.** 문서 응답에는 `uploaderId`(작성자)뿐이고 최종
// 수정자에 해당하는 필드가 없다. 버전 응답의 `publishedBy`가 사실상 최종 수정자지만 목록에서
// 문서마다 버전을 조회하면 N+1이라 쓰지 않는다 — 화면은 "—"로 표시한다(후속 과제).
//
// 라벨도 배열(최대 5개)인데 화면은 1개만 보여줘 첫 번째만 쓴다. `badge`는 `aligned`/`edited`에서
// 유도한다.
//
// 페이지네이션: 화면에 페이징 UI가 없어 문서 전체가 필요하다. 상한(100)에 딱 맞춰 한 번만
// 부르고 있었는데, 문서가 100개를 넘으면 조용히 잘렸다 — 상한 크기로 끝까지 페이징한다.
export async function fetchDocuments(workspaceId: WorkspaceId): Promise<DocumentListItem[]> {
  const documents = await fetchAllPages<DocumentListApiItem>((page, size) =>
    httpClient.get<PageResponse<DocumentListApiItem>>(
      `/api/workspaces/${workspaceId}/documents?page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  )
  if (documents.length === 0) return []

  const nameByMemberId = await fetchMemberNames(documents.map((doc) => doc.uploaderId))
  return documents.map((doc) => ({
    id: String(doc.documentId),
    workspaceId,
    title: doc.title,
    content: '', // 목록 응답엔 본문이 없다(docs/API.md 664행) — 상세 조회에서만 채워진다.
    currentVersionNo: doc.currentVersionNo,
    aligned: doc.aligned,
    edited: doc.edited,
    dictionaryVersionNo: doc.dictionaryVersionNo,
    ownerName: nameByMemberId.get(doc.uploaderId) ?? '—',
    createdAt: doc.createdAt,
    updatedAt: doc.updatedAt,
    labels: doc.labels,
    badge: doc.edited ? 'danger' : doc.aligned ? undefined : 'warn',
  }))
}
