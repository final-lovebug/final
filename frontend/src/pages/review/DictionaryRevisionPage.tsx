import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Avatar, Button, Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { cx } from '../../shared/lib/cx'
import { toRelativeTime } from '../../shared/lib/relativeTime'
import { useDictionaryRevision } from '../../features/review/hooks/useDictionaryRevision'
import { useSubmitReview, useReviewProgress } from '../../features/review/hooks/useSubmitReview'
import {
  usePerformReexamine,
  usePerformRevise,
} from '../../features/review/hooks/useReviewLifecycle'
import { ReviewerPanel } from '../../features/review/components/ReviewerPanel'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useAuthStore } from '../../shared/stores/authStore'
import type { DraftComment } from '../../features/review/api/submitReview'
import type { AvatarTone } from '../../shared/ui'

const AVATAR_TONES: AvatarTone[] = ['accent', 'warn', 'success']

/** 작성자마다 일관된 색을 준다 — 백엔드에 tone 개념이 없어 id로 정한다(T-INT-12 결정 6). */
function toneFor(memberId: string): AvatarTone {
  let hash = 0
  for (const char of memberId) hash = (hash + char.charCodeAt(0)) % AVATAR_TONES.length
  return AVATAR_TONES[hash]
}

/**
 * 사전집 개정안 검토 화면.
 *
 * **코멘트는 제출 전까지 서버에 가지 않는다**(`D-63`) — 후보어별로 로컬 draft에 쌓아 두고,
 * Approve / Change request를 고르는 순간 verdict와 함께 한 번에 보낸다. GitHub의 "pending
 * review"를 서버가 아니라 프론트가 들고 있는 셈이다.
 */
