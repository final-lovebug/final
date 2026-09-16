// 마크다운 본문 파서.
//
// 문서는 `.md`로 올리는 경우가 많은데(업로드 화면이 `.md`·`.txt`를 받는다) 지금까지
// 상세·개정안 화면이 본문을 `whitespace-pre-wrap`으로만 찍어서 `## 도메인`이 그대로
// 보였다. 여기서 블록/인라인 구조만 뽑고, 실제 그리기는 `shared/ui/Markdown`이 한다.
//
// react-markdown 같은 라이브러리를 쓰지 않는다 — 신규 의존성은 사전 승인이 필요하고
// (`CLAUDE.md`), 사내 문서에 쓰이는 범위(제목·목록·표·코드·인용·강조·링크)는 이 정도로
// 충분하다. `cx.ts`와 같은 판단이다. HTML 문자열을 만들지 않고 노드 트리만 돌려주므로
// 렌더러가 `dangerouslySetInnerHTML` 없이 React 엘리먼트로 그린다 — 본문은 다른 참여자가
// 올린 값이라 원시 HTML을 그대로 주입하면 안 된다.
//
// **모든 글자 노드는 원문 오프셋(`offset`)을 갖고 다닌다.** 교정 하이라이트(제안어 anchor)와
// 버전 비교(diff)가 본문의 문자 위치로 구간을 지정하기 때문이다 — 오프셋이 없으면 그 두
// 화면은 마크다운으로 그릴 수 없다(`shared/ui/Markdown`의 `decorations`).
//
// **들여쓰기 코드 블록(4칸)은 일부러 지원하지 않는다.** `.txt`도 같은 뷰어로 보여주는데,
// 줄 맞춤을 들여쓴 평문이 통째로 코드로 보이는 편이 훨씬 나쁘다. 코드는 울타리(```)로만 본다.

export type InlineNode =
  | { kind: 'text'; text: string; offset: number; sourceLength: number }
  /** 문단 안의 줄바꿈. 마크다운 표준은 이어 붙이지만 여기서는 살린다(아래 주석 참고). */
  | { kind: 'break' }
  | { kind: 'strong'; children: InlineNode[] }
  | { kind: 'emphasis'; children: InlineNode[] }
  | { kind: 'strike'; children: InlineNode[] }
  | { kind: 'code'; text: string; offset: number }
  | { kind: 'link'; href: string; children: InlineNode[] }
  | { kind: 'image'; src: string; alt: string }

export type TableAlign = 'left' | 'center' | 'right'

export type MarkdownBlock =
  | { kind: 'heading'; level: number; children: InlineNode[] }
  | { kind: 'paragraph'; children: InlineNode[] }
  | { kind: 'code'; language: string | null; text: string; offset: number }
  | { kind: 'quote'; blocks: MarkdownBlock[] }
  | { kind: 'list'; ordered: boolean; start: number; items: MarkdownBlock[][] }
  | { kind: 'table'; aligns: TableAlign[]; header: InlineNode[][]; rows: InlineNode[][][] }
  | { kind: 'rule' }

/** 한 줄과 그 줄이 원문에서 시작하는 위치. 줄을 잘라 쓸 때 오프셋도 함께 옮긴다. */
interface SourceLine {
  text: string
  offset: number
}

const HEADING = /^(#{1,6})\s+(.*)$/
const FENCE = /^\s{0,3}(```+|~~~+)\s*([^`]*)$/
const RULE = /^\s{0,3}(?:(?:-\s*){3,}|(?:\*\s*){3,}|(?:_\s*){3,})$/
const BULLET_ITEM = /^(\s*)([-*+])\s+(.*)$/
const ORDERED_ITEM = /^(\s*)(\d{1,9})[.)]\s+(.*)$/
const QUOTE = /^\s{0,3}>\s?(.*)$/
const TABLE_DELIMITER = /^\s*\|?\s*:?-{1,}:?\s*(\|\s*:?-{1,}:?\s*)*\|?\s*$/

/** 표준 마크다운의 이스케이프 대상. `\*`처럼 적으면 기호 그대로 보여야 한다. */
const ESCAPABLE = '\\`*_{}[]()#+-.!|~>'

