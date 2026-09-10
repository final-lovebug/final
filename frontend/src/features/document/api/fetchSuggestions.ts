import { delay } from '../../../shared/lib/delay'
import { SUGGESTION_FIXTURES } from '../model/suggestionFixtures'
import type { SuggestionTerm } from '../model/types'
import type { DocumentId } from '../../../shared/types/ids'

// draftDocumentId는 `draft-{documentId}` 규칙으로 만들어 뒀다(suggestionFixtures.ts 참고).
// 지금은 doc-plan·doc-retention 두 문서만 실제 제안어 데이터를 갖고 있다.
export async function fetchSuggestions(documentId: DocumentId): Promise<SuggestionTerm[]> {
  await delay()
  const draftDocumentId = `draft-${documentId}`
  return SUGGESTION_FIXTURES.filter((s) => s.draftDocumentId === draftDocumentId)
}
