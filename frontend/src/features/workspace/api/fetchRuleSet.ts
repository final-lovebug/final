import { delay } from '../../../shared/lib/delay'
import { RULE_SET_FIXTURE } from '../model/fixtures'
import type { RuleSet } from '../model/types'
import type { WorkspaceId } from '../../../shared/types/ids'

export async function fetchRuleSet(workspaceId: WorkspaceId): Promise<RuleSet | undefined> {
  await delay()
  return RULE_SET_FIXTURE.workspaceId === workspaceId ? RULE_SET_FIXTURE : undefined
}
