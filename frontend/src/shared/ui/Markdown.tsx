import { createContext, Fragment, useContext, useMemo, type ReactNode } from 'react'
import { cx } from '../lib/cx'
import {
  parseMarkdown,
  sortRanges,
  splitByRanges,
  type InlineNode,
  type MarkdownBlock,
  type TableAlign,
} from '../lib/markdown'

// 마크다운 본문 뷰어.
//
// 업로드 화면이 `.md`를 받으므로(`DocumentUploadPage`) 본문도 마크다운으로 보여야 한다 —
// 전에는 `whitespace-pre-wrap`으로만 찍어서 `## 도메인`이 기호째 노출됐다.
//
// 파서가 돌려준 노드를 React 엘리먼트로 그린다. `dangerouslySetInnerHTML`은 쓰지 않는다 —
// 본문은 다른 참여자가 올린 값이라 원시 HTML 주입 경로를 만들면 안 된다.

/**
 * 본문의 특정 구간을 감싸 그리는 규칙.
 *
 * 교정 하이라이트(제안어 anchor)와 버전 비교(diff)가 이걸 쓴다. 두 화면 모두 **본문의 문자
 * 위치**로 구간을 말하므로, 파서가 들고 온 원문 오프셋과 맞춰 글자 노드를 쪼갠다 — 그래서
 * 마크다운으로 그리면서도 하이라이트 위치가 어긋나지 않는다.
 *
 * `start`/`end`는 원문(마크다운 기호를 포함한 원본 문자열)의 오프셋이다.
 */
export interface MarkdownDecoration {
  start: number
  end: number
  render: (text: string, key: string) => ReactNode
}

interface MarkdownProps {
  source: string
  className?: string
  decorations?: MarkdownDecoration[]
}

const DecorationContext = createContext<MarkdownDecoration[]>([])

export function Markdown({ source, className, decorations }: MarkdownProps) {
  const blocks = useMemo(() => parseMarkdown(source), [source])
  // 겹치는 구간은 앞선 것이 이긴다 — 한 글자를 두 규칙이 동시에 차지할 수 없다.
  const ranges = useMemo(() => sortRanges(decorations ?? []), [decorations])

  return (
    <DecorationContext.Provider value={ranges}>
      <div className={cx('wrap-break-word', className)}>
        {blocks.map((block, index) => (
          <Block key={index} block={block} />
        ))}
      </div>
    </DecorationContext.Provider>
  )
}

/**
 * 글자 한 덩어리를 구간 규칙에 따라 쪼개 그린다. 쪼개는 규칙 자체는 순수 함수로 분리해
 * 뒀다(`shared/lib/markdown`의 `splitByRanges`) — 렌더링 없이 검증할 수 있어야 한다.
 */
function DecoratedText({
  text,
  offset,
  sourceLength = text.length,
}: {
  text: string
  offset: number
  sourceLength?: number
}) {
  const decorations = useContext(DecorationContext)
  const pieces = splitByRanges(text, offset, sourceLength, decorations)

  if (pieces.length === 1 && pieces[0].rangeIndex === null) return <Fragment>{text}</Fragment>

  return (
    <>
      {pieces.map((piece, index) => (
        <Fragment key={index}>
          {piece.rangeIndex === null
            ? piece.text
            : decorations[piece.rangeIndex].render(piece.text, `${index}`)}
        </Fragment>
      ))}
    </>
  )
}

const HEADING_CLASS: Record<number, string> = {
  1: 'mt-6 mb-3 font-display text-[18px] font-bold text-text first:mt-0',
  2: 'mt-6 mb-[10px] font-display text-[16px] font-bold text-text first:mt-0',
  3: 'mt-5 mb-2 font-display text-[14.5px] font-bold text-text first:mt-0',
  4: 'mt-4 mb-2 text-[13.5px] font-bold text-text first:mt-0',
  5: 'mt-4 mb-2 text-[13px] font-bold text-text-secondary first:mt-0',
  6: 'mt-4 mb-2 text-[12.5px] font-bold text-text-tertiary first:mt-0',
}

const ALIGN_CLASS: Record<TableAlign, string> = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
}

