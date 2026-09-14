import { httpClient } from '../../../shared/api/httpClient'
import type { CandidateTermStatus } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'
import { findExaminingDraftDictionaryId } from './candidateTermApi'

export interface BulkDecideInput {
  workspaceId: WorkspaceId
  candidateIds: string[]
  decision: Extract<
    CandidateTermStatus,
    'REGISTRATION_APPROVED' | 'ON_HOLD' | 'REJECTED' | 'MERGED_AS_SYNONYM'
  >
  /** decision이 REJECTED일 때 필수. */
  rejectReason?: string
  /** decision이 MERGED_AS_SYNONYM일 때 필수. */
  mergeTargetTermId?: string
}

export interface BulkDecisionFailure {
  candidateTermId: number
  code: string
  message: string
}

export interface BulkDecisionResult {
  succeeded: number[]
  failed: BulkDecisionFailure[]
}

// 여러 후보어를 한 번에 판정한다
// (`POST /api/draft-dictionaries/{id}/candidate-terms/bulk-decision`).
//
// **부분 실패가 정상 응답이다** — 성공/실패가 `succeeded`/`failed`로 갈려서 200으로 온다.
// 호출하는 쪽이 failed를 보고 사용자에게 알려야 한다(전체 실패로 취급하면 안 된다).
export async function bulkDecideCandidateTerms(
  input: BulkDecideInput,
): Promise<BulkDecisionResult> {
  const draftDictionaryId = await findExaminingDraftDictionaryId(input.workspaceId)
  if (draftDictionaryId === null) {
    throw new Error('교정 중인 사전 초안이 없습니다.')
  }

  return httpClient.post<BulkDecisionResult>(
    `/api/draft-dictionaries/${draftDictionaryId}/candidate-terms/bulk-decision`,
    {
      candidateTermIds: input.candidateIds.map(Number),
      decision: input.decision,
      rejectReason: input.rejectReason,
      mergeTargetTermId:
        input.mergeTargetTermId === undefined ? undefined : Number(input.mergeTargetTermId),
    },
  )
}
