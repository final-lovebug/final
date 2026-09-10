import { useMutation, useQueryClient } from '@tanstack/react-query'
import { requestDocumentReview } from '../api/requestDocumentReview'

export function useRequestDocumentReview(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: requestDocumentReview,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['draft-documents', workspaceId] })
      queryClient.invalidateQueries({ queryKey: ['document-review-requests', workspaceId] })
    },
  })
}
