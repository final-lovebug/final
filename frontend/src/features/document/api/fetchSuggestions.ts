import {
  fetchLatestDraftDocument,
  fetchSuggestionTermsOfDraft,
} from './draftDocumentApi'
import type { DraftDocument, SuggestionTerm } from '../model/types'
import type { DocumentId } from '../../../shared/types/ids'

export interface DocumentSuggestions {
  /** 대조를 한 번도 돌리지 않은 문서면 null — 화면은 "대조 결과가 없다"로 표시한다. */
  draft: DraftDocument | null
  /** `draft.draftBody`의 오프셋을 가리키는 제안어들. 초안이 없으면 빈 배열이다. */
  suggestions: SuggestionTerm[]
}

/**
 * 문서의 대조 결과(초안 본문 + 제안어)를 한 번에 읽는다.
 *
 * **본문과 제안어를 함께 돌려주는 이유** — 제안어의 `anchor`는 초안 본문(`draftBody`)의
 * 문자 오프셋이라 둘이 따로 오면 화면이 하이라이트를 그릴 수 없다. 예전에는 본문이
 * 화면에 하드코딩돼 있었고(`SUGGESTION_PARAGRAPHS`) 그래서 임의의 문서에서는 아무것도
 * 보이지 않았다 — T-INT-17에서 데이터 기반으로 바꾸며 이 함수가 둘을 함께 싣는다.
 *
 * id 체인은 `documentId → draftDocumentId → suggestion-terms` 두 홉이다.
 */
export async function fetchSuggestions(documentId: DocumentId): Promise<DocumentSuggestions> {
  const draft = await fetchLatestDraftDocument(documentId)
  if (!draft) {
    return { draft: null, suggestions: [] }
  }
  const suggestions = await fetchSuggestionTermsOfDraft(draft.id)
  return { draft, suggestions }
}
