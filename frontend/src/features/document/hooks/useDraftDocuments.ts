import { useQuery } from '@tanstack/react-query'
import { fetchDraftDocuments } from '../api/fetchDraftDocuments'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDraftDocuments(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['draft-documents', workspaceId],
    queryFn: () => fetchDraftDocuments(workspaceId),
  })
}
