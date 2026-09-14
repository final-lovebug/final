import { httpClient } from '../../../shared/api/httpClient'
import type { CandidateTermListItem } from '../model/fixtures'
import { toListItem, type CandidateTermApiResponse } from './candidateTermApi'

// 후보어 판정(docs/API.md "후보어 판정"). 상태마다 엔드포인트가 갈리고 필요한 본문이
// 달라서 한 개의 PATCH로 합칠 수 없다 — 거절은 사유가, 동의어 편입은 대상 용어가 필수다.
//
// 판정은 초안이 EXAMINING일 때만 가능하다. 교정 완료 뒤에 부르면 백엔드가
// DRAFT_DICTIONARY_NOT_EXAMINABLE로 거절한다.

/** 표준어로 등재 승인. `proposedDefinition`이 비어 있으면 백엔드가 거절한다. */
export async function approveCandidateTerm(
  candidateId: string,
  ownerName?: string,
): Promise<CandidateTermListItem> {
  const response = await httpClient.post<CandidateTermApiResponse>(
    `/api/candidate-terms/${candidateId}/registration-approval`,
  )
  return toListItem(response, ownerName ?? '—')
}

/** 판단 보류 — 이번 회차에서 결론을 내지 않는다. */
export async function holdCandidateTerm(
  candidateId: string,
  ownerName?: string,
): Promise<CandidateTermListItem> {
  const response = await httpClient.post<CandidateTermApiResponse>(
    `/api/candidate-terms/${candidateId}/hold`,
  )
  return toListItem(response, ownerName ?? '—')
}

/** 사전에 올리지 않는다. 사유가 필수다. */
export async function rejectCandidateTerm(
  candidateId: string,
  rejectReason: string,
  ownerName?: string,
): Promise<CandidateTermListItem> {
  const response = await httpClient.post<CandidateTermApiResponse>(
    `/api/candidate-terms/${candidateId}/rejection`,
    { rejectReason },
  )
  return toListItem(response, ownerName ?? '—')
}

/** 이미 있는 표준 용어의 동의어로 편입한다. 대상 용어 id가 필수다. */
export async function mergeCandidateTermAsSynonym(
  candidateId: string,
  mergeTargetTermId: string,
  ownerName?: string,
): Promise<CandidateTermListItem> {
  const response = await httpClient.post<CandidateTermApiResponse>(
    `/api/candidate-terms/${candidateId}/synonym-merge`,
    { mergeTargetTermId: Number(mergeTargetTermId) },
  )
  return toListItem(response, ownerName ?? '—')
}
