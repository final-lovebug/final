import { httpClient } from '../../../shared/api/httpClient'
import type { CandidateTermListItem } from '../model/fixtures'
import type { CandidateTermType } from '../model/types'
import { toListItem, type CandidateTermApiResponse } from './candidateTermApi'

export interface UpdateCandidateTermInput {
  candidateId: string
  /** words 중 표준어로 고른 것 — 백엔드에서는 대표 표기(form) 자체를 바꾸는 일이다. */
  selectedWord?: string
  proposedDefinition?: string
  type?: CandidateTermType
}

// 실제 백엔드 연동(`PATCH /api/candidate-terms/{candidateTermId}`). 넘긴 필드만 바뀐다.
//
// 표준어 선택은 별도 필드가 아니라 `form` 교체다 — 백엔드는 대표 표기 하나(form)와 표기
// 묶음(variantForms)을 갖고, 묶음 중 무엇이 대표인지가 곧 표준어다.
export async function updateCandidateTerm(
  input: UpdateCandidateTermInput,
): Promise<CandidateTermListItem> {
  const response = await httpClient.patch<CandidateTermApiResponse>(
    `/api/candidate-terms/${input.candidateId}`,
    {
      form: input.selectedWord,
      proposedDefinition: input.proposedDefinition,
      type: input.type,
    },
  )
  return toListItem(response)
}
