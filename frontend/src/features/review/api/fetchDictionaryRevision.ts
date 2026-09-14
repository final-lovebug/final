import { delay } from '../../../shared/lib/delay'
import {
  DICTIONARY_REVISION_ROWS,
  DICTIONARY_REVISION_THREAD,
  type RevisionCommentListItem,
  type RevisionDictionaryTermRow,
} from '../model/fixtures'

export interface DictionaryRevisionDetail {
  rows: RevisionDictionaryTermRow[]
  thread: RevisionCommentListItem[]
}

// revisionId도 마찬가지로 목데이터 1건뿐이다.
export async function fetchDictionaryRevision(
  _revisionId: string,
): Promise<DictionaryRevisionDetail> {
  await delay()
  return { rows: DICTIONARY_REVISION_ROWS, thread: DICTIONARY_REVISION_THREAD }
}
