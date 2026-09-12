import { delay } from '../../../shared/lib/delay'
import { WORKSPACE_MEMBER_FIXTURES, type WorkspaceMemberListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export async function fetchWorkspaceMembers(
  workspaceId: WorkspaceId,
): Promise<WorkspaceMemberListItem[]> {
  await delay()
  return WORKSPACE_MEMBER_FIXTURES.filter((m) => m.workspaceId === workspaceId)
}
