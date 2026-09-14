import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchCandidateExamineProgress,
  submitDictionaryRevision,
} from '../api/submitDictionaryRevision'
import type { WorkspaceId } from '../../../shared/types/ids'

/** 사전 초안의 후보어 판정 진행률. "개정안 제출"을 열어 줄지 정하는 데 쓴다. */
export function useCandidateExamineProgress(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['candidate-examine-progress', workspaceId],
    queryFn: () => fetchCandidateExamineProgress(workspaceId),
    enabled: workspaceId !== '',
  })
}

export function useSubmitDictionaryRevision(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: submitDictionaryRevision,
    onSuccess: () => {
      // 초안이 REVIEW_REQUESTED로 넘어가므로 후보어·진행률·개정안이 모두 바뀐다.
      queryClient.invalidateQueries({ queryKey: ['dictionary-candidates', workspaceId] })
      queryClient.invalidateQueries({ queryKey: ['candidate-examine-progress', workspaceId] })
      queryClient.invalidateQueries({ queryKey: ['dictionary-revision', workspaceId] })
    },
  })
}
