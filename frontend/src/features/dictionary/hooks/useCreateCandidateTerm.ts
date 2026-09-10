import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCandidateTerm } from '../api/createCandidateTerm'

export function useCreateCandidateTerm(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createCandidateTerm,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dictionary-candidates', workspaceId] })
    },
  })
}
