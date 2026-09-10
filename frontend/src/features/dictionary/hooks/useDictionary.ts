import { useQuery } from '@tanstack/react-query'
import { fetchDictionary } from '../api/fetchDictionary'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDictionary(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['dictionary', workspaceId],
    queryFn: () => fetchDictionary(workspaceId),
  })
}
