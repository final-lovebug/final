import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createDocument } from '../api/createDocument'

export function useCreateDocument() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createDocument,
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['documents', created.workspaceId] })
    },
  })
}