export function parseMarkdown(source: string): MarkdownBlock[] {
  return parseBlocks(toLines(source))
}

/**
 * 줄로 자르되 원문 오프셋을 유지한다.
 *
 * `\r\n`을 미리 `\n`으로 바꾸지 않는다 — 글자 수가 줄어 제안어 anchor가 통째로 밀린다.
 * 대신 줄 끝의 `\r`만 줄 안에서 떼어 낸다(줄 시작 위치는 그대로다).
 */
function toLines(source: string): SourceLine[] {
  const lines: SourceLine[] = []
  let offset = 0

  for (const raw of source.split('\n')) {
    lines.push({ text: raw.endsWith('\r') ? raw.slice(0, -1) : raw, offset })
    offset += raw.length + 1
  }

  return lines
}

/** 줄 앞부분을 떼어 낸 나머지. 뗀 만큼 오프셋을 민다. */
function sliceLine(line: SourceLine, from: number): SourceLine {
  return { text: line.text.slice(from), offset: line.offset + from }
}

/** 정규식 그룹이 줄 끝까지 가는 경우(`(.*)$`)의 시작 위치. */
function tailStart(line: SourceLine, tail: string): number {
  return line.text.length - tail.length
}

function parseBlocks(lines: SourceLine[]): MarkdownBlock[] {
  const blocks: MarkdownBlock[] = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]

    if (line.text.trim() === '') {
      index += 1
      continue
    }

    const fence = FENCE.exec(line.text)
    if (fence) {
      const marker = fence[1].slice(0, 3)
      const body: SourceLine[] = []
      index += 1
      // 닫는 울타리가 없으면 문서 끝까지가 코드다 — 본문이 잘려 보이는 편보다 낫다.
      while (index < lines.length && !lines[index].text.trimStart().startsWith(marker)) {
        body.push(lines[index])
        index += 1
      }
      index += 1
      blocks.push({
        kind: 'code',
        language: fence[2].trim() || null,
        text: body.map((bodyLine) => bodyLine.text).join('\n'),
        offset: body[0]?.offset ?? line.offset + line.text.length,
      })
      continue
    }

    const heading = HEADING.exec(line.text)
    if (heading) {
      // 닫는 `##`(`## 제목 ##`)은 장식이라 떼어 낸다. 뒤쪽만 건드리므로 오프셋은 그대로다.
      const text = heading[2].replace(/\s+#+\s*$/, '')
      blocks.push({
        kind: 'heading',
        level: heading[1].length,
        children: parseInline(text, line.offset + tailStart(line, heading[2])),
      })
      index += 1
      continue
    }

    if (RULE.test(line.text)) {
      blocks.push({ kind: 'rule' })
      index += 1
      continue
    }

    if (QUOTE.test(line.text)) {
      const quoted: SourceLine[] = []
      while (index < lines.length && lines[index].text.trim() !== '') {
        const match = QUOTE.exec(lines[index].text)
        // 인용 안에서 `>`가 빠진 줄도 같은 인용의 이어짐으로 본다(lazy continuation).
        quoted.push(match ? sliceLine(lines[index], tailStart(lines[index], match[1])) : lines[index])
        index += 1
      }
      blocks.push({ kind: 'quote', blocks: parseBlocks(quoted) })
      continue
    }

    const table = parseTable(lines, index)
    if (table) {
      blocks.push(table.block)
      index = table.nextIndex
      continue
    }

    if (matchItem(line.text)) {
      const list = parseList(lines, index)
      blocks.push(list.block)
      index = list.nextIndex
      continue
    }

    const paragraph: SourceLine[] = []
    while (
      index < lines.length &&
      lines[index].text.trim() !== '' &&
      !startsNewBlock(lines, index)
    ) {
      const current = lines[index]
      paragraph.push(sliceLine(current, current.text.length - current.text.trimStart().length))
      index += 1
    }
    blocks.push({ kind: 'paragraph', children: parseInlineLines(paragraph) })
  }

  return blocks
}

