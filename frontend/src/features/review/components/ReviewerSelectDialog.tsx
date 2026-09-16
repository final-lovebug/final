import { useState } from 'react'
import { Avatar, Button, Checkbox, Modal } from '../../../shared/ui'
import type { ReviewerCandidate } from './ReviewerPanel'

// 리뷰 요청을 보내기 전에 **누구에게 알릴지** 고르는 대화상자.
//
// 전에는 두 초안 화면(문서·사전집)이 버튼을 누르는 즉시 「본인 제외 참여자 전원」을
// 리뷰어로 지정해 보냈다. 백엔드는 생성 요청 본문의 `reviewerMemberIds`로 골라 받을 수
// 있는데(`docs/API.md` «리뷰 요청 생성») 화면에 고를 자리가 없었다.
//
// **기본값은 전원 선택이다** — 지금까지의 동작이고, 아무도 고르지 않은 채 보내면 리뷰
// 요청 알림이 아무에게도 가지 않는다(`CONFLICTS.md`: 지정 리뷰어가 없으면
// `REVIEW_REQUEST_RECEIVED`를 만들지 않는다).
//
// 지정은 **검토 자격이 아니라 알림 대상**을 정한다(`G-4`). 지정되지 않은 참여자도 검토를
// 제출할 수 있고 정족수는 룰셋 기준이라, 그 사실을 화면에 적어 둔다 —
// `ReviewerPanel`(개정안 화면)과 같은 문구다.

interface Props {
  open: boolean
  /** 워크스페이스 참여자 전원. 요청자 본인은 `excludeMemberId`로 뺀다. */
  candidates: ReviewerCandidate[]
  /** 리뷰 요청자는 자기 요청을 검토할 수 없으므로 후보에서 뺀다. */
  excludeMemberId?: string
  title: string
  confirmLabel: string
  isPending?: boolean
  error?: string | null
  onSubmit: (reviewerMemberIds: string[]) => void
  onClose: () => void
}

export function ReviewerSelectDialog({
  open,
  candidates,
  excludeMemberId,
  title,
  confirmLabel,
  isPending = false,
  error,
  onSubmit,
  onClose,
}: Props) {
  const selectable = candidates.filter((candidate) => candidate.memberId !== excludeMemberId)
  const signature = selectable.map((candidate) => candidate.memberId).join(',')

  const [selected, setSelected] = useState<string[]>(() =>
    selectable.map((candidate) => candidate.memberId),
  )
  // 참여자 목록이 늦게 도착하거나 바뀌면 선택을 다시 전원으로 맞춘다. effect가 아니라
  // 렌더 중 보정이다 — 목록이 오기 전에 연 대화상자가 빈 선택으로 남지 않게 한다.
  const [lastSignature, setLastSignature] = useState(signature)
  if (signature !== lastSignature) {
    setLastSignature(signature)
    setSelected(selectable.map((candidate) => candidate.memberId))
  }

  const allSelected = selectable.length > 0 && selected.length === selectable.length

  function toggle(memberId: string) {
    setSelected((current) =>
      current.includes(memberId)
        ? current.filter((id) => id !== memberId)
        : [...current, memberId],
    )
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button variant="outline" disabled={isPending} onClick={onClose}>
            취소
          </Button>
          <Button variant="primary" disabled={isPending} onClick={() => onSubmit(selected)}>
            {isPending ? '요청 중…' : confirmLabel}
          </Button>
        </>
      }
    >
      <p className="text-[12.5px] leading-[1.7] text-text-secondary">
        고른 참여자에게 리뷰 요청 알림이 갑니다. <b>검토 자격을 정하는 것은 아닙니다</b> —
        지정되지 않은 참여자도 검토를 제출할 수 있고, 승인 정족수는 워크스페이스 규칙을
        따릅니다.
      </p>

      {selectable.length === 0 ? (
        <p className="rounded-sm border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] text-text-tertiary">
          지정할 수 있는 참여자가 없습니다. 이대로 요청하면 알림 없이 리뷰 요청만 만들어집니다.
        </p>
      ) : (
        <div className="overflow-hidden rounded-sm border border-border-strong">
          <div className="flex items-center justify-between border-b border-border-soft bg-surface-muted px-3 py-[9px]">
            <span className="text-[11.5px] font-bold text-text-secondary">
              워크스페이스 참여자 · {selected.length}/{selectable.length}명 선택
            </span>
            <button
              type="button"
              onClick={() =>
                setSelected(
                  allSelected ? [] : selectable.map((candidate) => candidate.memberId),
                )
              }
              className="cursor-pointer text-[11.5px] font-semibold text-accent-strong"
            >
              {allSelected ? '전체 해제' : '전체 선택'}
            </button>
          </div>

          <div className="max-h-[240px] overflow-y-auto">
            {selectable.map((candidate) => {
              const checked = selected.includes(candidate.memberId)
              return (
                <button
                  key={candidate.memberId}
                  type="button"
                  aria-pressed={checked}
                  onClick={() => toggle(candidate.memberId)}
                  className="flex w-full cursor-pointer items-center gap-[10px] border-b border-border-faint bg-surface px-3 py-[9px] text-left text-[12.5px] last:border-b-0 hover:bg-surface-muted"
                >
                  <Checkbox checked={checked} />
                  <Avatar initial={candidate.name.charAt(0)} tone="accent" size={22} />
                  <span className="font-semibold">{candidate.name}</span>
                </button>
              )
            })}
          </div>
        </div>
      )}

      {selectable.length > 0 && selected.length === 0 && (
        <p className="text-[11.5px] text-warn">
          아무도 고르지 않으면 리뷰 요청 알림이 가지 않습니다. 요청 자체는 만들어지고, 리뷰어는
          개정안 화면에서 나중에 지정할 수 있습니다.
        </p>
      )}

      {error && <p className="text-xs text-danger">{error}</p>}
    </Modal>
  )
}
