import type { HTMLAttributes, ReactNode } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .toolbar / .filter-chip / .segmented / .spacer 이식.
// 목록 화면 상단의 필터 줄을 이루는 조각들이다.

export function Toolbar({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cx('mb-4 flex flex-wrap items-center gap-[10px]', className)} {...rest} />
  )
}

/** 툴바 좌우를 가르는 여백. ui의 `.toolbar .spacer`. */
export function ToolbarSpacer() {
  return <div className="flex-1" />
}

interface FilterChipProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
}

export function FilterChip({ className, ...rest }: FilterChipProps) {
  return (
    <div
      className={cx(
        'whitespace-nowrap rounded-sm border border-border-strong bg-surface px-3 py-[7px] text-[12.5px] text-text-secondary',
        className,
      )}
      {...rest}
    />
  )
}

export interface SegmentedOption<T extends string> {
  value: T
  label: string
}

interface SegmentedProps<T extends string> {
  options: SegmentedOption<T>[]
  value: T
  onChange: (value: T) => void
}

export function Segmented<T extends string>({ options, value, onChange }: SegmentedProps<T>) {
  return (
    <div className="flex overflow-hidden rounded-sm border border-border-strong bg-surface">
      {options.map((option, index) => (
        <button
          key={option.value}
          type="button"
          onClick={() => onChange(option.value)}
          className={cx(
            'cursor-pointer px-[13px] py-[7px] text-[12.5px] text-text-quaternary',
            index > 0 && 'border-l border-border-strong',
            option.value === value && 'bg-border-soft font-semibold text-text',
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