/** 문단을 읽다가 여기서 끊어야 하는 줄인지. 빈 줄 없이 목록·제목이 이어지는 문서가 흔하다. */
function startsNewBlock(lines: SourceLine[], index: number): boolean {
  const text = lines[index].text
  return (
    HEADING.test(text) ||
    FENCE.test(text) ||
    RULE.test(text) ||
    QUOTE.test(text) ||
    matchItem(text) !== null ||
    parseTable(lines, index) !== null
  )
}

interface ListParseResult {
  block: Extract<MarkdownBlock, { kind: 'list' }>
  nextIndex: number
}

function parseList(lines: SourceLine[], start: number): ListParseResult {
  const first = matchItem(lines[start].text)!
  const ordered = first.ordered
  const baseIndent = first.indent
  const items: MarkdownBlock[][] = []

  let index = start
  let current: SourceLine[] | null = null

  function flush() {
    if (current) items.push(parseBlocks(current))
    current = null
  }

  while (index < lines.length) {
    const line = lines[index]

    if (line.text.trim() === '') {
      // 빈 줄 다음에도 같은 목록이 이어질 수 있다(느슨한 목록). 항목 내부 여백으로 넘긴다.
      const next = lines[index + 1]
      if (next === undefined || next.text.trim() === '') break
      const nextIndent = next.text.length - next.text.trimStart().length
      const nextItem = matchItem(next.text)
      if (nextIndent <= baseIndent && (nextItem === null || nextItem.indent > baseIndent)) break
      current?.push({ text: '', offset: line.offset })
      index += 1
      continue
    }

    const item = matchItem(line.text)
    const indent = line.text.length - line.text.trimStart().length

    if (item && item.indent <= baseIndent) {
      // 다른 기호로 시작하면 별개의 목록이다(`- ` 목록 뒤의 `1. ` 목록).
      if (item.ordered !== ordered) break
      flush()
      current = [sliceLine(line, tailStart(line, item.text))]
      index += 1
      continue
    }

    if (current === null) break
    if (indent <= baseIndent && item === null) break

    // 항목에 딸린 줄은 표식만큼 들여쓰기를 걷어 내고 항목 본문으로 다시 파싱한다 —
    // 중첩 목록·항목 안의 코드 블록이 이 재귀로 처리된다.
    current.push(sliceLine(line, Math.min(indent, baseIndent + 2)))
    index += 1
  }

  flush()

  return {
    block: { kind: 'list', ordered, start: ordered ? first.number : 1, items },
    nextIndex: index,
  }
}

interface ItemMatch {
  indent: number
  ordered: boolean
  number: number
  text: string
}

function matchItem(line: string): ItemMatch | null {
  const bullet = BULLET_ITEM.exec(line)
  if (bullet) return { indent: bullet[1].length, ordered: false, number: 1, text: bullet[3] }

  const numbered = ORDERED_ITEM.exec(line)
  if (numbered) {
    return {
      indent: numbered[1].length,
      ordered: true,
      number: Number(numbered[2]),
      text: numbered[3],
    }
  }

  return null
}

interface TableParseResult {
  block: Extract<MarkdownBlock, { kind: 'table' }>
  nextIndex: number
}

function parseTable(lines: SourceLine[], start: number): TableParseResult | null {
  const header = lines[start]
  const delimiter = lines[start + 1]
  if (header === undefined || !header.text.includes('|')) return null
  if (delimiter === undefined || !TABLE_DELIMITER.test(delimiter.text)) return null
  if (!delimiter.text.includes('-')) return null

  const headerCells = splitRow(header)
  const alignCells = splitRow(delimiter)
  if (headerCells.length < 2 || headerCells.length !== alignCells.length) return null

  const aligns: TableAlign[] = alignCells.map((cell) => {
    const left = cell.text.startsWith(':')
    const right = cell.text.endsWith(':')
    if (left && right) return 'center'
    if (right) return 'right'
    return 'left'
  })

  const rows: InlineNode[][][] = []
  let index = start + 2
  while (index < lines.length && lines[index].text.trim() !== '' && lines[index].text.includes('|')) {
    const cells = splitRow(lines[index])
    // 칸 수가 맞지 않아도 버리지 않는다 — 모자라면 빈 칸으로 채우고 넘치면 자른다.
    rows.push(
      aligns.map((_, column) => {
        const cell = cells[column]
        return cell ? parseInline(cell.text, cell.offset) : []
      }),
    )
    index += 1
  }

  return {
    block: {
      kind: 'table',
      aligns,
      header: headerCells.map((cell) => parseInline(cell.text, cell.offset)),
      rows,
    },
    nextIndex: index,
  }
}

