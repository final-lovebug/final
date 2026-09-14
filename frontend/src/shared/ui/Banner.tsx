import type { HTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .banner / .banner-accent / .banner-neutral 이식.
export type BannerTone = 'accent' | 'neutral'

interface BannerProps extends HTMLAttributes<HTMLDivElement> {
  tone?: BannerTone
}

const toneClass: Record<BannerTone, string> = {
  accent: 'border border-accent-border bg-accent-bg-strong text-text',
  neutral: 'bg-neutral-bg text-text-secondary',
}

export function Banner({ tone = 'accent', className, ...rest }: BannerProps) {
  return (
    <div
      className={cx(
        'rounded-sm px-4 py-3 text-[12.5px] leading-[1.6]',
        toneClass[tone],
        className,
      )}
      {...rest}
    />
  )
}
