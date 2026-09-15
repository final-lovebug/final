import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  changeParticipantPermission,
  removeParticipant,
} from '../api/participants'
import type { WorkspaceId } from '../../../shared/types/ids'

/** 참여자 목록이 바뀌면 멤버 표와 탑바 아바타 스택이 함께 낡는다. */
function invalidateMemberViews(
  queryClient: ReturnType<typeof useQueryClient>,
  workspaceId: WorkspaceId,
) {
  queryClient.invalidateQueries({ queryKey: ['workspace-members', workspaceId] })
  queryClient.invalidateQueries({ queryKey: ['workspace-overviews'] })
}

export function useRemoveParticipant(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: removeParticipant,
    onSuccess: () => invalidateMemberViews(queryClient, workspaceId),
  })
}

export function useChangeParticipantPermission(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: changeParticipantPermission,
    onSuccess: () => invalidateMemberViews(queryClient, workspaceId),
  })
}
