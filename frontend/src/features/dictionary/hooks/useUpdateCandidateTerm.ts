import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateCandidateTerm } from '../api/updateCandidateTerm'

export function useUpdateCandidateTerm(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateCandidateTerm,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dictionary-candidates', workspaceId] })
    },
  })
}
