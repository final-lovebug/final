import { delay } from '../../../shared/lib/delay'
import {
  SUGGESTION_HISTORY_FIXTURES,
  type SuggestionHistoryItem,
} from '../model/suggestionFixtures'
import type { DocumentId } from '../../../shared/types/ids'

export async function fetchSuggestionHistory(
  documentId: DocumentId,
): Promise<SuggestionHistoryItem[]> {
  await delay()
  return SUGGESTION_HISTORY_FIXTURES[documentId] ?? []
}
