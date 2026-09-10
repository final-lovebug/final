import { delay } from '../../../shared/lib/delay'
import {
  DICTIONARY_DIFF_R6_R7,
  DICTIONARY_REVISION_TIMELINE,
  type DictionaryRevisionTimelineItem,
  type DictionaryVersionDiff,
} from '../model/versionFixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface DictionaryHistory {
  timeline: DictionaryRevisionTimelineItem[]
  /** 최신 두 리비전(r6→r7)의 차이만 목업으로 갖고 있다. 실제로는 선택한 두 버전을 비교해야 한다. */
  latestDiff: DictionaryVersionDiff
}

export async function fetchDictionaryRevisionTimeline(
  workspaceId: WorkspaceId,
): Promise<DictionaryHistory> {
  await delay()
  if (workspaceId !== 'potenup_be') {
    return { timeline: [], latestDiff: { added: [], changed: [], removed: [] } }
  }
  return { timeline: DICTIONARY_REVISION_TIMELINE, latestDiff: DICTIONARY_DIFF_R6_R7 }
}
