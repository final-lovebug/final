import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acceptInvitation,
  cancelInvitation,
  fetchInvitations,
  issueInvitation,
} from '../api/invitations'
import type { InvitationStatus } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

function invitationsKey(workspaceId: WorkspaceId, status?: InvitationStatus) {
  return ['invitations', workspaceId, status ?? 'ALL'] as const
}

/**
 * 목록 조회는 **ADMIN 이상만** 허용된다(docs/API.md). Regular가 설정 화면을 열었을 때
 * 403을 만들지 않도록 `enabled`로 호출 자체를 막는다.
 */
export function useInvitations(
  workspaceId: WorkspaceId,
  status: InvitationStatus | undefined,
  options: { enabled?: boolean } = {},
) {
  return useQuery({
    queryKey: invitationsKey(workspaceId, status),
    queryFn: () => fetchInvitations(workspaceId, status),
    enabled: workspaceId !== '' && (options.enabled ?? true),
  })
}

export function useIssueInvitation(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: issueInvitation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invitations', workspaceId] })
    },
  })
}

export function useCancelInvitation(workspaceId: WorkspaceId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: cancelInvitation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invitations', workspaceId] })
    },
  })
}

/**
 * 수락은 워크스페이스를 모른 채 토큰으로만 들어오므로 workspaceId를 받지 않는다.
 * 성공하면 참여자가 늘어난 것이므로 워크스페이스 목록·개요를 낡은 것으로 표시한다.
 */
export function useAcceptInvitation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: acceptInvitation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workspaces'] })
      queryClient.invalidateQueries({ queryKey: ['workspace-overviews'] })
    },
  })
}
