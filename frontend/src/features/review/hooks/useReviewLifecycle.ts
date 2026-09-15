import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  cancelReviewRequest,
  performReexamine,
  performRevise,
  type PerformReexamineInput,
} from '../api/reviewLifecycle'

/** 재교정·반영·취소가 끝나면 리뷰 화면 전체의 상태가 달라진다 — 관련 쿼리를 함께 턴다. */
function invalidateReviewState(
  queryClient: ReturnType<typeof useQueryClient>,
  reviewRequestId: string,
) {
  queryClient.invalidateQueries({ queryKey: ['review-progress', reviewRequestId] })
  queryClient.invalidateQueries({ queryKey: ['reviews', reviewRequestId] })
  queryClient.invalidateQueries({ queryKey: ['review-thread-comments', reviewRequestId] })
  queryClient.invalidateQueries({ queryKey: ['document-review-requests'] })
  queryClient.invalidateQueries({ queryKey: ['dictionary-revision'] })
}

export function usePerformReexamine(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: Omit<PerformReexamineInput, 'reviewRequestId'>) =>
      performReexamine({ ...input, reviewRequestId }),
    onSuccess: () => invalidateReviewState(queryClient, reviewRequestId),
  })
}

export function usePerformRevise(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => performRevise(reviewRequestId),
    onSuccess: () => {
      invalidateReviewState(queryClient, reviewRequestId)
      // 반영은 새 버전을 발행한다 — 사전집·문서 쪽도 낡는다.
      queryClient.invalidateQueries({ queryKey: ['dictionary'] })
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
  })
}

export function useCancelReviewRequest(reviewRequestId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => cancelReviewRequest(reviewRequestId),
    onSuccess: () => invalidateReviewState(queryClient, reviewRequestId),
  })
}
