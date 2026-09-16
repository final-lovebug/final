import type { SuggestionTerm } from './types'

// 초안 본문 위에 제안어를 얹기 위한 배치 규칙. 화면(pages)이 아니라 model에 두는 이유는
// 이것이 렌더링 기술과 무관한 도메인 규칙이기 때문이다 — anchor는 본문의 문자 오프셋이고
// (docs/API.md «제안어 등록·수정·삭제·목록»), 겹치는 구간은 하나만 살아남는다.
//
// 교정 중에는 초안 본문이 바뀌지 않는다 — 수용된 제안어의 실제 치환은 교정완료
// (`POST /api/draft-documents/{id}/examine-completion`) 시점에 한 번에 일어난다. 그래서
// 판정을 진행해도 anchor는 계속 유효하다.
//
// 본문을 조각으로 쪼개 주지 않고 **구간만** 돌려주는 이유는, 본문을 마크다운으로 그리기
// 때문이다(`shared/ui/Markdown`). 마크다운 뷰어가 같은 오프셋 기준으로 글자 노드를 쪼개므로
// 여기서는 「어느 구간이 어느 제안어인지」만 정하면 된다.

export interface PlacedSuggestion {
  start: number
  end: number
  suggestion: SuggestionTerm
}

/**
 * 제안어를 본문 구간에 배치한다.
 *
 * **범위를 벗어나거나 앞선 제안어와 겹치는 anchor는 건너뛴다.** 백엔드가 등록 시점에
 * 본문 길이를 검증하지만(`startOffset <= endOffset`, 본문 초과 거절), 초안 본문을 직접
 * 수정하면(`PATCH /api/draft-documents/{id}`) 기존 anchor가 어긋날 수 있다. 그때 화면이
 * 깨지는 대신 그 제안어만 본문 하이라이트에서 빠진다 — 우측 목록에는 그대로 남는다.
 *
 * 하이라이트 구간의 텍스트는 `originTerm`이 아니라 **본문에서 잘라낸 실제 문자열**이다
 * (뷰어가 본문에서 잘라 넘겨준다). 둘이 어긋나더라도 화면에 보이는 본문이 초안 본문과
 * 같아야 하기 때문이다.
 */
export function placeSuggestions(
  draftBody: string,
  suggestions: SuggestionTerm[],
): PlacedSuggestion[] {
  const placeable = suggestions
    .filter(
      (suggestion) =>
        suggestion.anchor.start >= 0 &&
        suggestion.anchor.end <= draftBody.length &&
        suggestion.anchor.start < suggestion.anchor.end,
    )
    .sort((a, b) => a.anchor.start - b.anchor.start)

  const placed: PlacedSuggestion[] = []
  let cursor = 0

  for (const suggestion of placeable) {
    // 앞선 제안어와 겹치면 건너뛴다 — 한 글자를 두 제안이 동시에 차지할 수 없다.
    if (suggestion.anchor.start < cursor) continue

    placed.push({ start: suggestion.anchor.start, end: suggestion.anchor.end, suggestion })
    cursor = suggestion.anchor.end
  }

  return placed
}

/**
 * 같은 제안어를 **교정이 끝난 본문**(개정안의 `proposedBody`) 위에 배치한다.
 *
 * anchor는 교정 **전** 본문 오프셋이라 그대로 쓰면 자리가 밀린다(`D-61`이 개정안 화면에서
 * 하이라이트를 포기한 이유). 치환은 결정적이므로 — 서버가 `DraftBodyComposer`에서 수용분만
 * 뒤에서부터 바꾼다 — 앞선 수용분의 길이 변화를 누적해 새 오프셋을 계산할 수 있다.
 *
 * **계산한 자리에 그 글자가 실제로 있는지 확인하고, 아니면 그 제안어만 뺀다.** 초안 본문을
 * 직접 고쳤거나(`PATCH /api/draft-documents/{id}`) 재교정 회차에서 본문이 달라졌으면 계산이
 * 어긋나는데, 그때 엉뚱한 자리를 강조하느니 강조하지 않는 편이 정직하다 — 표에는 그대로 남는다.
 */
export function placeSuggestionsInProposedBody(
  proposedBody: string,
  suggestions: SuggestionTerm[],
): PlacedSuggestion[] {
  const ordered = suggestions
    .filter((suggestion) => suggestion.anchor.start >= 0 && suggestion.anchor.start < suggestion.anchor.end)
    .sort((a, b) => a.anchor.start - b.anchor.start)

  const placed: PlacedSuggestion[] = []
  let shift = 0
  let cursor = -1

  for (const suggestion of ordered) {
    // 겹치는 anchor는 서버가 수용 시점에 거절하므로(`DraftBodyComposer.validateAnchors`)
    // 여기 들어오면 데이터가 어긋난 것이다 — 앞선 것만 남긴다.
    if (suggestion.anchor.start < cursor) continue
    cursor = suggestion.anchor.end

    const applied = suggestion.status === 'APPLY_SUGGESTION'
    const text = applied ? suggestion.suggestionTerm : suggestion.originTerm
    const start = suggestion.anchor.start + shift
    const end = start + text.length
    if (applied) {
      shift += suggestion.suggestionTerm.length - (suggestion.anchor.end - suggestion.anchor.start)
    }

    if (proposedBody.slice(start, end) !== text) continue
    placed.push({ start, end, suggestion })
  }

  return placed
}
