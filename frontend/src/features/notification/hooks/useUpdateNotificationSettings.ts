import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  updateNotificationSettings,
  type UpdateNotificationSettingsInput,
} from '../api/updateNotificationSettings'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useUpdateNotificationSettings(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpdateNotificationSettingsInput) =>
      updateNotificationSettings(workspaceId, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-settings', workspaceId] })
    },
  })
}
