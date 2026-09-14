import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchReviewProgress, fetchReviews, submitReview } from '../api/submitReview'

export function useSubmitReview(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: submitReview,
    onSuccess: () => {
      // 제출과 동시에 백엔드가 정족수를 재계산해 요청 상태를 바꾼다 — 진행 상황과 목록을
      // 함께 무효화해야 화면이 어긋나지 않는다.
      queryClient.invalidateQueries({ queryKey: ['review-progress', reviewRequestId] })
      queryClient.invalidateQueries({ queryKey: ['reviews', reviewRequestId] })
      queryClient.invalidateQueries({ queryKey: ['review-thread-comments', reviewRequestId] })
      queryClient.invalidateQueries({ queryKey: ['document-review-requests'] })
      queryClient.invalidateQueries({ queryKey: ['dictionary-revision'] })
    },
  })
}

export function useReviews(reviewRequestId: string) {
  return useQuery({
    queryKey: ['reviews', reviewRequestId],
    queryFn: () => fetchReviews(reviewRequestId),
    enabled: reviewRequestId !== '',
  })
}

export function useReviewProgress(reviewRequestId: string) {
  return useQuery({
    queryKey: ['review-progress', reviewRequestId],
    queryFn: () => fetchReviewProgress(reviewRequestId),
    enabled: reviewRequestId !== '',
  })
}
