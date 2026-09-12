import { useQuery } from '@tanstack/react-query'
import { fetchWorkspaces } from '../api/fetchWorkspaces'

export function useWorkspaces() {
  return useQuery({
    queryKey: ['workspaces'],
    queryFn: fetchWorkspaces,
  })
}
