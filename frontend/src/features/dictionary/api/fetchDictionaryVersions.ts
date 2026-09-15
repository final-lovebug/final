import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages, fetchAllPagesWith } from '../../../shared/api/fetchAllPages'
import { fetchMemberNames } from '../../../shared/api/memberNames'
import type { DictionaryStatus } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'
import type { Page } from '../../../shared/types/common'

// 사전집 리비전 이력(ui/main.js renderDictHistoryScreen).
//
// **개정 이력 전용 API는 없다.** `revisionlog` 도메인은 백엔드에 있지만 presentation
// 패키지가 없어 REST로 나오지 않는다(T-INT-11에서 확인해 그 항목만 건너뛰었다). 대신
// **버전 목록 + 버전별 용어 목록**으로 이력과 차이를 직접 만든다 — 둘 다 실재하는
// 엔드포인트다(docs/API.md «Dictionary API»).

interface DictionaryVersionApiItem {
  dictionaryId: number
  versionNo: number
  status: DictionaryStatus
  publishedAt: string
  createdBy: number
  termCount: number
}

interface TermSummary {
  termId: number
  preferredForm: string
  englishName?: string
}

interface DictionaryVersionDetailResponse {
  versionNo: number
  terms: Page<TermSummary>
}

export interface DictionaryVersionSummary {
  versionNo: number
  status: DictionaryStatus
  publishedAt: string
  publishedByName: string
  termCount: number
}

/** `GET /api/workspaces/{workspaceId}/dictionary/versions` — 최신이 먼저 오도록 정렬한다. */
export async function fetchDictionaryVersions(
  workspaceId: WorkspaceId,
): Promise<DictionaryVersionSummary[]> {
  // 상한(100)에 딱 맞춰 부르고 있었다 — 버전이 100개를 넘으면 조용히 잘린다. 끝까지 읽는다.
  const versions = await fetchAllPages<DictionaryVersionApiItem>((page, size) =>
    httpClient.get<Page<DictionaryVersionApiItem>>(
      `/api/workspaces/${workspaceId}/dictionary/versions?page=${page}&size=${size}`,
    ),
  )
  if (versions.length === 0) return []

  const nameByMemberId = await fetchMemberNames(versions.map((v) => v.createdBy))
  return versions
    .map((version) => ({
      versionNo: version.versionNo,
      status: version.status,
      publishedAt: version.publishedAt,
      publishedByName: nameByMemberId.get(version.createdBy) ?? '—',
      termCount: version.termCount,
    }))
    .sort((a, b) => b.versionNo - a.versionNo)
}

export interface DictionaryVersionDiff {
  fromVersionNo: number
  toVersionNo: number
  added: string[]
  removed: string[]
  /** 표기는 그대로인데 영문명이 달라진 것. 아래 주석의 한계를 함께 읽는다. */
  changed: string[]
  /**
   * 사라진 표기가 있으면 그 표기를 쓰던 문서를 다시 봐야 한다(`D-60`) — 대표어 변경도
   * 「삭제 + 추가」 한 쌍으로 나타나므로 같은 규칙에 걸린다.
   */
  recheckRequired: boolean
}

async function fetchVersionTerms(
  workspaceId: WorkspaceId,
  versionNo: number,
): Promise<TermSummary[]> {
  // `size=500`은 규격 상한(100)을 넘어 400이었다 — 버전 간 diff가 전혀 계산되지 않았다.
  // diff는 두 버전의 용어 전체를 비교해야 정확하므로 끝까지 페이징한다.
  const { content } = await fetchAllPagesWith(
    (page, size) =>
      httpClient.get<DictionaryVersionDetailResponse>(
        `/api/workspaces/${workspaceId}/dictionary/versions/${versionNo}?page=${page}&size=${size}&sort=preferredForm,asc`,
      ),
    (body) => body.terms,
  )
  return content
}

/**
 * 두 버전의 차이를 클라이언트에서 계산한다.
 *
 * diff 키는 `preferredForm`이다(`D-60`) — 추가는 새 버전에만, 삭제는 이전 버전에만 있는
 * 표기다.
 *
 * **「변경」은 영문명 차이로만 잡힌다는 한계가 있다.** 용어 목록 응답이 `definition`을
 * 의도적으로 싣지 않아서(`D-41`) 정의만 고친 용어는 여기서 드러나지 않는다. 정의까지
 * 비교하려면 용어 단건 조회나 개정 이력 API가 필요하다(후속 과제).
 */
export async function fetchDictionaryVersionDiff(
  workspaceId: WorkspaceId,
  fromVersionNo: number,
  toVersionNo: number,
): Promise<DictionaryVersionDiff> {
  const [fromTerms, toTerms] = await Promise.all([
    fetchVersionTerms(workspaceId, fromVersionNo),
    fetchVersionTerms(workspaceId, toVersionNo),
  ])

  const fromByForm = new Map(fromTerms.map((term) => [term.preferredForm, term]))
  const toByForm = new Map(toTerms.map((term) => [term.preferredForm, term]))

  const added = toTerms
    .filter((term) => !fromByForm.has(term.preferredForm))
    .map((term) => term.preferredForm)
  const removed = fromTerms
    .filter((term) => !toByForm.has(term.preferredForm))
    .map((term) => term.preferredForm)
  const changed = toTerms
    .filter((term) => {
      const before = fromByForm.get(term.preferredForm)
      return before !== undefined && (before.englishName ?? '') !== (term.englishName ?? '')
    })
    .map((term) => term.preferredForm)

  return {
    fromVersionNo,
    toVersionNo,
    added,
    removed,
    changed,
    recheckRequired: removed.length > 0,
  }
}
