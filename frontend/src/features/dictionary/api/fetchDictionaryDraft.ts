import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
import { fetchMemberNames } from '../../../shared/api/memberNames'
import {
  findOngoingDraftDictionary,
  toListItem,
  type CandidateTermApiResponse,
  type DraftDictionaryInfo,
  type PageResponse,
} from './candidateTermApi'
import type { CandidateTermListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface DictionaryDraft {
  draft: DraftDictionaryInfo
  /** 초안 작성자 이름. 화면 상단에 **한 곳만** 표시한다(`docs/plan/DRAFT_PLAN.md`). */
  creatorName: string
  candidates: CandidateTermListItem[]
  /** 대표어와 정의가 모두 채워졌는지 — 리뷰 요청 버튼을 열어 줄 조건이다. */
  reviewReady: boolean
  /** 정의가 빈 후보어. 화면이 무엇이 막고 있는지 알려 주는 데 쓴다. */
  missingDefinition: CandidateTermListItem[]
}

/**
 * 초안 정보와 후보어 목록을 **함께** 읽는다(`docs/plan/DRAFT_PLAN.md`).
 *
 * 화면이 둘을 동시에 필요로 한다 — 상단의 작성자 한 명은 초안에서, 표와 상세 패널은 후보어에서
 * 온다. 따로 두면 초안 조회가 두 번 일어나므로(후보어 엔드포인트가 초안 id를 요구한다) 한 번에 묶는다.
 *
 * **작성자 이름은 프런트가 해석한다**(`D-62`) — 초안 응답은 `createdBy`(회원 id)만 주고,
 * 도메인별 조인 대신 공용 회원 조회(`GET /api/members?ids=`)를 쓴다는 결정을 그대로 따른다.
 *
 * 초안이 없으면 `null`이다 — 아직 용어 추출을 한 번도 돌리지 않은 워크스페이스다.
 */
export async function fetchDictionaryDraft(
  workspaceId: WorkspaceId,
): Promise<DictionaryDraft | null> {
  const draft = await findOngoingDraftDictionary(workspaceId)
  if (draft === null) return null

  // **후보어 전체를 읽어야 한다.** 첫 20건만 읽던 동안 `reviewReady`가 그 20건으로만
  // 계산돼, 21번째 후보어의 정의가 비어 있어도 화면은 "리뷰 요청 가능"으로 보였고 요청은
  // 백엔드에서 `DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED`로 거절됐다 — 화면과
  // `DraftDictionaryReviewReadinessValidator`가 같은 집합을 봐야 조건이 일치한다.
  const [candidateResponses, nameByMemberId] = await Promise.all([
    fetchAllPages<CandidateTermApiResponse>((page, size) =>
      httpClient.get<PageResponse<CandidateTermApiResponse>>(
        `/api/draft-dictionaries/${draft.id}/candidate-terms?page=${page}&size=${size}&sort=occurrenceCount,desc`,
      ),
    ),
    fetchMemberNames([Number(draft.createdBy)]),
  ])

  const candidates = candidateResponses.map(toListItem)
  const missingDefinition = candidates.filter(
    (candidate) =>
      candidate.proposedDefinition === undefined || candidate.proposedDefinition.trim() === '',
  )
  // 대표어(form)는 생성 시 필수라 비어 있는 일이 없지만, 조건을 화면과 백엔드가 같은 문장으로
  // 갖도록 함께 확인한다(`DraftDictionaryReviewReadinessValidator`).
  const missingForm = candidates.filter((candidate) => candidate.form.trim() === '')

  return {
    draft,
    creatorName: nameByMemberId.get(Number(draft.createdBy)) ?? '—',
    candidates,
    reviewReady:
      candidates.length > 0 && missingDefinition.length === 0 && missingForm.length === 0,
    missingDefinition,
  }
}
