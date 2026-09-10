import { useQuery } from '@tanstack/react-query'
import { fetchExtractionEligibleDocuments } from '../api/fetchExtractionEligibleDocuments'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useExtractionEligibleDocuments(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['extraction-eligible-documents', workspaceId],
    queryFn: () => fetchExtractionEligibleDocuments(workspaceId),
  })
}
