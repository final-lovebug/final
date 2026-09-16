import { useQuery } from '@tanstack/react-query'
import { fetchSuggestionTermsOfDraft } from '../api/draftDocumentApi'
import type { DraftDocumentId } from '../../../shared/types/ids'

/**
 * 초안의 제안어 전체.
 *
 * 개정안 검토 화면이 **용어별 코멘트의 대상 목록**으로 쓴다 — 코멘트의 `targetItemId`가
 * 제안어 id다(사전집 개정안에서 후보어 id를 쓰는 것과 같은 자리). 초안 id를 아직 모르면 쉰다.
 */
export function useSuggestionTermsOfDraft(draftDocumentId: DraftDocumentId | null) {
  return useQuery({
    queryKey: ['suggestion-terms-of-draft', draftDocumentId],
    queryFn: () => fetchSuggestionTermsOfDraft(draftDocumentId as DraftDocumentId),
    enabled: draftDocumentId !== null && draftDocumentId !== '',
  })
}
