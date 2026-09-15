import { useQuery } from '@tanstack/react-query'
import { fetchDictionaryRevision } from '../api/fetchDictionaryRevision'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDictionaryRevision(workspaceId: WorkspaceId, reviewRequestId: string) {
  return useQuery({
    queryKey: ['dictionary-revision', workspaceId, reviewRequestId],
    queryFn: () => fetchDictionaryRevision(workspaceId, reviewRequestId),
    enabled: workspaceId !== '' && reviewRequestId !== '',
  })
}
