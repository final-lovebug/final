import { useQuery } from '@tanstack/react-query'
import { fetchReviewThreadComments } from '../api/fetchReviewThreadComments'

export function useReviewThreadComments(reviewId: string) {
  return useQuery({
    queryKey: ['review-thread-comments', reviewId],
    queryFn: () => fetchReviewThreadComments(reviewId),
  })
}
