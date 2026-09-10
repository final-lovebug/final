import { useQuery } from '@tanstack/react-query'
import { fetchDocuments } from '../api/fetchDocuments'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDocuments(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['documents', workspaceId],
    queryFn: () => fetchDocuments(workspaceId),
  })
}
