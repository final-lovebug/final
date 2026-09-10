import { useQuery } from '@tanstack/react-query'
import { fetchDictionaryRevision } from '../api/fetchDictionaryRevision'

export function useDictionaryRevision(revisionId: string) {
  return useQuery({
    queryKey: ['dictionary-revision', revisionId],
    queryFn: () => fetchDictionaryRevision(revisionId),
  })
}
