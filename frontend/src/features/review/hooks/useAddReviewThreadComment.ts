import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addReviewThreadComment } from '../api/addReviewThreadComment'

export function useAddReviewThreadComment(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: addReviewThreadComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['review-thread-comments', reviewRequestId] })
      queryClient.invalidateQueries({ queryKey: ['dictionary-revision'] })
    },
  })
}
