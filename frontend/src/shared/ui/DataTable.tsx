import type { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css table.dtable 이식. 목록 화면 6개가 표 마크업을 각자 손으로 쓰고 있어
// 여백·구분선·헤더 색이 조금씩 달랐다 — 한 벌로 모은다.
//
// 마지막 행의 아래 테두리를 지우는 규칙(`tr:last-child td { border-bottom: none }`)은
// Tailwind 유틸리티로 자식을 겨냥해 그대로 옮겼다.

export function DataTable({ className, ...rest }: HTMLAttributes<HTMLTableElement>) {
  return (
    <table
      className={cx(
        'w-full border-collapse text-[12.5px] [&_tr:last-child>td]:border-b-0',
        className,
      )}
      {...rest}
    />
  )
}

export function Th({ className, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cx(
        'whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary',
        className,
      )}
      {...rest}
    />
  )
}

export function Td({ className, ...rest }: TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td
      className={cx('border-b border-border-faint px-4 py-3 text-text-secondary', className)}
      {...rest}
    />
  )
}

interface TrProps extends HTMLAttributes<HTMLTableRowElement> {
  /** ui의 `tr.clickable` — 커서와 hover 배경이 붙는다. */
  clickable?: boolean
}

export function Tr({ clickable, className, ...rest }: TrProps) {
  return (
    <tr
      className={cx(clickable && 'cursor-pointer hover:bg-surface-muted', className)}
      {...rest}
    />
  )
}
