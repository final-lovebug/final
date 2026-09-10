import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_FIXTURES, type DocumentListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

// 목업 구현체. 지금은 픽스처가 'potenup_be' 워크스페이스 문서만 갖고 있어 다른
// 워크스페이스로 들어가면 빈 목록이 보인다 — ui/ 프로토타입도 워크스페이스 목록 화면에서
// POTENUP_BE만 클릭 가능하게 해뒀던 것과 같은 맥락이다.
export async function fetchDocuments(
  workspaceId: WorkspaceId,
): Promise<DocumentListItem[]> {
  await delay()
  return DOCUMENT_FIXTURES.filter((doc) => doc.workspaceId === workspaceId)
}
