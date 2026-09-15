import { httpClient } from '../../../shared/api/httpClient'
import { findOngoingDraftDictionaryId } from './candidateTermApi'
import type { WorkspaceId } from '../../../shared/types/ids'

// 사전 초안 → 리뷰 요청(ui/main.js 시절의 「개정안 제출」 자리).
//
// **두 단계다**(docs/API.md «후보어 판정과 교정완료»: "교정 완료와 리뷰 요청은 별도 단계다").
// ① `POST /api/draft-dictionaries/{id}/examine-completion` — 초안을 EXAMINED 로 올린다.
// ② `POST /api/draft-dictionaries/{id}/review-request` — 리뷰 요청과 최초 개정안을 한
//    트랜잭션에서 만든다(`D-44`).
//
// **준비 조건을 프런트가 다시 검사하지 않는다**(`docs/plan/DRAFT_PLAN.md`). 예전에는
// 사전 초안의 `examine-progress`로 미판정이 0건인지 먼저 확인했는데, 판정을 걷어낸 뒤 그 값이
// 늘 후보어 전체 수가 되어 뜻을 잃었다 — **그래서 그 엔드포인트 자체를 지웠다**(`D-87`).
// 조건은 「모든 후보어가 대표어와 정의를 가졌는가」로 바뀌었고 `fetchDictionaryDraft`가
// 계산해 화면이 버튼으로 막는다. 최종 판정은 백엔드에 맡기고 에러 메시지를 그대로 올린다.

export interface SubmitDictionaryRevisionInput {
  workspaceId: WorkspaceId
  title: string
  description?: string
  /** 지정할 리뷰어. 비어 있어도 리뷰는 진행된다(정족수는 룰셋 기준, `G-4`). */
  reviewerMemberIds?: string[]
}

export interface SubmittedRevision {
  reviewRequestId: string
}

export async function submitDictionaryRevision(
  input: SubmitDictionaryRevisionInput,
): Promise<SubmittedRevision> {
  const draftDictionaryId = await findOngoingDraftDictionaryId(input.workspaceId)
  if (draftDictionaryId === null) {
    throw new Error('진행 중인 사전 초안이 없습니다.')
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
