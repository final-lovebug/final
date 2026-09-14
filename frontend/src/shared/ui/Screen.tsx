import type { HTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .screen-title / .screen-subtitle / .two-col / .col-flex / .detail-panel
// / .quote-block 이식. 화면 골격을 이루는 조각들이라 한 파일에 모았다.

export function ScreenTitle({ className, ...rest }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h1
      className={cx('mb-[6px] font-display text-[19px] font-bold text-text', className)}
      {...rest}
    />
  )
}

export function ScreenSubtitle({ className, ...rest }: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p
      className={cx('mb-5 text-[12.5px] leading-[1.7] text-text-tertiary', className)}
      {...rest}
    />
  )
}

/** ui의 `.two-col` — 본문(넓게) + 사이드 패널(고정폭) 2단. */
export function TwoCol({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx('flex items-start gap-5', className)} {...rest} />
}

/** `.two-col > .col-flex` — 남는 폭을 전부 먹되 표가 넘치지 않게 min-w-0. */
export function ColFlex({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx('min-w-0 flex-1', className)} {...rest} />
}

/** ui의 `.detail-panel` — master-detail의 오른쪽 패널. */
export function DetailPanel({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cx(
        'flex shrink-0 flex-col gap-4 rounded-md border border-border bg-surface p-5',
        className,
      )}
      {...rest}
    />
  )
}

interface QuoteBlockProps extends HTMLAttributes<HTMLDivElement> {
  text: string
  source: string
  /** ui의 `.quote-block.split` — 뜻이 갈리는 근거 문장은 왼쪽 선이 accent로 바뀐다. */
  split?: boolean
}

export function QuoteBlock({ text, source, split, className, ...rest }: QuoteBlockProps) {
  return (
    <div
      className={cx(
        'rounded-r-xs border-l-2 bg-surface-muted px-3 py-[9px]',
        split ? 'border-accent' : 'border-border-strong',
        className,
      )}
      {...rest}
    >
      <div className="text-xs italic">“{text}”</div>
      <div className="mt-1 text-[10.5px] text-text-quaternary">{source}</div>
    </div>
  )
}
