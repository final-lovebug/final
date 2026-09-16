import { httpClient } from '../../../shared/api/httpClient'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

export interface UpdateDocumentInput {
  title: string
  /** **통째로 교체된다**(docs/API.md «문서 수정») — 빈 배열은 「라벨을 모두 뗀다」는 뜻이다. */
  labels: string[]
}

// 실제 백엔드 연동(docs/API.md «문서 수정», PATCH /api/workspaces/{workspaceId}/documents/
// {documentId} → 204).
//
// **제목과 라벨만 바꾼다. 본문은 이 경로로 바뀌지 않는다** — 본문 편집은 버전을 만들고
// 제목·라벨 수정은 만들지 않아서 백엔드가 경로를 나눴다(`G-9`). 화면은 한 폼에서 둘 다
// 받지만, 저장할 때 바뀐 쪽만 골라 각자의 엔드포인트로 보낸다(`useEditDocument`).
//
// 업로드 화면과 마찬가지로 **여기가 라벨이 생기는 경로이기도 하다** — 독립 라벨 생성 API가
// 없어(T-INT-10) 목록에 없는 이름을 보내면 라벨이 새로 만들어진다.
export async function updateDocument(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
  input: UpdateDocumentInput,
): Promise<void> {
  await httpClient.patch(`/api/workspaces/${workspaceId}/documents/${documentId}`, {
    title: input.title,
    labels: input.labels,
  })
}
