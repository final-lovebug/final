import { useEffect, useRef, useState } from 'react'
import { Avatar, Pill } from '../../../shared/ui'
import { cx } from '../../../shared/lib/cx'
import { useAssignReviewer, useRemoveReviewer, useReviewers } from '../hooks/useReviewers'
import { useReviews } from '../hooks/useSubmitReview'
import type { ReviewVerdict } from '../model/types'
import { latestReviewByMember } from '../model/reviewTimeline'

/**
 * 리뷰어 패널 — GitHub PR 우측 사이드바의 Reviewers 블록을 모티브로 했다.
 *
 * 워크스페이스 참여자 목록은 **prop으로 받는다** — features/review가 features/member를
 * 직접 참조하지 않기 위해서다(frontend/docs/ARCHITECTURE.md). 페이지가 조합한다.
 *
 * **지정은 알림 대상을 정하는 것이지 검토 자격을 정하는 게 아니다**(`G-4`) — 정족수는
 * 워크스페이스 룰셋의 `requiredReviewerCount`로 판정하고, 지정되지 않은 참여자도 검토를
 * 제출할 수 있다. GitHub과 달리 리뷰어 전원 승인이 발행 조건이 아니라는 뜻이라, 그 차이를
 * 패널 발치에 적어 둔다.
 *
 * **상태는 조회 두 번을 조인해서 만든다.** `ReviewerResponse`에는 판정 결과가 없어
 * (`reviewerId·memberId·assignedAt·createdBy`뿐) 제출된 검토 목록과 맞춰야 한다. 회차와
 * 무관하게 회원별 최신 리뷰 1건을 상태로 인정한다(`D-1`).
 */
export interface ReviewerCandidate {
  memberId: string
  name: string
}

interface Props {
  reviewRequestId: string
  members: ReviewerCandidate[]
  /** 리뷰 요청자는 자기 요청을 검토할 수 없으므로 후보에서 뺀다. */
  excludeMemberId?: string
  /** 리뷰어를 넣고 뺄 수 있는지(요청자이며 ADMIN 이상). false면 ⚙ 와 × 를 감춘다. */
  canEdit?: boolean
}

const VERDICT_LABEL: Record<ReviewVerdict, string> = {
  APPROVED: '✓ 승인',
  CHANGES_REQUESTED: '↻ 변경 요청',
}

