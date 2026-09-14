import { useQuery } from '@tanstack/react-query'
import { fetchLabels } from '../api/fetchLabels'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useLabels(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['labels', workspaceId],
    queryFn: () => fetchLabels(workspaceId),
  })
}
