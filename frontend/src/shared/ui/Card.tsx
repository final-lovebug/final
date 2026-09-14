import type { HTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .card 이식.
export function Card({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cx('rounded-md border border-border bg-surface', className)}
      {...rest}
    />
  )
}
