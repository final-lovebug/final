import { useQuery } from '@tanstack/react-query'
import { fetchDocumentVersions } from '../api/fetchDocumentVersions'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

export function useDocumentVersions(workspaceId: WorkspaceId, documentId: DocumentId) {
  return useQuery({
    queryKey: ['document-versions', workspaceId, documentId],
    queryFn: () => fetchDocumentVersions(workspaceId, documentId),
  })
}
