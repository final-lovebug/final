import { useQuery } from '@tanstack/react-query'
import { fetchRuleSet } from '../api/fetchRuleSet'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useRuleSet(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['ruleset', workspaceId],
    queryFn: () => fetchRuleSet(workspaceId),
  })
}
