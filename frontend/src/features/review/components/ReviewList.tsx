import { Avatar, Card, Pill } from '../../../shared/ui'
import { toRelativeTime } from '../../../shared/lib/relativeTime'
import { groupReviewsByMember, type ReviewEntry } from '../model/reviewTimeline'
import type { Comment, Review, Reviewer } from '../model/types'

interface Props {
  reviews: Review[]
  comments: Comment[]
  reviewers: Reviewer[]
  members: { memberId: string; name: string }[]
  termNameById: Map<string, string>
  onSelectTerm: (termId: string) => void
}

function verdictLabel(verdict: Review['verdict']) {
  return verdict === 'APPROVED' ? '✓ 승인' : '↻ 변경 요청'
}

function verdictTone(verdict: Review['verdict']) {
  return verdict === 'APPROVED' ? ('success' as const) : ('warn' as const)
}

function EntryBody({ entry, onSelectTerm, termNameById }: { entry: ReviewEntry; onSelectTerm: (id: string) => void; termNameById: Map<string, string> }) {
  return (
    <div className="px-[14px] pb-[13px] pt-1">
      {entry.summary ? (
        <p className="whitespace-pre-wrap text-[13px] leading-[1.65] text-text-secondary">
          {entry.summary.content}
        </p>
      ) : (
        <p className="text-[12px] italic text-text-quaternary">
          코멘트 없이 {entry.review.verdict === 'APPROVED' ? '승인' : '변경 요청'}했습니다.
        </p>
      )}
      {entry.itemComments.length > 0 && (
        <div className="mt-3 border-t border-border-soft pt-2">
          <p className="mb-1 text-[10.5px] font-bold text-text-quaternary">
            용어 코멘트 {entry.itemComments.filter((comment) => comment.targetItemId != null).length}
          </p>
          {entry.itemComments.map((comment) => (
            <div key={comment.id} className="flex gap-1 text-[11.5px] leading-[1.6] text-text-secondary">
              {comment.targetItemId != null ? (
                <button
                  type="button"
                  className="shrink-0 cursor-pointer font-bold text-accent-strong hover:underline"
                  onClick={() => onSelectTerm(comment.targetItemId as string)}
                >
                  ↳ {termNameById.get(comment.targetItemId as string) ?? '용어'}
                </button>
              ) : (
                <span className="shrink-0 text-text-quaternary">↳</span>
              )}
              <span>{comment.content}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function ReviewEntryCard({ entry, onSelectTerm, termNameById, old = false }: { entry: ReviewEntry; onSelectTerm: (id: string) => void; termNameById: Map<string, string>; old?: boolean }) {
  return (
    <div className={old ? 'border-t border-dashed border-border-strong px-3 py-3' : ''}>
      {old && (
        <div className="mb-2 flex items-center gap-2">
          <Pill tone={verdictTone(entry.review.verdict)}>{verdictLabel(entry.review.verdict)}</Pill>
          <Pill tone="neutral">대체됨</Pill>
          <span className="text-[10.5px] text-text-quaternary">{toRelativeTime(entry.review.submittedAt)}</span>
        </div>
      )}
      <EntryBody entry={entry} onSelectTerm={onSelectTerm} termNameById={termNameById} />
    </div>
  )
}

export function ReviewList({ reviews, comments, reviewers, members, termNameById, onSelectTerm }: Props) {
  const groups = groupReviewsByMember(reviews, comments)
  const nameById = new Map(members.map((member) => [member.memberId, member.name]))
  const reviewed = new Set(groups.map((group) => group.memberId))
  const pending = reviewers.filter((reviewer) => !reviewed.has(reviewer.memberId))
  const groupById = new Map(groups.map((group) => [group.memberId, group]))
  const allGroups = [
    ...groups,
    ...pending.map((reviewer) => ({ memberId: reviewer.memberId, entries: [] as ReviewEntry[] })),
  ]

  return (
    <Card className="overflow-hidden">
      <div className="flex items-center gap-2 border-b border-border-soft px-[18px] py-[13px]">
        <h2 className="font-display text-sm font-bold">리뷰</h2>
        <Pill tone="neutral">{reviews.length}건 · {allGroups.length}명</Pill>
        <span className="ml-auto text-[10.5px] text-text-quaternary">최신 리뷰만 정족수에 셉니다</span>
      </div>
      {allGroups.length === 0 && (
        <p className="px-[18px] py-4 text-[11px] text-text-quaternary">아직 제출된 리뷰가 없습니다.</p>
      )}
      {allGroups.map((group) => {
        const latest = groupById.get(group.memberId)?.entries[0]
        const name = nameById.get(group.memberId) ?? '—'
        const previous = latest ? group.entries.slice(1) : []
        return (
          <div key={group.memberId} className="border-b border-border-faint last:border-b-0">
            <div className="flex items-center gap-2 px-[14px] py-[10px]">
              <Avatar initial={name.charAt(0)} tone={latest ? verdictTone(latest.review.verdict) : 'neutral'} size={28} />
              <span className="text-[12.5px] font-bold">{name}</span>
              <Pill tone={latest ? verdictTone(latest.review.verdict) : 'neutral'}>
                {latest ? verdictLabel(latest.review.verdict) : '대기'}
              </Pill>
              <span className="ml-auto text-[10.5px] text-text-quaternary">
                {latest ? toRelativeTime(latest.review.submittedAt) : '아직 리뷰를 제출하지 않았습니다.'}
              </span>
            </div>
            {latest && (
              <>
                <ReviewEntryCard entry={latest} onSelectTerm={onSelectTerm} termNameById={termNameById} />
                {previous.length > 0 && (
                  <details className="mx-[14px] mb-3">
                    <summary className="inline-flex cursor-pointer list-none rounded-pill border border-border-strong px-[10px] py-1 text-[10.5px] font-semibold text-text-tertiary">
                      ▶ 대체된 이전 리뷰 {previous.length}건
                    </summary>
                    <div className="mt-2 rounded-sm border border-dashed border-border-strong">
                      {previous.map((entry) => (
                        <ReviewEntryCard key={entry.review.id} entry={entry} onSelectTerm={onSelectTerm} termNameById={termNameById} old />
                      ))}
                    </div>
                  </details>
                )}
              </>
            )}
          </div>
        )
      })}
    </Card>
  )
}
