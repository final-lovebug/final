import { useQuery } from '@tanstack/react-query'
import { fetchNotifications, type FetchNotificationsParams } from '../api/fetchNotifications'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useNotifications(workspaceId: WorkspaceId, params: FetchNotificationsParams = {}) {
  return useQuery({
    queryKey: ['notifications', workspaceId, params],
    queryFn: () => fetchNotifications(workspaceId, params),
  })
}
