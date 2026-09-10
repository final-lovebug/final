import { useQuery } from '@tanstack/react-query'
import { fetchDocument } from '../api/fetchDocument'
import type { DocumentId } from '../../../shared/types/ids'

export function useDocument(documentId: DocumentId) {
  return useQuery({
    queryKey: ['document', documentId],
    queryFn: () => fetchDocument(documentId),
  })
}
