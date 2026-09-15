import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCandidateTerm } from '../api/createCandidateTerm'

export function useCreateCandidateTerm(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createCandidateTerm,
    onSuccess: () => {
      // 초안 한 벌(작성자·후보어·준비 여부)을 함께 담은 캐시를 갱신한다 —
      // 대표어·정의가 바뀌면 리뷰 요청 버튼의 활성 여부도 함께 바뀐다.
      queryClient.invalidateQueries({ queryKey: ['dictionary-draft', workspaceId] })
    },
  })
}
