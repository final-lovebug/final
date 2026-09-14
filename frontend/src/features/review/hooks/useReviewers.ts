import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { assignReviewer, fetchReviewers, removeReviewer } from '../api/reviewers'

export function useReviewers(reviewRequestId: string) {
  return useQuery({
    queryKey: ['reviewers', reviewRequestId],
    queryFn: () => fetchReviewers(reviewRequestId),
    enabled: reviewRequestId !== '',
  })
}

export function useAssignReviewer(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (memberId: string) => assignReviewer(reviewRequestId, memberId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviewers', reviewRequestId] })
      queryClient.invalidateQueries({ queryKey: ['document-review-requests'] })
    },
  })
}

export function useRemoveReviewer(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (reviewerId: string) => removeReviewer(reviewRequestId, reviewerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviewers', reviewRequestId] })
      queryClient.invalidateQueries({ queryKey: ['document-review-requests'] })
    },
  })
}
