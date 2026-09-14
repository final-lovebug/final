import { useQuery } from '@tanstack/react-query'
import { fetchNotifications, type FetchNotificationsParams } from '../api/fetchNotifications'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useNotifications(workspaceId: WorkspaceId, params: FetchNotificationsParams = {}) {
  return useQuery({
    queryKey: ['notifications', workspaceId, params],
    queryFn: () => fetchNotifications(workspaceId, params),
    // 워크스페이스가 정해지기 전에 /api/workspaces//notifications 를 치지 않게 막는다.
    enabled: workspaceId !== '',
  })
}
