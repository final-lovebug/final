import { httpClient } from '../../../shared/api/httpClient'
import {
  toSuggestionTerm,
  type SuggestionTermApiResponse,
} from './draftDocumentApi'
import type { SuggestionTerm } from '../model/types'
import type { SuggestionTermId } from '../../../shared/types/ids'

/**
 * 제안어 판정 입력.
 *
 * **엔드포인트가 상태 하나로 묶여 있지 않다**(docs/API.md «제안어 판정과 교정완료») —
 * 수용은 `POST .../acceptance`, 유지는 `POST .../rejection`이고 후자는 사유가 필수다
 * (`@NotBlank rejectReason`). 그래서 상태 문자열 대신 판별 유니온으로 받는다.
 */
export type ResolveSuggestionInput =
  | { suggestionId: SuggestionTermId; decision: 'APPLY_SUGGESTION' }
  | { suggestionId: SuggestionTermId; decision: 'KEEP_ORIGINAL'; rejectReason: string }

export async function resolveSuggestion(
  input: ResolveSuggestionInput,
): Promise<SuggestionTerm> {
  const response =
    input.decision === 'APPLY_SUGGESTION'
      ? await httpClient.post<SuggestionTermApiResponse>(
          `/api/suggestion-terms/${input.suggestionId}/acceptance`,
        )
      : await httpClient.post<SuggestionTermApiResponse>(
          `/api/suggestion-terms/${input.suggestionId}/rejection`,
          { rejectReason: input.rejectReason },
        )
  return toSuggestionTerm(response)
}
