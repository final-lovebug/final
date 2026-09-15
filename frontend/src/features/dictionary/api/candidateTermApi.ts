import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
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
 * 추출 그룹핑을 거치지 않아 빈 배열로 온다(`D-65`).
 *
 * **후보어별 작성자는 더 이상 없다**(`docs/plan/DRAFT_PLAN.md`) — 응답에서 `createdBy`가
 * 빠졌고 화면도 보여주지 않는다. 작성자는 초안 하나에 한 명이라 초안 응답이 들고 온다.
 */
export function toListItem(response: CandidateTermApiResponse): CandidateTermListItem {
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
    createdAt: response.createdAt,
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

export interface DraftDictionaryInfo {
  id: string
  workspaceId: string
  dictionaryId: string | null
  sourceDocumentIds: string[]
  status: 'EXAMINING' | 'EXAMINED' | 'REVIEW_REQUESTED' | 'REVISED'
  /** 초안 작성자(회원 id). 이름은 `GET /api/members?ids=`로 해석한다(`D-62`). */
  createdBy: string
  createdAt: string
  updatedAt: string
}

function toDraftInfo(response: DraftDictionaryApiResponse): DraftDictionaryInfo {
  return {
    id: String(response.draftDictionaryId),
    workspaceId: String(response.workspaceId),
    dictionaryId: response.dictionaryId === null ? null : String(response.dictionaryId),
    sourceDocumentIds: response.sourceDocumentIds.map(String),
    status: response.status,
    createdBy: String(response.createdBy),
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}

/**
 * 워크스페이스의 **진행 중인** 사전 초안 하나를 찾는다(T-INT-20으로 생긴 조회).
 *
 * 후보어 엔드포인트는 전부 `draftDictionaryId`를 요구하는데 화면은 workspaceId만 안다.
 * 초안이 없으면 `null`이다 — 아직 추출을 한 번도 돌리지 않은 워크스페이스다.
 *
 * **`EXAMINING`만 보지 않는다.** 교정 완료·리뷰 요청을 지나면 초안은 `EXAMINED`
 * ·`REVIEW_REQUESTED`로 올라가는데, 재교정으로 되돌아오면 그 초안을 다시 고쳐야 한다
 * (`docs/plan/DRAFT_PLAN.md`의 재교정 흐름). 그래서 `REVISED`(이미 발행에 쓰인 것)만
 * 빼고 가장 최근 것을 집는다.
 */
export async function findOngoingDraftDictionary(
  workspaceId: WorkspaceId,
): Promise<DraftDictionaryInfo | null> {
  const drafts = await fetchAllPages<DraftDictionaryApiResponse>((page, size) =>
    httpClient.get<PageResponse<DraftDictionaryApiResponse>>(
      `/api/draft-dictionaries?workspaceId=${workspaceId}&page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  )
  const ongoing = drafts.find((draft) => draft.status !== 'REVISED')
  return ongoing ? toDraftInfo(ongoing) : null
}

/** 후보어 엔드포인트에 넘길 초안 id만 필요한 호출부용. */
export async function findOngoingDraftDictionaryId(
  workspaceId: WorkspaceId,
): Promise<string | null> {
  const draft = await findOngoingDraftDictionary(workspaceId)
  return draft?.id ?? null
}
