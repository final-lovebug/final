// 문서 버전 비교(ui/main.js renderDocHistoryScreen의 "v2 → v3" 인라인 diff).
//
// **백엔드는 본문 diff를 만들어 주지 않는다** — 개정 이력은 "어떤 경로로 생겼는지"만
// 남기고 본문 비교는 하지 않기로 확정돼 있다(`D-61`: 취소선 인라인 비교는 문장 분할·오프셋
// 규격 `REQ-DOC-005`가 선행돼야 한다). 그래서 화면이 두 버전의 본문을 받아 여기서 만든다.
//
// 공백 단위 토큰 LCS다. 문장 분할·형태소 단위가 아니라 거친 근사이며, 그 한계를 화면이
// 문구로 밝힌다.

export type DiffPart =
  | { kind: 'same'; text: string }
  | { kind: 'removed'; text: string }
  | { kind: 'added'; text: string }

/**
 * LCS 표가 커지면 계산을 포기한다. 본문 상한이 10,000자라 최악의 경우 토큰이 수천 개인데,
 * 그때 n×m 표는 수백만 칸이 된다 — 브라우저를 멈추게 하느니 비교를 접는 편이 낫다.
 */
const MAX_TOKENS_FOR_DIFF = 1_500

/** 공백을 유지한 채 쪼갠다 — 합칠 때 원문이 그대로 복원돼야 한다. */
function tokenize(text: string): string[] {
  return text.split(/(\s+)/).filter((token) => token !== '')
}

function join(parts: string[]): string {
  return parts.join('')
}

export interface TextDiffResult {
  parts: DiffPart[]
  /** 토큰이 너무 많아 비교를 포기했다 — 화면은 두 본문을 그대로 보여준다. */
  tooLarge: boolean
}

export function diffWords(before: string, after: string): TextDiffResult {
  const beforeTokens = tokenize(before)
  const afterTokens = tokenize(after)

  // 앞뒤로 같은 부분을 먼저 떼어 내면 실제로 LCS를 돌릴 구간이 크게 줄어든다.
  let head = 0
  while (
    head < beforeTokens.length &&
    head < afterTokens.length &&
    beforeTokens[head] === afterTokens[head]
  ) {
    head += 1
  }

  let tail = 0
  while (
    tail < beforeTokens.length - head &&
    tail < afterTokens.length - head &&
    beforeTokens[beforeTokens.length - 1 - tail] === afterTokens[afterTokens.length - 1 - tail]
  ) {
    tail += 1
  }

  const beforeMiddle = beforeTokens.slice(head, beforeTokens.length - tail)
  const afterMiddle = afterTokens.slice(head, afterTokens.length - tail)

  if (beforeMiddle.length > MAX_TOKENS_FOR_DIFF || afterMiddle.length > MAX_TOKENS_FOR_DIFF) {
    return { parts: [], tooLarge: true }
  }

  const parts: DiffPart[] = []
  const prefix = join(beforeTokens.slice(0, head))
  if (prefix !== '') parts.push({ kind: 'same', text: prefix })

  // 표준 LCS 표. 행이 beforeMiddle, 열이 afterMiddle이다.
  const rows = beforeMiddle.length
  const columns = afterMiddle.length
  const table: number[][] = Array.from({ length: rows + 1 }, () => new Array(columns + 1).fill(0))
  for (let i = rows - 1; i >= 0; i -= 1) {
    for (let j = columns - 1; j >= 0; j -= 1) {
      table[i][j] =
        beforeMiddle[i] === afterMiddle[j]
          ? table[i + 1][j + 1] + 1
          : Math.max(table[i + 1][j], table[i][j + 1])
    }
  }

  const pending = { same: [] as string[], removed: [] as string[], added: [] as string[] }
  function flush() {
    if (pending.removed.length > 0) {
      parts.push({ kind: 'removed', text: join(pending.removed) })
      pending.removed = []
    }
    if (pending.added.length > 0) {
      parts.push({ kind: 'added', text: join(pending.added) })
      pending.added = []
    }
    if (pending.same.length > 0) {
      parts.push({ kind: 'same', text: join(pending.same) })
      pending.same = []
    }
  }

  let i = 0
  let j = 0
  while (i < rows && j < columns) {
    if (beforeMiddle[i] === afterMiddle[j]) {
      if (pending.removed.length > 0 || pending.added.length > 0) flush()
      pending.same.push(beforeMiddle[i])
      i += 1
      j += 1
    } else if (table[i + 1][j] >= table[i][j + 1]) {
      if (pending.same.length > 0) flush()
      pending.removed.push(beforeMiddle[i])
      i += 1
    } else {
      if (pending.same.length > 0) flush()
      pending.added.push(afterMiddle[j])
      j += 1
    }
  }
  while (i < rows) {
    if (pending.same.length > 0) flush()
    pending.removed.push(beforeMiddle[i])
    i += 1
  }
  while (j < columns) {
    if (pending.same.length > 0) flush()
    pending.added.push(afterMiddle[j])
    j += 1
  }
  flush()

  const suffix = join(beforeTokens.slice(beforeTokens.length - tail))
  if (suffix !== '') parts.push({ kind: 'same', text: suffix })

  return { parts, tooLarge: false }
}

/** 합친 본문에서 삭제·추가 구간이 차지하는 자리. `kind: 'same'`인 구간은 표시가 없어 담지 않는다. */
export interface DiffRange {
  kind: 'removed' | 'added'
  start: number
  end: number
}

export interface DiffSource {
  /** 삭제분과 추가분을 원래 자리에 함께 끼운 본문. 마크다운으로 그릴 대상이다. */
  text: string
  ranges: DiffRange[]
}

/**
 * diff 조각을 「마크다운으로 그릴 수 있는 한 벌의 본문 + 구간 목록」으로 합친다.
 *
 * 비교 화면은 삭제분(취소선)과 추가분(굵게)을 한 흐름 안에서 보여준다. 마크다운으로 그리려면
 * 그릴 대상이 **하나의 문자열**이어야 하므로 두 버전을 한 벌로 합치고, 어디가 삭제·추가인지는
 * 문자 구간으로 따로 들고 간다(`shared/ui/Markdown`의 `decorations`).
 *
 * **합친 본문은 어느 쪽 버전과도 완전히 같지 않다.** 제목 줄이 통째로 바뀐 경우처럼 블록
 * 표식이 두 번 나오면 구조가 한쪽으로 치우쳐 보일 수 있다 — 공백 단위 근사 비교라는 한계의
 * 연장이며, 화면이 그 문구를 갖고 있다.
 */
export function toDiffSource(parts: DiffPart[]): DiffSource {
  let text = ''
  const ranges: DiffRange[] = []

  for (const part of parts) {
    const start = text.length
    text += part.text
    if (part.kind !== 'same') ranges.push({ kind: part.kind, start, end: text.length })
  }

  return { text, ranges }
}
