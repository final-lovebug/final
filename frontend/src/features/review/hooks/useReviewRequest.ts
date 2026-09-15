import { useQuery } from '@tanstack/react-query'
import { fetchReviewRequest } from '../api/fetchReviewRequest'

export function useReviewRequest(reviewRequestId: string) {
  return useQuery({
    queryKey: ['review-request', reviewRequestId],
    queryFn: () => fetchReviewRequest(reviewRequestId),
    enabled: reviewRequestId !== '',
  })
}
