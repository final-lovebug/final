import { useMutation, useQueryClient } from '@tanstack/react-query'
import { markNotificationRead } from '../api/markNotificationRead'
import type { NotificationId, WorkspaceId } from '../../../shared/types/ids'

export function useMarkNotificationRead(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (notificationId: NotificationId) => markNotificationRead(workspaceId, notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications', workspaceId] })
    },
  })
}