/**
 * 표 한 줄을 칸으로 자른다. 칸마다 원문 오프셋을 함께 돌려준다.
 *
 * `\|`는 여기서 풀지 않고 그대로 넘긴다 — 인라인 파서가 이스케이프를 처리하므로 칸 안의
 * 글자 위치가 원문과 어긋나지 않는다.
 */
function splitRow(line: SourceLine): SourceLine[] {
  let text = line.text
  let base = line.offset

  const leading = text.length - text.trimStart().length
  text = text.trim()
  base += leading
  if (text.startsWith('|')) {
    text = text.slice(1)
    base += 1
  }
  if (text.endsWith('|')) text = text.slice(0, -1)

  const cells: SourceLine[] = []
  let cellStart = 0

  for (let index = 0; index < text.length; index += 1) {
    if (text[index] === '\\') {
      index += 1
      continue
    }
    if (text[index] === '|') {
      cells.push(cellAt(text, cellStart, index, base))
      cellStart = index + 1
    }
  }
  cells.push(cellAt(text, cellStart, text.length, base))

  return cells
}

function cellAt(text: string, from: number, to: number, base: number): SourceLine {
  const raw = text.slice(from, to)
  const leading = raw.length - raw.trimStart().length
  return { text: raw.trim(), offset: base + from + leading }
}

/**
 * 문단 안의 줄바꿈을 `break`로 남긴다.
 *
 * 표준 마크다운은 한 줄 바꿈을 공백으로 합치지만, 업로드 화면이 `.txt`도 받기 때문에
 * 마크다운이 아닌 본문은 줄바꿈이 사라지면 형태가 무너진다. 줄바꿈을 살리는 쪽(GFM의
 * `breaks` 옵션과 같다)이 두 경우 모두에서 원문에 가깝다.
 */
function parseInlineLines(lines: SourceLine[]): InlineNode[] {
  const nodes: InlineNode[] = []

  lines.forEach((line, index) => {
    if (index > 0) nodes.push({ kind: 'break' })
    nodes.push(...parseInline(line.text.replace(/\s+$/, ''), line.offset))
  })

  return nodes
}

export function parseInline(text: string, offset = 0): InlineNode[] {
  const nodes: InlineNode[] = []
  let buffer = ''
  let bufferStart = 0
  let index = 0

  function flush() {
    if (buffer !== '') {
      nodes.push({
        kind: 'text',
        text: buffer,
        offset: offset + bufferStart,
        sourceLength: buffer.length,
      })
      buffer = ''
    }
  }

  while (index < text.length) {
    const rest = text.slice(index)
    const char = text[index]

    if (char === '\\' && ESCAPABLE.includes(text[index + 1] ?? '')) {
      // 이스케이프는 원문 2글자가 1글자가 된다. 앞의 글자 뭉치와 섞으면 그 뒤의 오프셋이
      // 전부 한 칸씩 밀리므로, 끊어서 「원문 2글자짜리 글자 노드」로 따로 담는다.
      flush()
      nodes.push({ kind: 'text', text: text[index + 1], offset: offset + index, sourceLength: 2 })
      index += 2
      bufferStart = index
      continue
    }

    if (char === '`') {
      const code = /^(`+)([\s\S]*?)\1(?!`)/.exec(rest)
      if (code) {
        flush()
        const padded = /^ (.*) $/.exec(code[2])
        nodes.push({
          kind: 'code',
          text: padded ? padded[1] : code[2],
          offset: offset + index + code[1].length + (padded ? 1 : 0),
        })
        index += code[0].length
        bufferStart = index
        continue
      }
    }

    if (char === '!' && text[index + 1] === '[') {
      const image = LINK_PATTERN.exec(text.slice(index + 1))
      if (image) {
        const src = safeUrl(image[2])
        flush()
        // 허용하지 않는 스킴이면 대체 텍스트만 남긴다.
        nodes.push(
          src === null
            ? { kind: 'text', text: image[1], offset: offset + index, sourceLength: image[0].length + 1 }
            : { kind: 'image', src, alt: image[1] },
        )
        index += image[0].length + 1
        bufferStart = index
        continue
      }
    }

    if (char === '[') {
      const link = LINK_PATTERN.exec(rest)
      if (link) {
        const href = safeUrl(link[2])
        flush()
        // `javascript:` 같은 스킴은 링크로 만들지 않고 글자만 남긴다.
        nodes.push(
          href === null
            ? { kind: 'text', text: link[1], offset: offset + index, sourceLength: link[0].length }
            : {
                kind: 'link',
                href,
                // 링크 글자는 `[` 다음에서 시작한다.
                children: parseInline(link[1], offset + index + 1),
              },
        )
        index += link[0].length
        bufferStart = index
        continue
      }
    }

    const emphasis = matchEmphasis(rest, index === 0 ? '' : text[index - 1])
    if (emphasis) {
      flush()
      nodes.push(emphasis.build(offset + index))
      index += emphasis.length
      bufferStart = index
      continue
    }

    if (buffer === '') bufferStart = index
    buffer += char
    index += 1
  }

  flush()
  return nodes
}

