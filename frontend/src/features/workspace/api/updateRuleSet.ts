import { delay } from '../../../shared/lib/delay'
import { RULE_SET_FIXTURE } from '../model/fixtures'
import type { RuleSet } from '../model/types'

export interface UpdateRuleSetInput {
  requiredDocumentReviewerCount: number
  requiredDictionaryReviewerCount: number
}

// 실제 백엔드가 생기기 전까지만 메모리에 반영.
export async function updateRuleSet(input: UpdateRuleSetInput): Promise<RuleSet> {
  await delay(200)
  RULE_SET_FIXTURE.requiredDocumentReviewerCount = input.requiredDocumentReviewerCount
  RULE_SET_FIXTURE.requiredDictionaryReviewerCount = input.requiredDictionaryReviewerCount
  RULE_SET_FIXTURE.updatedAt = new Date().toISOString()
  return RULE_SET_FIXTURE
}
