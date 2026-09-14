import type { HTMLAttributes } from 'react'
import { cx } from '../lib/cx'

// ui/style.css .timeline-item 이식. 문서 버전 이력·사전집 리비전 이력이 함께 쓴다.
export type TimelineTone = 'default' | 'current' | 'danger'

interface TimelineItemProps extends HTMLAttributes<HTMLDivElement> {
  tone?: TimelineTone
  /** 아직 오지 않은/지나간 항목을 흐리게. */
  dim?: boolean
}

const toneClass: Record<TimelineTone, string> = {
  default: 'border border-border',
  current: 'border-2 border-accent',
  danger: 'border-2 border-danger',
}

export function TimelineItem({
  tone = 'default',
  dim,
  className,
  ...rest
}: TimelineItemProps) {
  return (
    <div
      className={cx(
        'rounded-[10px] bg-surface px-[15px] py-3',
        toneClass[tone],
        dim && 'opacity-70',
        className,
      )}
      {...rest}
    />
  )
}
