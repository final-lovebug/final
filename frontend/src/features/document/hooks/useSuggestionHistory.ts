import { useQuery } from '@tanstack/react-query'
import { fetchSuggestionHistory } from '../api/fetchSuggestionHistory'
import type { DocumentId } from '../../../shared/types/ids'

export function useSuggestionHistory(documentId: DocumentId) {
  return useQuery({
    queryKey: ['suggestion-history', documentId],
    queryFn: () => fetchSuggestionHistory(documentId),
  })
}
