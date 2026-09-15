import { useQuery } from '@tanstack/react-query'
import { fetchUnreadCount } from '../api/fetchUnreadCount'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useUnreadCount(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['notifications', workspaceId, 'unread-count'],
    queryFn: () => fetchUnreadCount(workspaceId),
    enabled: workspaceId !== '',
  })
}
