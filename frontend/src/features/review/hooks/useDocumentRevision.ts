import { useQuery } from '@tanstack/react-query'
import { fetchDocumentRevision } from '../api/fetchDocumentRevision'

export function useDocumentRevision(reviewRequestId: string) {
  return useQuery({
    queryKey: ['document-revision', reviewRequestId],
    queryFn: () => fetchDocumentRevision(reviewRequestId),
    enabled: reviewRequestId !== '',
  })
}
