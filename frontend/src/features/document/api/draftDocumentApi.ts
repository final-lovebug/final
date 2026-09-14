import { httpClient } from '../../../shared/api/httpClient'
import type { Page } from '../../../shared/types/common'
import type {
  DraftDocument,
  DraftDocumentStatus,
  SuggestionTerm,
  SuggestionTermStatus,
} from '../model/types'
import type { DocumentId } from '../../../shared/types/ids'

// DraftDocument·SuggestionTerm의 실 API 공통 타입·매핑(docs/API.md «DraftDocument API»).
// 초안 목록·제안어 목록·판정·처리 내역이 모두 이 두 리소스를 쓰므로 한 곳에 모은다.

interface TextRangeApiResponse {
  startOffset: number
  endOffset: number
}

export interface DraftDocumentApiResponse {
  draftDocumentId: number
  documentId: number
  baseVersionNo: number
  draftBody: string
  status: DraftDocumentStatus
  requestedBy: number | null
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface SuggestionTermApiResponse {
  id: number
  draftDocumentId: number
  anchor: TextRangeApiResponse
  originTerm: string
  suggestionTerm: string
  status: 'PENDING' | 'KEPT_ORIGIN' | 'APPLIED_SUGGESTION'
  handledBy: number | null
  rejectReason: string | null
  createdBy: number
  createdAt: string
  updatedAt: string
}

/**
 * 제안어 상태 이름 변환. 실 API는 `KEPT_ORIGIN`·`APPLIED_SUGGESTION`이고 화면 코드는
 * `KEEP_ORIGINAL`·`APPLY_SUGGESTION`을 쓴다 — 경계에서만 이름이 바뀐다.
 */
export function toSuggestionTermStatus(
  status: SuggestionTermApiResponse['status'],
): SuggestionTermStatus {
  switch (status) {
    case 'KEPT_ORIGIN':
      return 'KEEP_ORIGINAL'
    case 'APPLIED_SUGGESTION':
      return 'APPLY_SUGGESTION'
    default:
      return 'PENDING'
  }
}

export function toDraftDocument(response: DraftDocumentApiResponse): DraftDocument {
  return {
    id: String(response.draftDocumentId),
    documentId: String(response.documentId),
    baseVersionNo: response.baseVersionNo,
    draftBody: response.draftBody,
    status: response.status,
    requestedBy: response.requestedBy === null ? undefined : String(response.requestedBy),
    createdBy: String(response.createdBy),
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}

export function toSuggestionTerm(response: SuggestionTermApiResponse): SuggestionTerm {
  return {
    id: String(response.id),
    draftDocumentId: String(response.draftDocumentId),
    // 실 API는 {startOffset, endOffset}, 프론트 공용 TextRange는 {start, end}다.
    anchor: { start: response.anchor.startOffset, end: response.anchor.endOffset },
    originTerm: response.originTerm,
    suggestionTerm: response.suggestionTerm,
    status: toSuggestionTermStatus(response.status),
    handledBy: response.handledBy === null ? undefined : String(response.handledBy),
    rejectReason: response.rejectReason ?? undefined,
    createdBy: String(response.createdBy),
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}

/**
 * 문서의 가장 최근 초안 한 건을 찾는다(`GET /api/draft-documents?documentId=`).
 *
 * **화면은 documentId만 아는데 제안어·판정 엔드포인트는 draftDocumentId를 요구한다** —
 * 그 사이를 잇는 조회다. 초안이 없으면(대조를 한 번도 돌리지 않은 문서) `null`이다.
 */
export async function fetchLatestDraftDocument(
  documentId: DocumentId,
): Promise<DraftDocument | null> {
  const page = await httpClient.get<Page<DraftDocumentApiResponse>>(
    `/api/draft-documents?documentId=${documentId}&page=0&size=1&sort=createdAt,desc`,
  )
  const draft = page.content[0]
  return draft ? toDraftDocument(draft) : null
}

/** 초안의 제안어 전체(`GET /api/draft-documents/{id}/suggestion-terms`). */
export async function fetchSuggestionTermsOfDraft(
  draftDocumentId: string,
): Promise<SuggestionTerm[]> {
  // 한 문서의 제안어가 100건을 넘는 경우는 실무상 드물어 첫 페이지만 읽는다 —
  // 넘어가면 페이징 UI가 필요하다(후속 과제).
  const page = await httpClient.get<Page<SuggestionTermApiResponse>>(
    `/api/draft-documents/${draftDocumentId}/suggestion-terms?page=0&size=100&sort=createdAt,asc`,
  )
  return page.content.map(toSuggestionTerm)
}
