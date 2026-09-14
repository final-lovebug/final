import { httpClient } from '../../../shared/api/httpClient'
import type { Dictionary, Term } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

interface TermSummary {
  termId: number
  preferredForm: string
  englishName?: string
}

interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

interface ActiveDictionaryResponse {
  versionNo: number
  status: 'ACTIVE' | 'ARCHIVED'
  publishedAt: string
  publishedBy: number
  terms: PageResponse<TermSummary>
}

export interface WorkspaceDictionary {
  dictionary: Dictionary
  terms: Term[]
}

// 실제 백엔드 연동(docs/API.md "활성 사전집 조회", GET /api/workspaces/{workspaceId}/dictionary).
// 사전집이 아직 없는 워크스페이스는 404(DICTIONARY_NOT_FOUND)를 던진다 — 목업의
// `undefined` 반환과 달리 예외로 나타난다(호출 쪽에서 처리 필요).
//
// **알려진 제약(2026-09-14, docs/task/T-INT-11-dictionary.md 참고)**: 실 API 목록
// 응답(`TermSummary`)에는 `definition`이 없다(`D-41`, 목록 엔드포인트가 의도적으로
// 뺀다 — 단건 조회 엔드포인트도 없음). `Term.definition`은 항상 `undefined`로 남고
// 화면은 "—"로 표시한다. 사전집 자체에도 id/name 개념이 없어(워크스페이스당 활성
// 1개일 뿐) `workspaceId` 기반으로 합성한다.
//
// 페이지네이션: 지금은 한 번에 최대 200개까지만 가져온다(첫 페이지, size=200) — 화면에
// 페이징 UI가 아직 없다. 사전집 용어가 200개를 넘으면 나머지가 잘린다(후속 과제).
export async function fetchDictionary(workspaceId: WorkspaceId): Promise<WorkspaceDictionary> {
  const response = await httpClient.get<ActiveDictionaryResponse>(
    `/api/workspaces/${workspaceId}/dictionary?page=0&size=200&sort=preferredForm,asc`,
  )
  const dictionary: Dictionary = {
    id: `dict-${workspaceId}`,
    workspaceId,
    name: '사전집',
    currentVersionNo: response.versionNo,
    status: response.status,
    publishedAt: response.publishedAt,
    publishedBy: String(response.publishedBy),
  }
  const terms: Term[] = response.terms.content.map((term) => ({
    id: String(term.termId),
    preferredForm: term.preferredForm,
    englishName: term.englishName,
  }))
  return { dictionary, terms }
}
