import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCheckJob } from '../api/checkJobApi'
import type { DocumentId } from '../../../shared/types/ids'

/**
 * 문서 대조 작업 접수(DD-5). 성공하면 곧 새 초안이 생기므로 그 문서의 제안어·초안
 * 조회를 무효화한다 — 작업이 끝나는 시점은 폴링이 알려준다.
 */
export function useCreateCheckJob(workspaceId: string, documentId: DocumentId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => createCheckJob(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['draft-document', documentId] })
      queryClient.invalidateQueries({ queryKey: ['suggestions', documentId] })
      queryClient.invalidateQueries({ queryKey: ['draft-documents', workspaceId] })
    },
  })
}
