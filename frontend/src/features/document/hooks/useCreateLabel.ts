import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createLabel } from '../api/createLabel'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useCreateLabel(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createLabel,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['labels', workspaceId] })
    },
  })
}
