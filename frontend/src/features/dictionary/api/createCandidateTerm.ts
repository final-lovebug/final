import { httpClient } from '../../../shared/api/httpClient'
import type { CandidateTermListItem } from '../model/fixtures'
import type { CandidateTermType } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'
import {
  findOngoingDraftDictionaryId,
  toListItem,
  type CandidateTermApiResponse,
} from './candidateTermApi'

export interface CreateCandidateTermInput {
  workspaceId: WorkspaceId
  form: string
  type: CandidateTermType
}

/**
 * 사전집 초안 화면의 "+"로 직접 등록하는 후보어
 * (`POST /api/draft-dictionaries/{id}/candidate-terms`).
 *
 * 문서에서 추출된 게 아니라 사람이 바로 적은 것이라 occurredDocumentIds·contextSnippets·
 * variantForms를 비워 보낸다. `occurrenceCount`는 1 미만이면 백엔드가 거절하므로 1이다
 * (docs/API.md "후보어 등록·수정·삭제·목록").
 */
export async function createCandidateTerm(
  input: CreateCandidateTermInput,
): Promise<CandidateTermListItem> {
  const draftDictionaryId = await findOngoingDraftDictionaryId(input.workspaceId)
  if (draftDictionaryId === null) {
    throw new Error('교정 중인 사전 초안이 없습니다. 먼저 용어 추출을 실행해 주세요.')
  }

  const response = await httpClient.post<CandidateTermApiResponse>(
    `/api/draft-dictionaries/${draftDictionaryId}/candidate-terms`,
    {
      form: input.form,
      occurrenceCount: 1,
      occurredDocumentIds: [],
      contextSnippets: [],
      variantForms: [],
      type: input.type,
    },
  )
  return toListItem(response)
}
