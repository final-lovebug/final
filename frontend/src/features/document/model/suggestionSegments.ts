import type { SuggestionTerm } from './types'

// 초안 본문 위에 제안어를 얹기 위한 분할 규칙. 화면(pages)이 아니라 model에 두는 이유는
// 이것이 렌더링 기술과 무관한 도메인 규칙이기 때문이다 — anchor는 본문의 문자 오프셋이고
// (docs/API.md «제안어 등록·수정·삭제·목록»), 겹치는 구간은 하나만 살아남는다.
//
// 교정 중에는 초안 본문이 바뀌지 않는다 — 수용된 제안어의 실제 치환은 교정완료
// (`POST /api/draft-documents/{id}/examine-completion`) 시점에 한 번에 일어난다. 그래서
// 판정을 진행해도 anchor는 계속 유효하다.

export type SuggestionSegment =
  | { kind: 'text'; text: string }
  | { kind: 'suggestion'; text: string; suggestion: SuggestionTerm }

/**
 * 초안 본문을 제안어 구간 기준으로 쪼갠다.
 *
 * **범위를 벗어나거나 앞선 제안어와 겹치는 anchor는 건너뛴다.** 백엔드가 등록 시점에
 * 본문 길이를 검증하지만(`startOffset <= endOffset`, 본문 초과 거절), 초안 본문을 직접
 * 수정하면(`PATCH /api/draft-documents/{id}`) 기존 anchor가 어긋날 수 있다. 그때 화면이
 * 깨지는 대신 그 제안어만 본문 하이라이트에서 빠진다 — 우측 목록에는 그대로 남는다.
 *
 * 하이라이트 구간의 텍스트는 `originTerm`이 아니라 **본문에서 잘라낸 실제 문자열**이다.
 * 둘이 어긋나더라도 화면에 보이는 본문이 초안 본문과 같아야 하기 때문이다.
 */
export function buildSuggestionSegments(
  draftBody: string,
  suggestions: SuggestionTerm[],
): SuggestionSegment[] {
  const placeable = suggestions
    .filter(
      (suggestion) =>
        suggestion.anchor.start >= 0 &&
        suggestion.anchor.end <= draftBody.length &&
        suggestion.anchor.start < suggestion.anchor.end,
    )
    .sort((a, b) => a.anchor.start - b.anchor.start)

  const segments: SuggestionSegment[] = []
  let cursor = 0

  for (const suggestion of placeable) {
    // 앞선 제안어와 겹치면 건너뛴다 — 한 글자를 두 제안이 동시에 차지할 수 없다.
    if (suggestion.anchor.start < cursor) continue

    if (suggestion.anchor.start > cursor) {
      segments.push({ kind: 'text', text: draftBody.slice(cursor, suggestion.anchor.start) })
    }
    segments.push({
      kind: 'suggestion',
      text: draftBody.slice(suggestion.anchor.start, suggestion.anchor.end),
      suggestion,
    })
    cursor = suggestion.anchor.end
  }

  if (cursor < draftBody.length) {
    segments.push({ kind: 'text', text: draftBody.slice(cursor) })
  }
  return segments
}
