import { httpClient } from '../../../shared/api/httpClient'
import type { CandidateTermListItem } from '../model/fixtures'
import type {
  CandidateTermOrigin,
  CandidateTermStatus,
  CandidateTermType,
} from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

// 후보어 api 4개(조회·등록·수정·판정)가 공유하는 응답 타입과 매핑.
// docs/API.md "후보어 등록·수정·삭제·목록" 절.

export interface CandidateTermApiResponse {
  candidateTermId: number
  draftDictionaryId: number
  origin: CandidateTermOrigin
  sourceTermId: number | null
  form: string
  proposedDefinition: string | null
  proposedEnglishName: string | null
  occurrenceCount: number | null
  status: CandidateTermStatus
  type: CandidateTermType | null
  createdBy: number | null
  handledBy: number | null
  rejectReason: string | null
  mergeTargetTermId: number | null
  resultTermId: number | null
  occurredDocumentIds: number[]
  contextSnippets: string[]
  variantForms: string[]
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * 실 응답을 화면 행으로 옮긴다.
 *
 * `words`는 `variantForms`를 쓰되 비어 있으면 `form` 하나짜리로 만든다 — 수동 등록분은
 * 추출 그룹핑을 거치지 않아 빈 배열로 온다(`D-65`). `ownerName`은 여기서 채우지 않는다
 * (id→이름 해석은 목록 단위 배치 조회라 호출하는 쪽이 한다).
 */
export function toListItem(
  response: CandidateTermApiResponse,
  ownerName = '—',
): CandidateTermListItem {
  const words = response.variantForms.length > 0 ? response.variantForms : [response.form]
  return {
    id: String(response.candidateTermId),
    draftDictionaryId: String(response.draftDictionaryId),
    form: response.form,
    words,
    selectedWord: response.form,
    type: response.type ?? undefined,
    origin: response.origin,
    variantForms: response.variantForms,
    proposedDefinition: response.proposedDefinition ?? undefined,
    proposedEnglishName: response.proposedEnglishName ?? undefined,
    occurredDocumentIds: response.occurredDocumentIds.map(String),
    occurrenceCount: response.occurrenceCount ?? 0,
    contextSnippets: response.contextSnippets,
    status: response.status,
    handledBy: response.handledBy === null ? undefined : String(response.handledBy),
    rejectReason: response.rejectReason ?? undefined,
    mergeTargetTermId:
      response.mergeTargetTermId === null ? undefined : String(response.mergeTargetTermId),
    resultTermId: response.resultTermId === null ? undefined : String(response.resultTermId),
    ownerName,
    createdAt: response.createdAt,
    createdBy: response.createdBy === null ? '' : String(response.createdBy),
    updatedAt: response.updatedAt,
  }
}

interface DraftDictionaryApiResponse {
  draftDictionaryId: number
  workspaceId: number
  dictionaryId: number | null
  sourceDocumentIds: number[]
  status: 'EXAMINING' | 'EXAMINED' | 'REVIEW_REQUESTED' | 'REVISED'
  createdBy: number
  createdAt: string
  updatedAt: string
}

/**
 * 워크스페이스의 **교정 중인** 사전 초안 id를 찾는다(T-INT-20으로 생긴 조회).
 *
 * 후보어 엔드포인트는 전부 `draftDictionaryId`를 요구하는데 화면은 workspaceId만 안다.
 * 초안이 없으면 `null`을 돌려준다 — 아직 추출을 한 번도 돌리지 않은 워크스페이스다.
 */
export async function findExaminingDraftDictionaryId(
  workspaceId: WorkspaceId,
): Promise<number | null> {
  const response = await httpClient.get<PageResponse<DraftDictionaryApiResponse>>(
    `/api/draft-dictionaries?workspaceId=${workspaceId}&status=EXAMINING&page=0&size=1&sort=createdAt,desc`,
  )
  return response.content[0]?.draftDictionaryId ?? null
}
