import { httpClient } from '../../../shared/api/httpClient'
import { fetchMemberNames } from '../../../shared/api/memberNames'
import type { CandidateTermListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'
import {
  findExaminingDraftDictionaryId,
  toListItem,
  type CandidateTermApiResponse,
  type PageResponse,
} from './candidateTermApi'

// 실제 백엔드 연동(docs/API.md "후보어 등록·수정·삭제·목록").
//
// **두 단계다.** 후보어 엔드포인트는 draftDictionaryId 스코프인데 화면은 workspaceId만
// 안다 — T-INT-20으로 생긴 `GET /api/draft-dictionaries?workspaceId=`로 교정 중인 초안을
// 먼저 찾는다. 초안이 없으면(=추출을 한 번도 안 돌린 워크스페이스) 빈 배열이다.
//
// 등록자 이름은 `GET /api/members?ids=`(T-INT-18) 배치 조회로 한 번에 해석한다 —
// 후보어마다 단건 조회하면 N+1이 된다.
//
// 페이지네이션: 지금은 최대 200개까지만 조회한다(화면에 페이징 UI가 없다).
export async function fetchCandidates(
  workspaceId: WorkspaceId,
): Promise<CandidateTermListItem[]> {
  const draftDictionaryId = await findExaminingDraftDictionaryId(workspaceId)
  if (draftDictionaryId === null) return []

  const response = await httpClient.get<PageResponse<CandidateTermApiResponse>>(
    `/api/draft-dictionaries/${draftDictionaryId}/candidate-terms?page=0&size=200&sort=occurrenceCount,desc`,
  )
  if (response.content.length === 0) return []

  const nameByMemberId = await fetchMemberNames(response.content.map((c) => c.createdBy))
  return response.content.map((candidate) =>
    toListItem(
      candidate,
      candidate.createdBy === null ? '—' : (nameByMemberId.get(candidate.createdBy) ?? '—'),
    ),
  )
}
