import { useMutation, useQueryClient } from '@tanstack/react-query'
import { resolveSuggestion } from '../api/resolveSuggestion'

/**
 * 제안어 판정(수용/유지). 처리 내역은 제안어에서 파생하므로 함께 무효화한다.
 */
export function useResolveSuggestion(documentId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: resolveSuggestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suggestions', documentId] })
      queryClient.invalidateQueries({ queryKey: ['suggestion-history', documentId] })
    },
  })
}
