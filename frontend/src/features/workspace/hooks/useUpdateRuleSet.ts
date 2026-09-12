import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateRuleSet } from '../api/updateRuleSet'

export function useUpdateRuleSet(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateRuleSet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ruleset', workspaceId] })
    },
  })
}
