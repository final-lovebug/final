import { useMutation, useQueryClient } from '@tanstack/react-query'
import { submitDictionaryRevision } from '../api/submitDictionaryRevision'
import type { WorkspaceId } from '../../../shared/types/ids'

/**
 * 리뷰 요청 — 교정 완료 처리 후 리뷰 요청과 최초 사전 개정안을 만든다
 * (`docs/plan/DRAFT_PLAN.md`).
 *
 * 준비 여부(대표어·정의)는 `useDictionaryDraft`가 이미 계산해 두므로 여기서 다시 묻지 않는다 —
 * 최종 판정은 백엔드 `DraftDictionaryReviewReadinessValidator`가 한다.
 */
export function useSubmitDictionaryRevision(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: submitDictionaryRevision,
    onSuccess: () => {
      // 초안이 REVIEW_REQUESTED 로 넘어가고 개정안이 새로 생긴다.
      queryClient.invalidateQueries({ queryKey: ['dictionary-draft', workspaceId] })
      queryClient.invalidateQueries({ queryKey: ['dictionary-revision', workspaceId] })
    },
  })
}
