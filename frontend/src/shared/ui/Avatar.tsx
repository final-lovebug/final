import type { HTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .avatar + .avatar-* 이식. 원본은 고정 크기였지만 sidebar-footer(작게)와
// avatar-stack(26px) 등 크기가 달라 size prop으로 받는다.
export type AvatarTone = 'accent' | 'warn' | 'success' | 'danger' | 'neutral'

interface AvatarProps extends HTMLAttributes<HTMLDivElement> {
  initial: string
  tone?: AvatarTone
  size?: number
}

const toneClass: Record<AvatarTone, string> = {
  accent: 'bg-accent-bg text-accent-strong',
  warn: 'bg-[#FEF1E3] text-[#92590B]',
  success: 'bg-[#E8F6EE] text-[#1F7A4D]',
  danger: 'bg-[#FDECEC] text-[#B3261E]',
  neutral: 'bg-border-soft text-text-tertiary',
}

export function Avatar({
  initial,
  tone = 'neutral',
  size = 32,
  className,
  style,
  ...rest
}: AvatarProps) {
  return (
    <div
      className={cx(
        'flex shrink-0 items-center justify-center rounded-full font-display font-bold',
        toneClass[tone],
        className,
      )}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.4), ...style }}
      {...rest}
    >
      {initial}
    </div>
  )
}