/** 목록 항목 안에서는 문단·중첩 목록의 위아래 여백을 줄인다 — 줄 간격이 벌어져 보인다. */
function Block({ block, dense = false }: { block: MarkdownBlock; dense?: boolean }) {
  switch (block.kind) {
    case 'heading': {
      const Tag = `h${block.level}` as 'h1'
      return (
        <Tag className={HEADING_CLASS[block.level]}>
          <Inline nodes={block.children} />
        </Tag>
      )
    }

    case 'paragraph':
      return (
        <p className={cx(dense ? 'my-0' : 'my-3 first:mt-0 last:mb-0')}>
          <Inline nodes={block.children} />
        </p>
      )

    case 'code':
      return (
        <pre
          className={cx(
            'overflow-x-auto rounded-sm border border-border-strong bg-surface-muted px-[14px] py-3',
            dense ? 'my-2' : 'my-3 first:mt-0 last:mb-0',
          )}
        >
          <code className="font-mono text-[12px] leading-[1.7] text-text">
            <DecoratedText text={block.text} offset={block.offset} />
          </code>
        </pre>
      )

    case 'quote':
      return (
        <blockquote
          className={cx(
            'border-l-[3px] border-accent-border bg-accent-bg-strong px-4 py-[10px]',
            dense ? 'my-2' : 'my-3 first:mt-0 last:mb-0',
          )}
        >
          {block.blocks.map((child, index) => (
            <Block key={index} block={child} />
          ))}
        </blockquote>
      )

    case 'list': {
      const Tag = block.ordered ? 'ol' : 'ul'
      return (
        <Tag
          // Tailwind preflight가 목록 기호를 지우므로 list-* 유틸리티로 되살린다.
          className={cx(
            'pl-[22px]',
            block.ordered ? 'list-decimal' : 'list-disc',
            dense ? 'my-1' : 'my-3 first:mt-0 last:mb-0',
          )}
          start={block.ordered && block.start !== 1 ? block.start : undefined}
        >
          {block.items.map((item, index) => (
            <li key={index} className="my-1 pl-1">
              {item.map((child, childIndex) => (
                <Block key={childIndex} block={child} dense />
              ))}
            </li>
          ))}
        </Tag>
      )
    }

    case 'table':
      return (
        // 좁은 패널에서도 표가 레이아웃을 밀지 않도록 표만 가로 스크롤한다.
        <div className={cx('overflow-x-auto', dense ? 'my-2' : 'my-4 first:mt-0 last:mb-0')}>
          <table className="w-full border-collapse text-[12.5px] leading-[1.6]">
            <thead>
              <tr>
                {block.header.map((cell, index) => (
                  <th
                    key={index}
                    className={cx(
                      'border border-border-strong bg-surface-muted px-3 py-2 font-semibold text-text',
                      ALIGN_CLASS[block.aligns[index]],
                    )}
                  >
                    <Inline nodes={cell} />
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {block.rows.map((row, rowIndex) => (
                <tr key={rowIndex}>
                  {row.map((cell, index) => (
                    <td
                      key={index}
                      className={cx(
                        'border border-border-strong px-3 py-2 align-top',
                        ALIGN_CLASS[block.aligns[index]],
                      )}
                    >
                      <Inline nodes={cell} />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )

    case 'rule':
      return <hr className="my-5 border-t border-border-strong" />
  }
}

function Inline({ nodes }: { nodes: InlineNode[] }) {
  return (
    <>
      {nodes.map((node, index) => (
        <InlineItem key={index} node={node} />
      ))}
    </>
  )
}

function InlineItem({ node }: { node: InlineNode }) {
  switch (node.kind) {
    case 'text':
      return <DecoratedText text={node.text} offset={node.offset} sourceLength={node.sourceLength} />

    case 'break':
      return <br />

    case 'strong':
      return (
        <strong className="font-bold text-text">
          <Inline nodes={node.children} />
        </strong>
      )

    case 'emphasis':
      return (
        <em className="italic">
          <Inline nodes={node.children} />
        </em>
      )

    case 'strike':
      return (
        <s className="text-text-faint line-through">
          <Inline nodes={node.children} />
        </s>
      )

    case 'code':
      return (
        <code className="rounded-xs border border-border-soft bg-neutral-bg px-[5px] py-[1px] font-mono text-[12px] text-text">
          <DecoratedText text={node.text} offset={node.offset} />
        </code>
      )

    case 'link':
      return (
        // 본문 링크는 외부로 나가는 경우가 많아 새 탭에서 연다. `noreferrer`로 opener를 끊는다.
        <a
          href={node.href}
          target="_blank"
          rel="noreferrer noopener"
          className="text-accent-strong underline underline-offset-2"
        >
          <Inline nodes={node.children} />
        </a>
      )

    case 'image':
      return (
        <img
          src={node.src}
          alt={node.alt}
          loading="lazy"
          className="my-2 max-w-full rounded-sm border border-border-soft"
        />
      )
  }
}
