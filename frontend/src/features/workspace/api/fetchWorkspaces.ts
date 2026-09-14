import { httpClient } from '../../../shared/api/httpClient'
import type { ParticipantPermission, Workspace } from '../model/types'

interface WorkspaceApiItem {
  workspaceId: number
  name: string
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  myPermission: ParticipantPermission
  createdAt: string
}

// 실제 백엔드 연동(docs/API.md "참여 중인 워크스페이스 목록 조회", GET /api/workspaces).
// 참여 중인 워크스페이스만 내려오고, 페이징 규격의 명시적 예외로 배열을 그대로 반환한다
// (한 회원이 참여하는 워크스페이스 수가 구조적으로 작기 때문 — API.md 495행).
export async function fetchWorkspaces(): Promise<Workspace[]> {
  const items = await httpClient.get<WorkspaceApiItem[]>('/api/workspaces')
  return items.map((item) => ({
    id: String(item.workspaceId),
    name: item.name,
    createdAt: item.createdAt,
    myPermission: item.myPermission,
  }))
}
