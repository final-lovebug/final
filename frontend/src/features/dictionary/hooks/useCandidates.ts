import { useQuery } from '@tanstack/react-query'
import { fetchCandidates } from '../api/fetchCandidates'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useCandidates(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['dictionary-candidates', workspaceId],
    queryFn: () => fetchCandidates(workspaceId),
  })
}
