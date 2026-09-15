import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteDocument } from '../api/deleteDocument'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

// 삭제된 문서의 상세 캐시는 무효화가 아니라 제거한다 — 무효화하면 곧바로 404를 받으러
// 다시 나간다. 목록은 남은 문서를 다시 받아야 하므로 무효화다.
export function useDeleteDocument(workspaceId: WorkspaceId, documentId: DocumentId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => deleteDocument(workspaceId, documentId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['document', workspaceId, documentId] })
      queryClient.invalidateQueries({ queryKey: ['documents', workspaceId] })
    },
  })
}
