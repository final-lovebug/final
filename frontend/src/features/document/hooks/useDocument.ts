import { useQuery } from '@tanstack/react-query'
import { fetchDocument } from '../api/fetchDocument'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

export function useDocument(workspaceId: WorkspaceId, documentId: DocumentId) {
  return useQuery({
    queryKey: ['document', workspaceId, documentId],
    queryFn: () => fetchDocument(workspaceId, documentId),
  })
}
