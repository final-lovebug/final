import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateRuleSet, type UpdateRuleSetInput } from '../api/updateRuleSet'

export function useUpdateRuleSet(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpdateRuleSetInput) => updateRuleSet(workspaceId, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ruleset', workspaceId] })
    },
  })
}
