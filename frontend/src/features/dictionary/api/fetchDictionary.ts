import { delay } from '../../../shared/lib/delay'
import { DICTIONARY_FIXTURE, TERM_FIXTURES } from '../model/fixtures'
import type { Dictionary, Term } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface WorkspaceDictionary {
  dictionary: Dictionary
  terms: Term[]
}

// 워크스페이스당 사전집 1개만 다루는 지금 범위에 맞춘 목업. 여러 사전집을 다루게 되면
// fetchDictionaries(workspaceId): Dictionary[]로 바꾼다.
export async function fetchDictionary(
  workspaceId: WorkspaceId,
): Promise<WorkspaceDictionary | undefined> {
  await delay()
  if (DICTIONARY_FIXTURE.workspaceId !== workspaceId) return undefined
  return { dictionary: DICTIONARY_FIXTURE, terms: TERM_FIXTURES }
}
