import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Button,
  Card,
  ColFlex,
  CommentCard,
  DataTable,
  Pill,
  PrThread,
  Td,
  TextArea,
  Th,
  Toolbar,
  ToolbarSpacer,
  Tr,
  TwoCol,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { cx } from '../../shared/lib/cx'
import { toRelativeTime } from '../../shared/lib/relativeTime'
import { useDictionaryRevision } from '../../features/review/hooks/useDictionaryRevision'
import { useSubmitReview, useReviewProgress, useReviews } from '../../features/review/hooks/useSubmitReview'
import { usePerformRevise } from '../../features/review/hooks/useReviewLifecycle'
import { ReviewerPanel } from '../../features/review/components/ReviewerPanel'
import { ReviewList } from '../../features/review/components/ReviewList'
import { ReviewSubmitPopover } from '../../features/review/components/ReviewSubmitPopover'
import { commentsForTerm } from '../../features/review/model/reviewTimeline'
import { useReviewers } from '../../features/review/hooks/useReviewers'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useWorkspace } from '../../features/workspace/hooks/useWorkspace'
import { useAuthStore } from '../../shared/stores/authStore'
import type { DraftComment } from '../../features/review/api/submitReview'

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
  const { data: reviews } = useReviews(reviewRequestId)
  const { data: reviewers } = useReviewers(reviewRequestId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const { data: workspace } = useWorkspace(workspaceId)
  const submit = useSubmitReview(reviewRequestId)
  const revise = usePerformRevise(reviewRequestId)
  const currentMember = useAuthStore((state) => state.currentMember)

  const [selectedTermId, setSelectedTermId] = useState<string | null>(null)
  const [commentDraft, setCommentDraft] = useState('')
  const [pending, setPending] = useState<DraftComment[]>([])

  // 리뷰어를 넣고 뺄 수 있는 사람 — 리뷰를 요청한 본인이면서 ADMIN 이상이다.
  // 최종 판정은 서버가 한다(403이 올라오면 그대로 보여준다).
  const isAdminOrAbove =
    workspace?.myPermission === 'OWNER' || workspace?.myPermission === 'ADMIN'

  if (isLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  if (error || !data) {
    return (
      <p className="text-sm text-text-tertiary">
        {error instanceof Error ? error.message : '개정안을 불러오지 못했습니다.'}
      </p>
    )
  }

  const revision = data
  const canManageReviewers = isAdminOrAbove && currentMember?.id === revision.requesterId
  const selected =
    revision.rows.find((row) => row.candidateTermId === selectedTermId) ?? revision.rows[0]
  const nameByMemberId = new Map((members ?? []).map((member) => [member.id, member.name]))
  const threadComments = selected
    ? commentsForTerm(reviews ?? [], revision.comments, selected.candidateTermId)
    : []
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

  /**
   * 판정과 코멘트를 한 번에 제출한다(`D-63`).
   *
   * `summaryComment`는 「리뷰 마무리」의 전체 코멘트다 — 특정 후보어에 달리지 않으므로
   * `targetItemId` 없이 함께 보낸다.
   */
  function submitVerdict(verdict: 'APPROVED' | 'CHANGES_REQUESTED', summaryComment: string) {
    const comments =
      summaryComment === '' ? pending : [...pending, { content: summaryComment }]
    submit.mutate(
      { reviewRequestId, targetRound: revision.reexamineRound, verdict, comments },
      { onSuccess: () => setPending([]) },
    )
  }

  return (
    <div>
      <Toolbar>
        <h1 className="font-display text-lg font-bold text-text">
          사전집 개정안 — r{revision.baseVersionNo} → r{revision.baseVersionNo + 1}
        </h1>
        <Pill tone="neutral">{revision.status}</Pill>
        <ToolbarSpacer />
        <div className="flex gap-[10px]">
          {/* 「재교정」은 제출이 아니라 이동이다 — 고칠 대상은 이 표가 아니라 개정안이 물고
              있는 사전집 초안의 후보어다(`docs/plan/DRAFT_PLAN.md`). 초안 화면에서 고친 뒤
              거기서 「재교정 완료」를 누르면 회차가 올라가며 이 화면으로 돌아온다. */}
          {revision.status === 'CHANGES_REQUESTED' && (
            <Button
              variant="outline"
              title="개정하려는 단어를 고치러 초안으로 갑니다"
              onClick={() =>
                navigate(`${routes.dictionaryDraft(workspaceId)}?reviewRequest=${reviewRequestId}`)
              }
            >
              재교정
            </Button>
          )}

          <ReviewSubmitPopover
            pendingCommentCount={pending.length}
            isSubmitting={submit.isPending}
            disabled={currentMember?.id === revision.requesterId}
            disabledReason="본인이 올린 요청은 본인이 검토할 수 없습니다"
            onSubmit={(verdict, summaryComment) => submitVerdict(verdict, summaryComment)}
          />

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
      </Toolbar>

      {progress && (
        <p className="mb-4 text-[11px] text-text-quaternary">
          승인 {progress.approvedCount} · 변경요청 {progress.changesRequestedCount} · 필요 정족수{' '}
          {progress.requiredReviewerCount}
        </p>
      )}

      <TwoCol>
        <ColFlex className="flex flex-col gap-4">
          <Card className="overflow-hidden">
            <DataTable>
              <thead>
                <tr>
                  <Th>용어</Th>
                  <Th>변경</Th>
                  <Th>코멘트</Th>
                </tr>
              </thead>
              <tbody>
                {revision.rows.length === 0 && (
                  <Tr>
                    <Td colSpan={3} className="py-6 text-center text-[11px] text-text-quaternary">
                      이번 개정안에 달라지는 용어가 없습니다.
                    </Td>
                  </Tr>
                )}
                {revision.rows.map((row) => {
                  const pendingCount = pending.filter(
                    (comment) => comment.targetItemId === row.candidateTermId,
                  ).length
                  const total = row.comments + pendingCount
                  return (
                    <Tr
                      key={row.candidateTermId}
                      clickable
                      onClick={() => setSelectedTermId(row.candidateTermId)}
                      className={cx(
                        row.candidateTermId === selected?.candidateTermId &&
                          'bg-accent-bg-strong shadow-[inset_3px_0_0_var(--color-accent)]',
                      )}
                    >
                      <Td className="font-bold text-text">{row.term}</Td>
                      <Td>
                        <Pill tone={row.changeTone}>{row.change}</Pill>
                      </Td>
                      <Td
                        className={cx(
                          total ? 'font-semibold text-warn' : 'text-text-quaternary',
                        )}
                      >
                        {total === 0
                          ? '—'
                          : '💬 ' + total + '개' + (pendingCount ? ' (미제출 포함)' : '')}
                      </Td>
                    </Tr>
                  )
                })}
              </tbody>
            </DataTable>
          </Card>
          <ReviewList
            reviews={reviews ?? []}
            comments={revision.comments}
            reviewers={reviewers ?? []}
            members={(members ?? []).map((member) => ({ memberId: member.id, name: member.name }))}
            termNameById={new Map(revision.rows.map((row) => [row.candidateTermId, row.term]))}
            onSelectTerm={setSelectedTermId}
          />
        </ColFlex>

        <PrThread>
          <ReviewerPanel
            reviewRequestId={reviewRequestId}
            members={(members ?? []).map((member) => ({
              memberId: member.id,
              name: member.name,
            }))}
            excludeMemberId={revision.requesterId}
            canEdit={canManageReviewers}
          />

          {selected && (
            <>
              <p className="text-[12.5px] font-bold">
                {selected.term} · 코멘트 {threadComments.length + pendingForSelected.length}
              </p>
              {threadComments.length === 0 && pendingForSelected.length === 0 && (
                <p className="text-[11px] text-text-quaternary">아직 코멘트가 없습니다.</p>
              )}
              {threadComments.map(({ comment, verdict }) => {
                const name = nameByMemberId.get(comment.authorId) ?? '—'
                return (
                  <Card key={comment.id} className="p-[14px]">
                    <div className="mb-2 flex items-center gap-2">
                      <span className="text-[12.5px] font-bold">{name}</span>
                      <Pill tone={verdict === 'APPROVED' ? 'success' : 'warn'}>
                        {verdict === 'APPROVED' ? '✓ 승인' : '↻ 변경 요청'}
                      </Pill>
                      <span className="text-[10.5px] text-text-quaternary">
                        {toRelativeTime(comment.createdAt)}
                      </span>
                    </div>
                    <p className="whitespace-pre-wrap text-[12.5px] leading-[1.6] text-text-secondary">
                      {comment.content}
                    </p>
                  </Card>
                )
              })}
              {pendingForSelected.map((comment, index) => (
                <CommentCard
                  key={'pending-' + index}
                  name={currentMember?.displayName ?? '나'}
                  initial={currentMember?.displayName.charAt(0) ?? '나'}
                  tone="accent"
                  time="미제출"
                  text={comment.content}
                />
              ))}

              <div className="flex flex-col gap-2">
                <TextArea
                  value={commentDraft}
                  onChange={(event) => setCommentDraft(event.target.value)}
                  placeholder="댓글 남기기… (Approve / Change request 할 때 함께 제출됩니다)"
                  rows={2}
                  className="rounded-[10px] bg-surface-muted text-[12.5px]"
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
        </PrThread>
      </TwoCol>
    </div>
  )
}
