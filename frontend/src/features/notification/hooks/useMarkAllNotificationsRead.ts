import { useMutation, useQueryClient } from '@tanstack/react-query'
import { markAllNotificationsRead } from '../api/markAllNotificationsRead'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useMarkAllNotificationsRead(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => markAllNotificationsRead(workspaceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications', workspaceId] })
    },
  })
}
