import { useQuery } from '@tanstack/react-query'
import { fetchWorkspace } from '../api/fetchWorkspace'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useWorkspace(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['workspace', workspaceId],
    queryFn: () => fetchWorkspace(workspaceId),
    enabled: workspaceId !== '',
  })
}
