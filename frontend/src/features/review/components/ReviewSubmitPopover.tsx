import { useEffect, useRef, useState } from 'react'
import { Button, FieldLabel, RadioDot, TextArea } from '../../../shared/ui'
import { cx } from '../../../shared/lib/cx'
import type { ReviewVerdict } from '../model/types'

/**
 * 검토 제출 — GitHub PR 의 "Finish your review" 를 모티브로 했다.
 *
 * 예전에는 `Change request`·`Approve` 버튼 둘을 바로 눌렀는데, 그러면 무엇을 고르는지와
 * 무엇을 남기는지가 갈려 있었다. 라디오로 판정을 고르고 한 번에 제출하는 쪽으로 묶었다.
 *
 * **판정은 둘뿐이다.** GitHub 의 「Comment」에 해당하는 값이 백엔드에 없다 —
 * `ReviewVerdict` 가 `APPROVED`·`CHANGES_REQUESTED` 뿐이고 "반대(REJECT)"도 없다.
 * 의견만 남기려면 개정안 표에서 용어별 코멘트를 단다(그 코멘트는 제출과 함께 나간다, `D-63`).
 */
interface Props {
  /** 제출 직전까지 쌓아 둔 용어별 코멘트 수. 버튼에 개수를 보여준다. */
  pendingCommentCount: number
  isSubmitting: boolean
  disabled?: boolean
  disabledReason?: string
  onSubmit: (verdict: ReviewVerdict, summaryComment: string) => void
}

const OPTIONS: { verdict: ReviewVerdict; title: string; desc: string }[] = [
  {
    verdict: 'APPROVED',
    title: '승인',
    desc: '이대로 발행해도 좋다는 뜻입니다. 정족수에 포함됩니다.',
  },
  {
    verdict: 'CHANGES_REQUESTED',
    title: '변경 요청',
    desc: '요청자가 초안을 고쳐 재교정을 돌려야 합니다. 기존 승인은 무효가 됩니다.',
  },
]

export function ReviewSubmitPopover({
  pendingCommentCount,
  isSubmitting,
  disabled,
  disabledReason,
  onSubmit,
}: Props) {
  const [open, setOpen] = useState(false)
  const [verdict, setVerdict] = useState<ReviewVerdict>('APPROVED')
  const [summary, setSummary] = useState('')
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    function handleClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false)
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [open])

  return (
    <div ref={rootRef} className="relative">
      <Button
        variant="primary"
        disabled={disabled || isSubmitting}
        title={disabled ? disabledReason : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        리뷰 마무리
        {pendingCommentCount > 0 ? ` (코멘트 ${pendingCommentCount})` : ''} ▾
      </Button>

      {open && (
        <div className="absolute right-0 top-[calc(100%+8px)] z-40 w-[360px] rounded-md border border-border bg-surface shadow-pop">
          <div className="border-b border-border-soft px-[14px] py-3 font-display text-[13.5px] font-bold">
            리뷰 마무리
          </div>

          <div className="flex flex-col gap-3 p-[14px]">
            <div>
              <FieldLabel>
                전체 코멘트 <span className="font-medium text-text-quaternary">(선택)</span>
              </FieldLabel>
              <TextArea
                value={summary}
                onChange={(event) => setSummary(event.target.value)}
                rows={3}
                placeholder="남길 말이 있으면 적습니다. 용어별 코멘트는 표에서 따로 답니다."
                className="min-h-16 bg-surface-muted text-[12.5px]"
              />
            </div>

            <div>
              <FieldLabel required>판정</FieldLabel>
              <div className="flex flex-col gap-1">
                {OPTIONS.map((option) => (
                  <button
                    key={option.verdict}
                    type="button"
                    onClick={() => setVerdict(option.verdict)}
                    className={cx(
                      'flex cursor-pointer gap-[9px] rounded-sm border border-transparent px-[10px] py-[9px] text-left hover:bg-bg',
                      verdict === option.verdict && 'border-accent-border bg-accent-bg-strong',
                    )}
                  >
                    <span className="mt-[2px]">
                      <RadioDot checked={verdict === option.verdict} />
                    </span>
                    <span>
                      <span className="block text-[12.5px] font-bold">{option.title}</span>
                      <span className="mt-[2px] block text-[11px] leading-[1.55] text-text-tertiary">
                        {option.desc}
                      </span>
                    </span>
                  </button>
                ))}
              </div>
              <p className="mt-[7px] text-[10.5px] leading-[1.6] text-text-quaternary">
                GitHub 의 「Comment」에 해당하는 판정은 없습니다 — 백엔드 판정이 승인·변경 요청
                둘뿐입니다. 의견만 남기려면 표에서 용어별 코멘트를 답니다.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-[10px] border-t border-border-soft px-[14px] py-3">
            <span className="text-[10.5px] leading-[1.5] text-text-quaternary">
              본인이 올린 요청은 본인이 승인할 수 없습니다
            </span>
            <Button
              variant="primary"
              size="sm"
              className="ml-auto"
              disabled={isSubmitting}
              onClick={() => {
                onSubmit(verdict, summary.trim())
                setOpen(false)
                setSummary('')
              }}
            >
              {isSubmitting ? '제출 중…' : '리뷰 제출'}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
