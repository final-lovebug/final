import { useQuery } from '@tanstack/react-query'
import { fetchWorkspaceOverviews } from '../api/fetchWorkspaceOverviews'

export function useWorkspaceOverviews() {
  return useQuery({
    queryKey: ['workspace-overviews'],
    queryFn: fetchWorkspaceOverviews,
  })
}
