import { httpClient } from '../../../shared/api/httpClient'
import type { DocumentListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface CreateDocumentInput {
  workspaceId: WorkspaceId
  title: string
  content: string
  ownerId: string
  ownerName: string
}

// docs/DOMAIN.md 정책: 업로드 파일 형식은 txt/md 뿐이고 본문은 10,000자 이내다.
// 실제 파일 업로드는 다루지 않고(텍스트 입력만) 길이 제약만 재현한다.
export const DOCUMENT_CONTENT_MAX_LENGTH = 10_000

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

// 실제 백엔드 연동(docs/API.md "문서 생성", POST /api/workspaces/{workspaceId}/documents).
// 서버도 같은 길이 제약을 검증하지만(COMMON_INVALID_REQUEST), 빠른 피드백을 위해
// 클라이언트에서 먼저 걸러낸다. 지금 업로드 폼(DocumentUploadPage)은 라벨 입력이 없어
// `labels`는 항상 빈 배열로 보낸다.
//
// 생성자는 곧 요청자 본인이라 ownerName/updaterName은 조회 없이 입력값(`input.ownerName`)
// 을 그대로 쓴다 — T-INT-18(회원 이름 조회 수단 부재)의 영향을 받지 않는다.
export async function createDocument(input: CreateDocumentInput): Promise<DocumentListItem> {
  if (input.content.length > DOCUMENT_CONTENT_MAX_LENGTH) {
    throw new Error(
      `문서 본문은 ${DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}자를 넘을 수 없습니다.`,
    )
  }

  const response = await httpClient.post<DocumentApiResponse>(
    `/api/workspaces/${input.workspaceId}/documents`,
    { title: input.title, content: input.content, labels: [] },
  )

  return {
    id: String(response.documentId),
    workspaceId: input.workspaceId,
    title: response.title,
    content: response.content,
    currentVersionNo: response.currentVersionNo,
    aligned: response.aligned,
    edited: response.edited,
    dictionaryVersionNo: response.dictionaryVersionNo,
    ownerId: input.ownerId,
    updaterId: input.ownerId,
    ownerName: input.ownerName,
    updaterName: input.ownerName,
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}
