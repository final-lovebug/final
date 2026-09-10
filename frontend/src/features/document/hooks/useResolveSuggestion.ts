import { useMutation, useQueryClient } from '@tanstack/react-query'
import { resolveSuggestion } from '../api/resolveSuggestion'

export function useResolveSuggestion(documentId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: resolveSuggestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suggestions', documentId] })
    },
  })
}
