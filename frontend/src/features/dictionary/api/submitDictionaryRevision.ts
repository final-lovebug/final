import { httpClient } from '../../../shared/api/httpClient'
import { findExaminingDraftDictionaryId } from './candidateTermApi'
import type { WorkspaceId } from '../../../shared/types/ids'

// 사전 초안 → 개정안 제출(ui/main.js renderDraftScreen의 「개정안 제출」).
//
// **두 단계다**(docs/API.md «후보어 판정과 교정완료»: "교정 완료와 리뷰 요청은 별도 단계다").
// ① `POST /api/draft-dictionaries/{id}/examine-completion` — 미판정 후보어가 0건일 때만
//    초안을 EXAMINED로 바꾼다.
// ② `POST /api/draft-dictionaries/{id}/review-request` — 리뷰 요청과 최초 개정안을 한
//    트랜잭션에서 만든다(`D-44`).
//
// 이미 EXAMINED인 초안에 ①을 다시 부르면 거절될 수 있으므로, 진행률을 먼저 읽어
// 미판정이 남아 있을 때만 부른다.

/** 백엔드 `ExamineProgressResponse` 그대로(docs/API.md «후보어 판정과 교정완료»). */
interface ExamineProgressApiResponse {
  total: number
  pending: number
  kept: number
  approved: number
  merged: number
  rejected: number
  onHold: number
}

/** 후보어 판정 진행률(`GET /api/draft-dictionaries/{id}/examine-progress`). */
export async function fetchCandidateExamineProgress(
  workspaceId: WorkspaceId,
): Promise<{ draftDictionaryId: string; pending: number; total: number } | null> {
  const draftDictionaryId = await findExaminingDraftDictionaryId(workspaceId)
  if (draftDictionaryId === null) return null

  const progress = await httpClient.get<ExamineProgressApiResponse>(
    `/api/draft-dictionaries/${draftDictionaryId}/examine-progress`,
  )
  return {
    draftDictionaryId: String(draftDictionaryId),
    pending: progress.pending,
    total: progress.total,
  }
}

export interface SubmitDictionaryRevisionInput {
  workspaceId: WorkspaceId
  title: string
  description?: string
  /** 지정할 리뷰어. 비어 있어도 리뷰는 진행된다(정족수는 룰셋 기준). */
  reviewerMemberIds?: string[]
}

export interface SubmittedRevision {
  reviewRequestId: string
}

export async function submitDictionaryRevision(
  input: SubmitDictionaryRevisionInput,
): Promise<SubmittedRevision> {
  const draftDictionaryId = await findExaminingDraftDictionaryId(input.workspaceId)
  if (draftDictionaryId === null) {
    throw new Error('교정 중인 사전 초안이 없습니다.')
  }

  const progress = await httpClient.get<ExamineProgressApiResponse>(
    `/api/draft-dictionaries/${draftDictionaryId}/examine-progress`,
  )
  if (progress.pending > 0) {
    throw new Error(`아직 판정하지 않은 후보어가 ${progress.pending}건 남아 있습니다.`)
  }

  await httpClient.post(`/api/draft-dictionaries/${draftDictionaryId}/examine-completion`)

  const created = await httpClient.post<{ reviewRequestId: number }>(
    `/api/draft-dictionaries/${draftDictionaryId}/review-request`,
    {
      title: input.title,
      description: input.description,
      reviewerMemberIds: (input.reviewerMemberIds ?? []).map(Number),
    },
  )
  return { reviewRequestId: String(created.reviewRequestId) }
}
