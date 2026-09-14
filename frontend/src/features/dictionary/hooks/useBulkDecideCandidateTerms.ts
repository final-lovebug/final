import { useMutation, useQueryClient } from '@tanstack/react-query'
import { bulkDecideCandidateTerms } from '../api/bulkDecideCandidateTerms'

export function useBulkDecideCandidateTerms(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: bulkDecideCandidateTerms,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dictionary-candidates', workspaceId] })
    },
  })
}
