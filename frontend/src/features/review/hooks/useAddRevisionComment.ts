import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addRevisionComment } from '../api/addRevisionComment'

export function useAddRevisionComment(revisionId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: addRevisionComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dictionary-revision', revisionId] })
    },
  })
}
