import { fetchSuggestions } from './fetchSuggestions'
import type { SuggestionHistoryItem, SuggestionTerm } from '../model/types'
import type { DocumentId } from '../../../shared/types/ids'

function toHistory(suggestions: SuggestionTerm[]): SuggestionHistoryItem[] {
  return suggestions
    .filter((suggestion) => suggestion.status !== 'PENDING')
    .map((suggestion) => ({
      original: suggestion.originTerm,
      result: suggestion.suggestionTerm,
      action: suggestion.status === 'APPLY_SUGGESTION' ? 'applied' : 'ignored',
      reason: suggestion.rejectReason ?? '사유 없음',
    }))
}

/**
 * "처리 내역" 뷰. **전용 엔드포인트가 없다** — 판정이 끝난 제안어가 곧 처리 내역이므로
 * 제안어 목록에서 파생한다(`status !== 'PENDING'`인 것만).
 *
 * `action: 'manual'`(직접 입력)은 실 API에 대응 개념이 없어 나오지 않는다 —
 * 자세한 사유는 `model/types.ts`의 `SuggestionHistoryItem` 주석 참고.
 */
export async function fetchSuggestionHistory(
  documentId: DocumentId,
): Promise<SuggestionHistoryItem[]> {
  const { suggestions } = await fetchSuggestions(documentId)

  return toHistory(suggestions)
}
