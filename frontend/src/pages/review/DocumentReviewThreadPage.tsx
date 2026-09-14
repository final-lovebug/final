import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Avatar, Button, Card, Pill } from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { toRelativeTime } from '../../shared/lib/relativeTime'
import { useReviewThreadComments } from '../../features/review/hooks/useReviewThreadComments'
import { useSubmitReview, useReviewProgress } from '../../features/review/hooks/useSubmitReview'
import {
  usePerformReexamine,
  usePerformRevise,
} from '../../features/review/hooks/useReviewLifecycle'
import { useReviewRequest } from '../../features/review/hooks/useReviewRequest'
import { useDocumentRevision } from '../../features/review/hooks/useDocumentRevision'
import { ReviewerPanel } from '../../features/review/components/ReviewerPanel'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useAuthStore } from '../../shared/stores/authStore'
import type { DraftComment } from '../../features/review/api/submitReview'
import type { AvatarTone } from '../../shared/ui'

const AVATAR_TONES: AvatarTone[] = ['accent', 'warn', 'success']

function toneFor(memberId: string): AvatarTone {
  let hash = 0
  for (const char of memberId) hash = (hash + char.charCodeAt(0)) % AVATAR_TONES.length
  return AVATAR_TONES[hash]
}

/**
 * 문서 개정안 검토 화면.
 *
 * **라우트의 `reviewId` 파라미터는 실제로는 리뷰 요청 id다** — 코멘트 조회·검토 제출·
 * 재교정·반영이 전부 리뷰 요청 스코프다(`GET /api/review-requests/{id}/...`). 코멘트를
 * 새로 달 때만 reviewId가 필요한데, 그건 검토 제출과 한 트랜잭션으로 묶여 있다(`D-63`).
 *
 * 본문의 하이라이트는 아직 없다 — 대조 결과의 위치(anchor)를 본문 위에 표시하는 렌더링은
 * `T-INT-10` 제안 클러스터와 `T-INT-17`에서 함께 다룬다.
 */
export function DocumentReviewThreadPage() {
  const {
    workspaceId = '',
    documentId = '',
    reviewId: reviewRequestId = '',
  } = useParams<{
    workspaceId: string
    documentId: string
    reviewId: string
  }>()
  const navigate = useNavigate()
  const { data: comments } = useReviewThreadComments(reviewRequestId)
  const { data: reviewRequest } = useReviewRequest(reviewRequestId)
  const { data: progress } = useReviewProgress(reviewRequestId)
  const { data: revision } = useDocumentRevision(reviewRequestId)
  const { data: document } = useDocument(workspaceId, documentId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const submit = useSubmitReview(reviewRequestId)
  const reexamine = usePerformReexamine(reviewRequestId)
  const revise = usePerformRevise(reviewRequestId)
  const currentMember = useAuthStore((state) => state.currentMember)

  const [commentDraft, setCommentDraft] = useState('')
  const [pending, setPending] = useState<DraftComment[]>([])

  const nameByMemberId = new Map((members ?? []).map((member) => [member.id, member.name]))

  function stashComment() {
    if (!commentDraft.trim()) return
    setPending((previous) => [...previous, { content: commentDraft.trim() }])
    setCommentDraft('')
  }

  function submitVerdict(verdict: 'APPROVED' | 'CHANGES_REQUESTED') {
    // 리뷰어는 항상 최신 재교정 회차를 보고 검토한다 — 개정안이 아직 없으면 최초 회차(0)다.
    submit.mutate(
      {
        reviewRequestId,
        targetRound: revision?.reexamineRound ?? 0,
        verdict,
        comments: pending,
      },
      { onSuccess: () => setPending([]) },
    )
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h1 className="font-display text-lg font-bold text-text">
            {reviewRequest?.title ?? (document?.title ?? '문서') + ' 개정 반영'}
          </h1>
          {reviewRequest && <Pill tone="neutral">{reviewRequest.status}</Pill>}
          {revision !== null && revision !== undefined && revision.reexamineRound > 0 && (
            <span className="text-[11px] text-text-quaternary">
              재교정 {revision.reexamineRound}회차
            </span>
          )}
        </div>
        <div className="flex gap-[10px]">
          {reviewRequest?.status === 'CHANGES_REQUESTED' && (
            <Button
              variant="outline"
              disabled={reexamine.isPending}
              onClick={() =>
                reexamine.mutate({
                  proposedBody: revision?.proposedBody ?? document?.content,
                  addressedCommentIds: (comments ?? []).map((comment) => comment.id),
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
            title={progress?.reviseEligible ? undefined : '정족수를 채워야 반영할 수 있습니다'}
            onClick={() =>
              revise.mutate(undefined, {
                onSuccess: () => navigate(routes.documentDetail(workspaceId, documentId)),
              })
            }
          >
            반영
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
        <Card className="flex-1 p-[26px] text-sm leading-[2.1] text-[#2A2D33]">
          <p className="whitespace-pre-wrap">{document?.content}</p>
        </Card>

        <div className="flex w-[340px] shrink-0 flex-col gap-3">
          <ReviewerPanel
            reviewRequestId={reviewRequestId}
            members={(members ?? []).map((member) => ({
              memberId: member.id,
              name: member.name,
            }))}
            excludeMemberId={reviewRequest?.requesterId}
          />

          {comments?.length === 0 && pending.length === 0 && (
            <p className="text-xs text-text-tertiary">아직 코멘트가 없습니다.</p>
          )}
          {comments?.map((comment) => {
            const name = nameByMemberId.get(comment.authorId) ?? '—'
            const mine = comment.authorId === currentMember?.id
            return (
              <Card
                key={comment.id}
                className={mine ? 'border-[1.5px] border-accent p-[14px]' : 'p-[14px]'}
              >
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
          {pending.map((comment, idx) => (
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
              <p className="text-[12.5px] leading-[1.6] text-text-secondary">{comment.content}</p>
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
        </div>
      </div>
    </div>
  )
}
