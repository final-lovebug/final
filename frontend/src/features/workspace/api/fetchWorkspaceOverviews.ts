import { httpClient } from '../../../shared/api/httpClient'
import { fetchWorkspaces } from './fetchWorkspaces'
import type { Workspace } from '../model/types'

// 워크스페이스 선택 화면(ui/main.js renderWorkspaces)의 카드는 이름만으로는 부족하다 —
// 멤버 수·문서 수·사전집 배지가 함께 있어야 "어느 워크스페이스였더라"를 고를 수 있다.
// 목록 응답(`GET /api/workspaces`)에는 그 셋이 없어 워크스페이스마다 따로 센다.
//
// **워크스페이스마다 3번 부른다(N+1).** 한 회원이 참여하는 워크스페이스 수는 구조적으로
// 작아(그래서 목록 API가 페이징도 안 한다 — docs/API.md) 감수한다. 셋 다 가벼운 조회다:
// 참여자는 배열 하나, 문서 수는 `size=1`로 `totalElements`만 읽는다.

interface ParticipantApiItem {
  memberId: number
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
}

interface ActiveDictionaryApiResponse {
  versionNo: number
}

export interface WorkspaceOverview extends Workspace {
  memberCount: number
  documentCount: number
  /** 활성 사전집 버전. 아직 첫 발행 전이면 null(오류가 아니라 정상 상태다). */
  dictionaryVersionNo: number | null
}

/** 실패를 값으로 바꾼다 — 카드 하나의 부가 정보 때문에 화면 전체가 죽지 않게 한다. */
async function orNull<T>(promise: Promise<T>): Promise<T | null> {
  try {
    return await promise
  } catch {
    return null
  }
}

export async function fetchWorkspaceOverviews(): Promise<WorkspaceOverview[]> {
  const workspaces = await fetchWorkspaces()

  return Promise.all(
    workspaces.map(async (workspace) => {
      const [participants, documents, dictionary] = await Promise.all([
        orNull(
          httpClient.get<ParticipantApiItem[]>(
            `/api/workspaces/${workspace.id}/participants`,
          ),
        ),
        orNull(
          httpClient.get<PageResponse<unknown>>(
            `/api/workspaces/${workspace.id}/documents?page=0&size=1`,
          ),
        ),
        // 사전집이 없는 워크스페이스는 404(DICTIONARY_NOT_FOUND)다 — 첫 발행 전의 정상 상태.
        orNull(
          httpClient.get<ActiveDictionaryApiResponse>(
            `/api/workspaces/${workspace.id}/dictionary?page=0&size=1`,
          ),
        ),
      ])

      return {
        ...workspace,
        memberCount: participants?.length ?? 0,
        documentCount: documents?.totalElements ?? 0,
        dictionaryVersionNo: dictionary?.versionNo ?? null,
      }
    }),
  )
}
