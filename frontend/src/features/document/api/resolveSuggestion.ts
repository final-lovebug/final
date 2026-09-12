import { delay } from '../../../shared/lib/delay'
import { SUGGESTION_FIXTURES } from '../model/suggestionFixtures'
import type { SuggestionTerm, SuggestionTermStatus } from '../model/types'
import type { SuggestionTermId } from '../../../shared/types/ids'

export interface ResolveSuggestionInput {
  suggestionId: SuggestionTermId
  status: SuggestionTermStatus
}

// 실제 백엔드가 생기기 전까지만 메모리에 반영 — 새로고침하면 원래 상태로 돌아간다.
export async function resolveSuggestion({
  suggestionId,
  status,
}: ResolveSuggestionInput): Promise<SuggestionTerm> {
  await delay(200)
  const target = SUGGESTION_FIXTURES.find((s) => s.id === suggestionId)
  if (!target) throw new Error('제안을 찾을 수 없습니다.')
  target.status = status
  return target
}
