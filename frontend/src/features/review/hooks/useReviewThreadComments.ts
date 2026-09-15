import { useQuery } from '@tanstack/react-query'
import {
  fetchReviewThreadComments,
  type FetchReviewThreadCommentsOptions,
} from '../api/fetchReviewThreadComments'

export function useReviewThreadComments(
  reviewRequestId: string,
  options: FetchReviewThreadCommentsOptions = {},
) {
  return useQuery({
    queryKey: ['review-thread-comments', reviewRequestId, options],
    queryFn: () => fetchReviewThreadComments(reviewRequestId, options),
    enabled: reviewRequestId !== '',
  })
}
