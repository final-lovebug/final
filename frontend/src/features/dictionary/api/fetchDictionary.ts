import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPagesWith } from '../../../shared/api/fetchAllPages'
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
  dictionaryId: number
  workspaceId: number
  versionNo: number
  status: 'ACTIVE' | 'ARCHIVED'
  publishedAt: string
  createdBy: number
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
// 화면은 "—"로 표시한다. 사전집에 "이름"은 없으므로(워크스페이스당 활성 1개일 뿐)
// `name`만 고정 문자열로 합성한다.
//
// **2026-09-14 T-INT-17에서 정정**: 실 응답(`DictionaryResponse`)에는 `dictionaryId`가
// 있다 — docs/API.md의 예시가 낡아 없는 줄 알고 `dict-{workspaceId}`로 합성하고 있었다
// (문서도 함께 고쳤다). 용어 추출 접수(`POST /api/draft-dictionaries/extractions`)가
// 이 id를 요구하므로 이제 실제 값을 그대로 쓴다. 발행자 필드명도 `publishedBy`가 아니라
// `createdBy`라 그동안 `publishedBy`가 undefined였다.
//
// 페이지네이션: 화면에 페이징 UI가 없어 용어 전체가 필요하다. `size=200`으로 한 번에
// 받으려 했지만 규격 상한이 100이라 **모든 요청이 400(`COMMON_INVALID_REQUEST`)이었다** —
// 사전집 화면이 통째로 열리지 않던 원인이다. 상한 크기로 끝까지 페이징한다.
export async function fetchDictionary(
  workspaceId: WorkspaceId,
  /** 보관 버전을 열어 볼 때만 준다. 없으면 활성 사전집이다. */
  versionNo?: number,
): Promise<WorkspaceDictionary> {
  const path =
    versionNo === undefined
      ? `/api/workspaces/${workspaceId}/dictionary`
      : `/api/workspaces/${workspaceId}/dictionary/versions/${versionNo}`
  const { first: response, content: termContent } = await fetchAllPagesWith(
    (page, size) =>
      httpClient.get<ActiveDictionaryResponse>(
        `${path}?page=${page}&size=${size}&sort=preferredForm,asc`,
      ),
    (body) => body.terms,
  )
  const dictionary: Dictionary = {
    id: String(response.dictionaryId),
    workspaceId: String(response.workspaceId),
    name: '사전집',
    currentVersionNo: response.versionNo,
    status: response.status,
    publishedAt: response.publishedAt,
    publishedBy: String(response.createdBy),
  }
  const terms: Term[] = termContent.map((term) => ({
    id: String(term.termId),
    preferredForm: term.preferredForm,
    englishName: term.englishName,
  }))
  return { dictionary, terms }
}
