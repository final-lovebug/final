import { delay } from '../../../shared/lib/delay'
import { SUGGESTION_FIXTURES } from '../model/suggestionFixtures'
import type { SuggestionTerm } from '../model/types'
import type { DocumentId } from '../../../shared/types/ids'

// 지금은 doc-plan 하나만 실제 제안어 데이터를 갖고 있다(ui/ 프로토타입과 동일한 범위).
export async function fetchSuggestions(documentId: DocumentId): Promise<SuggestionTerm[]> {
  await delay()
  return documentId === 'doc-plan' ? SUGGESTION_FIXTURES : []
}