export function DictionaryRevisionPage() {
  const { workspaceId = '', revisionId = '' } = useParams<{
    workspaceId: string
    revisionId: string
  }>()
  const navigate = useNavigate()
  const { data, isLoading, error } = useDictionaryRevision(workspaceId, revisionId)
  const reviewRequestId = data?.reviewRequestId ?? ''
  const { data: progress } = useReviewProgress(reviewRequestId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const submit = useSubmitReview(reviewRequestId)
  const reexamine = usePerformReexamine(reviewRequestId)
  const revise = usePerformRevise(reviewRequestId)
  const currentMember = useAuthStore((state) => state.currentMember)

  const [selectedTermId, setSelectedTermId] = useState<string | null>(null)
  const [commentDraft, setCommentDraft] = useState('')
  const [pending, setPending] = useState<DraftComment[]>([])

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (error || !data) {
    return (
      <p className="text-sm text-text-tertiary">
        {error instanceof Error ? error.message : '개정안을 불러오지 못했습니다.'}
      </p>
    )
  }

  const revision = data
  const selected =
    revision.rows.find((row) => row.candidateTermId === selectedTermId) ?? revision.rows[0]
  const nameByMemberId = new Map((members ?? []).map((member) => [member.id, member.name]))
  const threadComments = revision.comments.filter(
    (comment) => comment.targetItemId === selected?.candidateTermId,
  )
  const pendingForSelected = pending.filter(
    (comment) => comment.targetItemId === selected?.candidateTermId,
  )

  function stashComment() {
    if (!commentDraft.trim() || !selected) return
    setPending((previous) => [
      ...previous,
      { content: commentDraft.trim(), targetItemId: selected.candidateTermId },
    ])
    setCommentDraft('')
  }

  function submitVerdict(verdict: 'APPROVED' | 'CHANGES_REQUESTED') {
    submit.mutate(
      { reviewRequestId, targetRound: revision.reexamineRound, verdict, comments: pending },
      { onSuccess: () => setPending([]) },
    )
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h1 className="font-display text-lg font-bold text-text">
            사전집 개정안 — r{revision.baseVersionNo} → r{revision.baseVersionNo + 1}
          </h1>
          <Pill tone="neutral">{revision.status}</Pill>
          {revision.reexamineRound > 0 && (
            <span className="text-[11px] text-text-quaternary">
              재교정 {revision.reexamineRound}회차
            </span>
          )}
        </div>
        <div className="flex gap-[10px]">
          {revision.status === 'CHANGES_REQUESTED' && (
            <Button
              variant="outline"
              disabled={reexamine.isPending}
              onClick={() =>
                reexamine.mutate({
                  addressedCommentIds: revision.comments.map((comment) => comment.id),
                })
              }
            >
              재교정 완료
            </Button>
          )}
          <Button
            variant="outline"
            disabled={submit.isPending}
            onClick={() => submitVerdict('CHANGES_REQUESTED')}
          >
            Change request
            {pending.length > 0 ? ' (' + pending.length + ')' : ''}
          </Button>
          <Button
            variant="outline"
            disabled={submit.isPending}
            onClick={() => submitVerdict('APPROVED')}
          >
            Approve
          </Button>
          <Button
            variant="primary"
            disabled={!progress?.reviseEligible || revise.isPending}
            title={progress?.reviseEligible ? undefined : '정족수를 채워야 발행할 수 있습니다'}
            onClick={() =>
              revise.mutate(undefined, {
                onSuccess: () => navigate(routes.dictionary(workspaceId)),
              })
            }
          >
            반영 · r{revision.baseVersionNo + 1} 발행
          </Button>
        </div>
      </div>

      {progress && (
        <p className="mb-4 text-[11px] text-text-quaternary">
          승인 {progress.approvedCount} · 변경요청 {progress.changesRequestedCount} · 필요 정족수{' '}
          {progress.requiredReviewerCount}
        </p>
      )}

      <div className="flex items-start gap-5">
        <Card className="flex-1 overflow-hidden">
          <table className="w-full border-collapse text-[12.5px]">
            <thead>
              <tr>
                {['용어', '변경', '코멘트'].map((h) => (
                  <th
                    key={h}
                    className="whitespace-nowrap border-b border-border-soft px-4 py-[11px] text-left text-[11px] font-semibold text-text-quaternary"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {revision.rows.length === 0 && (
                <tr>
                  <td
                    colSpan={3}
                    className="px-4 py-6 text-center text-[11px] text-text-quaternary"
                  >
                    이번 개정안에 달라지는 용어가 없습니다.
                  </td>
                </tr>
              )}
              {revision.rows.map((row) => {
                const pendingCount = pending.filter(
                  (comment) => comment.targetItemId === row.candidateTermId,
                ).length
                const total = row.comments + pendingCount
                return (
                  <tr
                    key={row.candidateTermId}
                    onClick={() => setSelectedTermId(row.candidateTermId)}
                    className={cx(
                      'cursor-pointer',
                      row.candidateTermId === selected?.candidateTermId
                        ? 'bg-accent-bg-strong shadow-[inset_3px_0_0_var(--color-accent)]'
                        : 'hover:bg-surface-muted',
                    )}
                  >
                    <td className="border-b border-border-faint px-4 py-3 font-bold text-text">
                      {row.term}
                    </td>
                    <td className="border-b border-border-faint px-4 py-3">
                      <Pill tone={row.changeTone}>{row.change}</Pill>
                    </td>
                    <td
                      className={cx(
                        'border-b border-border-faint px-4 py-3',
                        total ? 'font-semibold text-warn' : 'text-text-quaternary',
                      )}
                    >
                      {total === 0
                        ? '—'
                        : '💬 ' + total + '개' + (pendingCount ? ' (미제출 포함)' : '')}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </Card>

        <div className="flex w-[340px] shrink-0 flex-col gap-3">
          <ReviewerPanel
            reviewRequestId={reviewRequestId}
            members={(members ?? []).map((member) => ({
              memberId: member.id,
              name: member.name,
            }))}
            excludeMemberId={revision.requesterId}
          />

          {selected && (
            <>
              <p className="text-[12.5px] font-bold">{selected.term} · 코멘트</p>
              {threadComments.length === 0 && pendingForSelected.length === 0 && (
                <p className="text-[11px] text-text-quaternary">아직 코멘트가 없습니다.</p>
              )}
              {threadComments.map((comment) => {
                const name = nameByMemberId.get(comment.authorId) ?? '—'
                return (
                  <Card key={comment.id} className="p-[14px]">
                    <div className="mb-2 flex items-center gap-2">
                      <Avatar initial={name.charAt(0)} tone={toneFor(comment.authorId)} size={22} />
                      <span className="text-[12.5px] font-bold">{name}</span>
                      <span className="text-[10.5px] text-text-quaternary">
                        {toRelativeTime(comment.createdAt)}
                      </span>
                    </div>
                    <p className="text-[12.5px] leading-[1.6] text-text-secondary">
                      {comment.content}
                    </p>
                  </Card>
                )
              })}
              {pendingForSelected.map((comment, idx) => (
                <Card key={'pending-' + idx} className="border-dashed p-[14px]">
                  <div className="mb-2 flex items-center gap-2">
                    <Avatar
                      initial={currentMember?.displayName.charAt(0) ?? '나'}
                      tone="accent"
                      size={22}
                    />
                    <span className="text-[12.5px] font-bold">
                      {currentMember?.displayName ?? '나'}
                    </span>
                    <span className="text-[10.5px] text-text-quaternary">미제출</span>
                  </div>
                  <p className="text-[12.5px] leading-[1.6] text-text-secondary">
                    {comment.content}
                  </p>
                </Card>
              ))}

              <div className="flex flex-col gap-2">
                <textarea
                  value={commentDraft}
                  onChange={(event) => setCommentDraft(event.target.value)}
                  placeholder="댓글 남기기… (Approve / Change request 할 때 함께 제출됩니다)"
                  rows={2}
                  className="rounded-[10px] border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] text-text placeholder:text-text-quaternary"
                />
                <Button
                  size="sm"
                  variant="primary"
                  className="self-end"
                  onClick={stashComment}
                  disabled={!commentDraft.trim()}
                >
                  담기
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
