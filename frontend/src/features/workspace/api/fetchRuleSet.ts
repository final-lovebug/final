import { httpClient } from '../../../shared/api/httpClient'
import type { RuleSet } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

interface WorkspaceApiResponse {
  workspaceId: number
  name: string
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  myPermission: string
  createdAt: string
}

// 실제 백엔드 연동. 룰셋은 별도 엔티티로 조회되지 않는다 — 워크스페이스 상세
// (docs/API.md "워크스페이스 상세 조회", GET /api/workspaces/{workspaceId})가 두 카운트를
// 함께 내려주므로 그걸 룰셋 모양으로 추려낸다. 요청자가 참여자가 아니면 404
// (WORKSPACE_NOT_FOUND)가 그대로 던져진다 — 목업의 `undefined` 반환과 달리 예외로 나타난다.
export async function fetchRuleSet(workspaceId: WorkspaceId): Promise<RuleSet> {
  const response = await httpClient.get<WorkspaceApiResponse>(`/api/workspaces/${workspaceId}`)
  return {
    workspaceId,
    requiredDocumentReviewerCount: response.requiredDocumentReviewerCount,
    requiredDictionaryReviewerCount: response.requiredDictionaryReviewerCount,
  }
}
