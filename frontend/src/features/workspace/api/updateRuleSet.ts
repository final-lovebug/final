import { httpClient } from '../../../shared/api/httpClient'
import type { RuleSet } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface UpdateRuleSetInput {
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
}

interface WorkspaceApiResponse {
  workspaceId: number
  name: string
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
  myPermission: string
  createdAt: string
}

// 실제 백엔드 연동(docs/API.md "워크스페이스 룰셋 수정", PATCH /api/workspaces/{id}/rule-set).
// ADMIN 이상만 가능 — 권한 부족 시 403(WORKSPACE_ADMIN_REQUIRED)이 그대로 던져진다.
export async function updateRuleSet(workspaceId: WorkspaceId, input: UpdateRuleSetInput): Promise<RuleSet> {
  const response = await httpClient.patch<WorkspaceApiResponse>(
    `/api/workspaces/${workspaceId}/rule-set`,
    input,
  )
  return {
    workspaceId,
    requiredDocumentReviewerCount: response.requiredDocumentReviewerCount,
    requiredDictionaryReviewerCount: response.requiredDictionaryReviewerCount,
  }
}