export function ReviewerPanel({
  reviewRequestId,
  members,
  excludeMemberId,
  canEdit = false,
}: Props) {
  const { data: reviewers, isLoading } = useReviewers(reviewRequestId)
  const { data: reviews } = useReviews(reviewRequestId)
  const assign = useAssignReviewer(reviewRequestId)
  const remove = useRemoveReviewer(reviewRequestId)

  const [pickerOpen, setPickerOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!pickerOpen) return
    function handleClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setPickerOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [pickerOpen])

  const nameByMemberId = new Map(members.map((member) => [member.memberId, member.name]))
  const verdictByMemberId = new Map(
    [...latestReviewByMember(reviews ?? [])].map(([memberId, review]) => [memberId, review.verdict]),
  )
  const assignedMemberIds = new Set((reviewers ?? []).map((reviewer) => reviewer.memberId))
  const selectable = members.filter((member) => member.memberId !== excludeMemberId)

  return (
    <div ref={rootRef} className="relative rounded-md border border-border bg-surface">
      <div className="flex items-center justify-between border-b border-border-soft px-[14px] py-[11px]">
        <span className="text-xs font-bold text-text-secondary">
          리뷰어 {(reviewers ?? []).length}
        </span>
        {canEdit && (
          <button
            type="button"
            aria-label="리뷰어 추가·제외"
            onClick={() => setPickerOpen((open) => !open)}
            className="flex h-6 w-6 cursor-pointer items-center justify-center rounded-xs text-[13px] text-text-quaternary hover:bg-bg hover:text-text-secondary"
          >
            ⚙
          </button>
        )}
      </div>

      {isLoading && <p className="px-[14px] py-3 text-[11px] text-text-quaternary">불러오는 중…</p>}
      {!isLoading && (reviewers ?? []).length === 0 && (
        <p className="px-[14px] py-3 text-[11px] text-text-quaternary">
          지정된 리뷰어가 없습니다. 지정하지 않아도 참여자는 검토할 수 있습니다.
        </p>
      )}

      {(reviewers ?? []).map((reviewer) => {
        const name = nameByMemberId.get(reviewer.memberId) ?? '—'
        const verdict = verdictByMemberId.get(reviewer.memberId)
        return (
          <div
            key={reviewer.id}
            className="flex items-center gap-2 border-b border-border-faint px-[14px] py-[9px] text-[12.5px] last:border-b-0"
          >
            <Avatar initial={name.charAt(0)} tone="accent" size={22} />
            <span className="font-semibold">{name}</span>
            <span
              className={cx(
                'ml-auto text-[11px] font-bold',
                verdict === 'APPROVED' && 'text-success',
                verdict === 'CHANGES_REQUESTED' && 'text-warn',
                verdict === undefined && 'font-semibold text-text-quaternary',
              )}
            >
              {verdict === undefined ? '대기' : VERDICT_LABEL[verdict]}
            </span>
            {canEdit && (
              <button
                type="button"
                aria-label={`${name} 리뷰어에서 제외`}
                disabled={remove.isPending}
                onClick={() => remove.mutate(reviewer.id)}
                className="cursor-pointer font-bold text-text-disabled hover:text-danger"
              >
                ×
              </button>
            )}
          </div>
        )
      })}

      <p className="border-t border-border-soft px-[12px] py-[9px] text-[10.5px] leading-[1.6] text-text-quaternary">
        지정은 <b>알림 대상</b>을 정하는 것이고 검토 자격을 정하는 것이 아닙니다 — 지정되지
        않은 참여자도 검토를 제출할 수 있습니다.
      </p>

      {pickerOpen && (
        <div className="absolute right-[10px] top-10 z-40 w-[260px] overflow-hidden rounded-md border border-border bg-surface shadow-pop">
          <div className="border-b border-border-soft px-3 py-[10px] text-[11.5px] font-bold text-text-secondary">
            워크스페이스 참여자
          </div>
          {selectable.length === 0 && (
            <p className="px-3 py-[10px] text-[11.5px] text-text-quaternary">
              지정할 수 있는 참여자가 없습니다.
            </p>
          )}
          {selectable.map((member) => {
            const assigned = assignedMemberIds.has(member.memberId)
            const reviewerId = (reviewers ?? []).find(
              (reviewer) => reviewer.memberId === member.memberId,
            )?.id
            return (
              <button
                key={member.memberId}
                type="button"
                disabled={assign.isPending || remove.isPending}
                onClick={() =>
                  assigned && reviewerId
                    ? remove.mutate(reviewerId)
                    : assign.mutate(member.memberId)
                }
                className="flex w-full cursor-pointer items-center gap-2 border-b border-border-faint px-3 py-[9px] text-left text-[12.5px] last:border-b-0 hover:bg-surface-muted"
              >
                <Avatar initial={member.name.charAt(0)} tone="accent" size={20} />
                {member.name}
                {assigned && <span className="ml-auto font-bold text-accent">✓</span>}
              </button>
            )
          })}
          {excludeMemberId && (
            <div className="flex items-center gap-2 border-t border-border-soft px-3 py-[9px] text-[12.5px] text-text-disabled">
              <Avatar
                initial={(nameByMemberId.get(excludeMemberId) ?? '—').charAt(0)}
                tone="neutral"
                size={20}
              />
              {nameByMemberId.get(excludeMemberId) ?? '—'}
              <Pill tone="neutral" className="ml-auto">
                요청자
              </Pill>
            </div>
          )}
          <p className="px-3 py-[9px] text-[10.5px] leading-[1.6] text-text-quaternary">
            요청자는 자기 요청을 검토할 수 없어 후보에서 빠집니다.
          </p>
        </div>
      )}
    </div>
  )
}
