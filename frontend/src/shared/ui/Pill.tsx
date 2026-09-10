import type { HTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .pill + .tone-* / .pill-outline / .pill-lg 이식.
export type PillTone = 'accent' | 'danger' | 'warn' | 'success' | 'neutral' | 'outline'

interface PillProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: PillTone
  size?: 'md' | 'lg'
}

const toneClass: Record<PillTone, string> = {
  accent: 'border-accent-border bg-accent-bg text-accent-strong',
  danger: 'border-danger-border bg-danger-bg text-danger',
  warn: 'border-warn-border bg-warn-bg text-warn',
  success: 'border-success-border bg-success-bg text-success',
  neutral: 'border-border-strong bg-neutral-bg text-text-tertiary',
  outline: 'border-border-strong bg-surface text-text-secondary',
}

export function Pill({ tone = 'neutral', size = 'md', className, ...rest }: PillProps) {
  return (
    <span
      className={cx(
        'inline-flex items-center gap-1 whitespace-nowrap border font-semibold',
        size === 'lg'
          ? 'rounded-pill px-[11px] py-1 text-[11.5px]'
          : 'rounded-xs px-[9px] py-[3px] text-[11px]',
        toneClass[tone],
        className,
      )}
      {...rest}
    />
  )
}
