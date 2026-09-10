import { useQuery } from '@tanstack/react-query'
import { fetchDocumentVersions } from '../api/fetchDocumentVersions'
import type { DocumentId } from '../../../shared/types/ids'

export function useDocumentVersions(documentId: DocumentId) {
  return useQuery({
    queryKey: ['document-versions', documentId],
    queryFn: () => fetchDocumentVersions(documentId),
  })
}