const LINK_PATTERN =
  /^\[((?:[^[\]\\]|\\.)*)\]\(\s*<?((?:[^\s<>()]|\([^\s()]*\))*)>?(?:\s+"[^"]*")?\s*\)/

const EMPHASIS_MARKERS: Array<{ marker: string; kind: 'strong' | 'emphasis' | 'strike' }> = [
  { marker: '~~', kind: 'strike' },
  { marker: '**', kind: 'strong' },
  { marker: '__', kind: 'strong' },
  { marker: '*', kind: 'emphasis' },
  { marker: '_', kind: 'emphasis' },
]

interface EmphasisMatch {
  /** 표식이 원문에서 차지하는 길이. */
  length: number
  build: (nodeOffset: number) => InlineNode
}

function matchEmphasis(rest: string, previousChar: string): EmphasisMatch | null {
  for (const { marker, kind } of EMPHASIS_MARKERS) {
    if (!rest.startsWith(marker)) continue
    // `snake_case`·`user_id`가 기울임으로 바뀌면 안 된다 — `_`는 단어 경계에서만 연다.
    if (marker.startsWith('_') && /[\p{L}\p{N}]/u.test(previousChar)) continue

    const closing = findClosing(rest, marker)
    if (closing === -1) continue

    const content = rest.slice(marker.length, closing)
    if (content.trim() === '') continue

    return {
      length: closing + marker.length,
      build: (nodeOffset) => ({
        kind,
        children: parseInline(content, nodeOffset + marker.length),
      }),
    }
  }

  return null
}

function findClosing(rest: string, marker: string): number {
  let index = marker.length

  while (index < rest.length) {
    if (rest[index] === '\\') {
      index += 2
      continue
    }
    if (rest.startsWith(marker, index)) {
      // `*`는 `**`의 일부일 수 있다. 여는 것과 같은 길이의 표식만 닫는 것으로 본다.
      if (marker.length === 1 && rest[index + 1] === marker) {
        index += 2
        continue
      }
      return index
    }
    index += 1
  }

  return -1
}

/** `http(s)`·`mailto`·문서 내부 앵커·상대 경로만 허용한다. 나머지는 링크로 만들지 않는다. */
function safeUrl(url: string): string | null {
  const trimmed = url.trim()
  if (trimmed === '') return null
  if (/^(https?:|mailto:)/i.test(trimmed)) return trimmed
  if (/^[a-z][a-z0-9+.-]*:/i.test(trimmed)) return null
  return trimmed
}

/**
 * 목록의 한 줄 발췌용 — 마크다운 기호를 걷어 낸 본문 글자만 남긴다.
 *
 * 발췌 칸은 한 줄로 잘리므로(`line-clamp-1`) 블록 구조를 그릴 수 없다. 대신 `# 제목`·`**굵게**`
 * 같은 기호가 그대로 보이지 않도록 파서를 한 번 태워 글자만 뽑는다. 앞부분만 필요하니
 * 본문 전체를 파싱하지 않는다 — 목록 한 화면에 수십 건이 그려진다.
 */
export function markdownToPlainText(source: string, maxLength = 200): string {
  const head = source.slice(0, maxLength * 2)
  const text = parseMarkdown(head)
    .map(plainTextOfBlock)
    .filter((part) => part !== '')
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim()

  return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text
}

function plainTextOfBlock(block: MarkdownBlock): string {
  switch (block.kind) {
    case 'heading':
    case 'paragraph':
      return plainTextOfInline(block.children)
    case 'code':
      return block.text
    case 'quote':
      return block.blocks.map(plainTextOfBlock).join(' ')
    case 'list':
      return block.items.map((item) => item.map(plainTextOfBlock).join(' ')).join(' ')
    case 'table':
      return [block.header, ...block.rows]
        .map((row) => row.map(plainTextOfInline).join(' '))
        .join(' ')
    case 'rule':
      return ''
  }
}

function plainTextOfInline(nodes: InlineNode[]): string {
  return nodes
    .map((node) => {
      switch (node.kind) {
        case 'text':
        case 'code':
          return node.text
        case 'break':
          return ' '
        case 'image':
          return node.alt
        case 'strong':
        case 'emphasis':
        case 'strike':
        case 'link':
          return plainTextOfInline(node.children)
      }
    })
    .join('')
}

/** 본문의 한 구간. 교정 하이라이트(anchor)와 버전 비교(diff)가 이 모양으로 구간을 말한다. */
export interface SourceRange {
  start: number
  end: number
}

export interface TextPiece {
  text: string
  /** 이 조각을 차지한 구간의 번호. 어느 구간에도 속하지 않으면 `null`이다. */
  rangeIndex: number | null
}

/**
 * 글자 한 덩어리를 구간 경계에서 쪼갠다. 뷰어가 하이라이트를 얹는 데 쓴다.
 *
 * `sourceLength`가 표시 길이와 다른 덩어리(`\*`처럼 이스케이프된 한 글자)는 원문 위치와
 * 표시 위치가 1:1이 아니라 쪼갤 수 없다 — 겹치는 구간이 있으면 통째로 그 구간에 넣는다.
 * 한 글자라 시각적으로 차이가 없다.
 *
 * 구간 목록은 `start` 오름차순이고 서로 겹치지 않아야 한다(`sortRanges`).
 */
export function splitByRanges(
  text: string,
  offset: number,
  sourceLength: number,
  ranges: SourceRange[],
): TextPiece[] {
  const end = offset + sourceLength
  const overlapping = ranges
    .map((range, index) => ({ range, index }))
    .filter(({ range }) => range.start < end && range.end > offset)

  if (overlapping.length === 0) return [{ text, rangeIndex: null }]
  if (sourceLength !== text.length) return [{ text, rangeIndex: overlapping[0].index }]

  const pieces: TextPiece[] = []
  let cursor = offset

  for (const { range, index } of overlapping) {
    const from = Math.max(range.start, offset)
    const to = Math.min(range.end, end)
    if (from > cursor) pieces.push({ text: text.slice(cursor - offset, from - offset), rangeIndex: null })
    pieces.push({ text: text.slice(from - offset, to - offset), rangeIndex: index })
    cursor = to
  }
  if (cursor < end) pieces.push({ text: text.slice(cursor - offset), rangeIndex: null })

  return pieces
}

/** 구간을 앞에서부터 정렬하고, 겹치는 뒤쪽 구간은 버린다 — 한 글자를 둘이 차지할 수 없다. */
export function sortRanges<T extends SourceRange>(ranges: T[]): T[] {
  const sorted = [...ranges].filter((range) => range.start < range.end).sort((a, b) => a.start - b.start)

  const kept: T[] = []
  let cursor = 0
  for (const range of sorted) {
    if (range.start < cursor) continue
    kept.push(range)
    cursor = range.end
  }

  return kept
}
