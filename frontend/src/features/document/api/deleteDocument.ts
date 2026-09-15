import { httpClient } from '../../../shared/api/httpClient'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

// 실제 백엔드 연동(docs/API.md «문서 삭제», DELETE /api/workspaces/{workspaceId}/documents/
// {documentId} → 204).
//
// **참여자면 누구나 지울 수 있다**(`CONFLICTS.md` D-92 — 2026-09-15에 ADMIN 제한을 풀었다).
// 그래서 화면이 권한으로 버튼을 감추지 않는다. 비참여자·이미 삭제된 문서는 404다.
//
// 소프트 삭제라 확정된 버전 행은 서버에 남지만, 조회가 문서에서 먼저 막히므로 화면에서는
// 되돌릴 수단이 없다 — 호출 전에 확인을 받는다.
export async function deleteDocument(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
): Promise<void> {
  await httpClient.delete(`/api/workspaces/${workspaceId}/documents/${documentId}`)
}
