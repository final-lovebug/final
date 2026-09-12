import { useQuery } from '@tanstack/react-query'
import { fetchSuggestions } from '../api/fetchSuggestions'
import type { DocumentId } from '../../../shared/types/ids'

export function useSuggestions(documentId: DocumentId) {
  return useQuery({
    queryKey: ['suggestions', documentId],
    queryFn: () => fetchSuggestions(documentId),
  })
}
