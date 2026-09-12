import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addReviewThreadComment } from '../api/addReviewThreadComment'

export function useAddReviewThreadComment(reviewId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: addReviewThreadComment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['review-thread-comments', reviewId] })
    },
  })
}
