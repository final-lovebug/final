import { useQuery } from '@tanstack/react-query'
import { fetchSuggestions } from '../api/fetchSuggestions'
import type { DocumentId } from '../../../shared/types/ids'

/**
 * 문서의 대조 결과(초안 본문 + 제안어). 본문과 제안어를 함께 돌려주는 이유는
 * `api/fetchSuggestions.ts` 주석 참고 — 제안어 anchor가 초안 본문의 오프셋이다.
 */
export function useSuggestions(documentId: DocumentId) {
  return useQuery({
    queryKey: ['suggestions', documentId],
    queryFn: () => fetchSuggestions(documentId),
    enabled: documentId !== '',
  })
}
