import { useQuery } from '@tanstack/react-query'
import { fetchDocumentReviewRequests } from '../api/fetchDocumentReviewRequests'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDocumentReviewRequests(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['document-review-requests', workspaceId],
    queryFn: () => fetchDocumentReviewRequests(workspaceId),
  })
}
