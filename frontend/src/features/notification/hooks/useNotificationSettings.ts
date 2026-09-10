import { useQuery } from '@tanstack/react-query'
import { fetchNotificationSettings } from '../api/fetchNotificationSettings'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useNotificationSettings(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['notification-settings', workspaceId],
    queryFn: () => fetchNotificationSettings(workspaceId),
  })
}
