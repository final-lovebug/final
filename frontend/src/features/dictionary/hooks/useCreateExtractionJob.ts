import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createExtractionJob } from '../api/createExtractionJob'

/**
 * 용어 추출 작업 접수(DI-5). 성공하면 작업이 하나 생기므로 그 워크스페이스의 사전 초안
 * 목록을 무효화한다 — 접수 시점엔 아직 초안이 없지만, 완료 후 화면을 다시 열었을 때
 * 낡은 목록이 남지 않게 한다.
 */
export function useCreateExtractionJob(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createExtractionJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dictionary-candidates', workspaceId] })
    },
  })
}
