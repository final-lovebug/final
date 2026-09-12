import { useQuery } from '@tanstack/react-query'
import { fetchDictionaryRevisionTimeline } from '../api/fetchDictionaryRevisionTimeline'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDictionaryRevisionTimeline(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['dictionary-revision-timeline', workspaceId],
    queryFn: () => fetchDictionaryRevisionTimeline(workspaceId),
  })
}
