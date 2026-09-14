import { useState } from 'react'
import { Avatar, Button, Card } from '../../../shared/ui'
import { useAssignReviewer, useRemoveReviewer, useReviewers } from '../hooks/useReviewers'

/**
 * 리뷰어 지정 패널.
 *
 * 워크스페이스 참여자 목록은 **prop으로 받는다** — features/review가 features/member를
 * 직접 참조하지 않기 위해서다(frontend/docs/ARCHITECTURE.md). 페이지가 조합한다.
 *
 * 지정은 알림 대상을 정하는 것이지 검토 자격을 정하는 게 아니다 — 정족수는 워크스페이스
 * 룰셋의 requiredReviewerCount로 판정하고, 지정되지 않은 참여자도 검토를 제출할 수 있다(G-4).
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
}

export function ReviewerPanel({ reviewRequestId, members, excludeMemberId }: Props) {
  const { data: reviewers, isLoading } = useReviewers(reviewRequestId)
  const assign = useAssignReviewer(reviewRequestId)
  const remove = useRemoveReviewer(reviewRequestId)
  const [picked, setPicked] = useState('')

  const nameByMemberId = new Map(members.map((member) => [member.memberId, member.name]))
  const assignedMemberIds = new Set((reviewers ?? []).map((reviewer) => reviewer.memberId))
  const selectable = members.filter(
    (member) => !assignedMemberIds.has(member.memberId) && member.memberId !== excludeMemberId,
  )

  return (
    <Card className="flex flex-col gap-3 p-[14px]">
      <p className="text-[12.5px] font-bold text-text">리뷰어</p>

      {isLoading && <p className="text-[11px] text-text-quaternary">불러오는 중…</p>}
      {!isLoading && (reviewers ?? []).length === 0 && (
        <p className="text-[11px] text-text-quaternary">
          지정된 리뷰어가 없습니다. 지정하지 않아도 참여자는 검토할 수 있습니다.
        </p>
      )}

      <div className="flex flex-col gap-2">
        {(reviewers ?? []).map((reviewer) => {
          const name = nameByMemberId.get(reviewer.memberId) ?? '—'
          return (
            <div key={reviewer.id} className="flex items-center gap-2">
              <Avatar initial={name.charAt(0)} size={22} />
              <span className="flex-1 text-[12.5px]">{name}</span>
              <button
                type="button"
                onClick={() => remove.mutate(reviewer.id)}
                disabled={remove.isPending}
                className="text-[11px] text-text-quaternary hover:text-danger"
              >
                해제
              </button>
            </div>
          )
        })}
      </div>

      <div className="flex items-center gap-2">
        <select
          value={picked}
          onChange={(event) => setPicked(event.target.value)}
          className="flex-1 rounded-sm border border-border-strong px-2 py-[6px] text-[12px] text-text"
        >
          <option value="">참여자 선택…</option>
          {selectable.map((member) => (
            <option key={member.memberId} value={member.memberId}>
              {member.name}
            </option>
          ))}
        </select>
        <Button
          size="sm"
          variant="outline"
          disabled={!picked || assign.isPending}
          onClick={() => assign.mutate(picked, { onSuccess: () => setPicked('') })}
        >
          지정
        </Button>
      </div>
    </Card>
  )
}
