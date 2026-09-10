import { useQuery } from '@tanstack/react-query'
import { fetchWorkspaceMembers } from '../api/fetchWorkspaceMembers'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useWorkspaceMembers(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['workspace-members', workspaceId],
    queryFn: () => fetchWorkspaceMembers(workspaceId),
  })
}
