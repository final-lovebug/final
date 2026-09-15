import type { HTMLAttributes, ReactNode } from 'react'
import { Avatar, type AvatarTone } from './Avatar'
import { cx } from '../lib/cx'

// ui/style.css .pr-thread / .comment-card / .comment-* / .comment-compose 이식.

export function PrThread({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cx('flex w-[340px] shrink-0 flex-col gap-3', className)} {...rest} />
  )
}

interface CommentCardProps {
  name: string
  initial: string
  tone?: AvatarTone
  time: string
  text: string
  /** 내가 쓴 코멘트는 테두리가 accent로 강조된다. */
  mine?: boolean
  /** 삭제·전환 같은 하단 액션. */
  action?: ReactNode
}

export function CommentCard({
  name,
  initial,
  tone = 'neutral',
  time,
  text,
  mine,
  action,
}: CommentCardProps) {
  return (
    <div
      className={cx(
        'rounded-[10px] bg-surface p-[14px]',
        mine ? 'border-[1.5px] border-accent' : 'border border-border',
      )}
    >
      <div className="mb-2 flex items-center gap-2">
        <Avatar initial={initial} tone={tone} size={22} />
        <span className="text-[12.5px] font-bold">{name}</span>
        <span className="text-[10.5px] text-text-quaternary">{time}</span>
      </div>
      <p className="whitespace-pre-wrap wrap-break-word text-[12.5px] leading-[1.6] text-text-secondary">
        {text}
      </p>
      {action && <div className="mt-2 text-[11px] font-semibold text-accent-strong">{action}</div>}
    </div>
  )
}
