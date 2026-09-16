import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Banner,
  Button,
  Card,
  ColFlex,
  CommentCard,
  Markdown,
  Pill,
  PrThread,
  TextArea,
  Toolbar,
  ToolbarSpacer,
  TwoCol,
} from '../../shared/ui'
import { routes } from '../../shared/config/routes'
import { toRelativeTime } from '../../shared/lib/relativeTime'
import { useReviewThreadComments } from '../../features/review/hooks/useReviewThreadComments'
import {
  useSubmitReview,
  useReviewProgress,
  useReviews,
} from '../../features/review/hooks/useSubmitReview'
import {
  usePerformReexamine,
  usePerformRevise,
} from '../../features/review/hooks/useReviewLifecycle'
import { useReviewRequest } from '../../features/review/hooks/useReviewRequest'
import { useReviewers } from '../../features/review/hooks/useReviewers'
import { useDocumentRevision } from '../../features/review/hooks/useDocumentRevision'
import { ReviewerPanel } from '../../features/review/components/ReviewerPanel'
import { ReviewList } from '../../features/review/components/ReviewList'
import { ReviewSubmitPopover } from '../../features/review/components/ReviewSubmitPopover'
import { flattenComments } from '../../features/review/api/reviewApi'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useWorkspace } from '../../features/workspace/hooks/useWorkspace'
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
 * **라우트 파라미터 `reviewRequestId`는 개정안 id가 아니라 리뷰 요청 id다** — 코멘트 조회·검토 제출·
 * 재교정·반영이 전부 리뷰 요청 스코프다(`GET /api/review-requests/{id}/...`). 코멘트를
 * 새로 달 때만 reviewId가 필요한데, 그건 검토 제출과 한 트랜잭션으로 묶여 있다(`D-63`).
 *
 * **본문에 anchor 하이라이트를 그리지 않는다**(프로토타입의 `term-flag-*`에 해당하는 것).
 * 제안어의 anchor는 **교정 전** 초안 본문(`draftBody`) 기준인데, 개정안이 들고 있는
 * `proposedBody`는 교정완료 시점에 치환이 끝난 본문이라 두 좌표계가 어긋난다. 잘못된
 * 위치를 강조하는 것보다 「처리 내역」으로 무엇이 바뀌었는지 보여주는 편이 정확하다 —
 * 본문 위 표시는 문장 분할·오프셋 규격(`REQ-DOC-005`)이 선행돼야 한다(`D-61`).
 *
 * **(2026-09-16)** 사전집 개정안 화면에만 들어갔던 리뷰 UI 수정(`REVIEW_UI_FIX_PLAN`,
 * `D-95`~`D-98`)을 이 화면에도 맞췄다 — 사람별 리뷰 목록(`ReviewList`), 리뷰어 편집 권한,
 * 로딩·에러 처리, 「리뷰 마무리」 팝오버가 그것이다. **용어 단위 코멘트만 없을 뿐** 나머지
 * 흐름은 두 화면이 같다.
 */
export function DocumentRevisionPage() {
  const {
    workspaceId = '',
    documentId = '',
    reviewRequestId = '',
  } = useParams<{
    workspaceId: string
    documentId: string
    reviewRequestId: string
  }>()
  const navigate = useNavigate()
  const { data: comments } = useReviewThreadComments(reviewRequestId)
  const {
    data: reviewRequest,
    isLoading: isReviewRequestLoading,
    error: reviewRequestError,
  } = useReviewRequest(reviewRequestId)
  const { data: progress } = useReviewProgress(reviewRequestId)
  const { data: reviews } = useReviews(reviewRequestId)
  const { data: reviewers } = useReviewers(reviewRequestId)
  const { data: revision } = useDocumentRevision(reviewRequestId)
  const { data: document } = useDocument(workspaceId, documentId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  const { data: workspace } = useWorkspace(workspaceId)
  const submit = useSubmitReview(reviewRequestId)
  const reexamine = usePerformReexamine(reviewRequestId)
  const revise = usePerformRevise(reviewRequestId)
  const currentMember = useAuthStore((state) => state.currentMember)
  const isRequester = currentMember?.id === reviewRequest?.requesterId

  // 리뷰어를 넣고 뺄 수 있는 사람 — 리뷰를 요청한 본인이면서 ADMIN 이상이다(사전집 화면과
  // 같은 식). 최종 판정은 서버가 한다.
  const isAdminOrAbove =
    workspace?.myPermission === 'OWNER' || workspace?.myPermission === 'ADMIN'
  const canManageReviewers = isAdminOrAbove && isRequester

  const [commentDraft, setCommentDraft] = useState('')
  const [pending, setPending] = useState<DraftComment[]>([])

  const nameByMemberId = new Map((members ?? []).map((member) => [member.id, member.name]))
  // 응답은 답글이 children으로 중첩된 트리다 — 답글 UI는 만들지 않지만(`REVIEW_UI_FIX_PLAN`
  // 1-1) 서버에 달려 있는 답글까지 세고 보여야 한다.
  const threadComments = flattenComments(comments ?? [])

  function stashComment() {
    if (!commentDraft.trim()) return
    setPending((previous) => [...previous, { content: commentDraft.trim() }])
    setCommentDraft('')
  }

  /**
   * 판정과 코멘트를 한 번에 제출한다(`D-63`).
   *
   * `summaryComment`는 「리뷰 마무리」의 전체 코멘트다. 이 화면은 본문 앵커가 없어
   * `targetItemId` 없는 코멘트만 오가므로 담아 둔 것과 같은 모양으로 붙이면 된다.
   */
  function submitVerdict(verdict: 'APPROVED' | 'CHANGES_REQUESTED', summaryComment: string) {
    const outgoing = summaryComment === '' ? pending : [...pending, { content: summaryComment }]
    // 리뷰어는 항상 최신 재교정 회차를 보고 검토한다 — 개정안이 아직 없으면 최초 회차(0)다.
    submit.mutate(
      {
        reviewRequestId,
        targetRound: revision?.reexamineRound ?? 0,
        verdict,
        comments: outgoing,
      },
      { onSuccess: () => setPending([]) },
    )
  }

  if (isReviewRequestLoading) return <p className="text-sm text-text-tertiary">불러오는 중…</p>
  // 개정안(`useDocumentRevision`)이 비는 것은 정상이다 — 재교정 전에는 개정안 행이 없고
  // 본문은 원본으로 그린다. 화면을 못 그리는 경우는 리뷰 요청 자체를 못 읽을 때뿐이다.
  if (reviewRequestError) {
    return (
      <p className="text-sm text-text-tertiary">
        {reviewRequestError instanceof Error
          ? reviewRequestError.message
          : '리뷰 요청을 불러오지 못했습니다.'}
      </p>
    )
  }

  return (
    <div>
      <Toolbar>
        <h1 className="font-display text-lg font-bold text-text">
          {reviewRequest?.title ?? (document?.title ?? '문서') + ' 개정 반영'}
        </h1>
        {reviewRequest && <Pill tone="neutral">{reviewRequest.status}</Pill>}
        <ToolbarSpacer />
        <div className="flex gap-[10px]">
          {reviewRequest?.status === 'CHANGES_REQUESTED' && (
            <Button
              variant="outline"
              disabled={reexamine.isPending}
              onClick={() =>
                reexamine.mutate({
                  proposedBody: revision?.proposedBody ?? document?.content,
                  addressedCommentIds: threadComments.map((comment) => comment.id),
                })
              }
            >
              재교정 완료
            </Button>
          )}
          <ReviewSubmitPopover
            pendingCommentCount={pending.length}
            isSubmitting={submit.isPending}
            disabled={isRequester}
            disabledReason="본인이 올린 요청은 본인이 검토할 수 없습니다"
            onSubmit={(verdict, summaryComment) => submitVerdict(verdict, summaryComment)}
          />
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
      </Toolbar>

      {progress && (
        <p className="mb-4 text-[11px] text-text-quaternary">
          승인 {progress.approvedCount} · 변경요청 {progress.changesRequestedCount} · 필요 정족수{' '}
          {progress.requiredReviewerCount}
        </p>
      )}

      <TwoCol>
        <ColFlex className="flex flex-col gap-4">
          <Card className="px-[30px] py-[26px] text-sm leading-[2.1] text-[#2A2D33]">
            {/* 개정안도 원본과 같은 마크다운 본문이다 — 상세 화면과 같은 뷰어로 그린다. */}
            <Markdown source={revision?.proposedBody ?? document?.content ?? ''} />
            <Banner tone="neutral" className="mt-6 text-[11.5px]">
              개정안 본문입니다 — 교정에서 수용한 치환이 이미 반영돼 있습니다. 무엇이 어떻게
              바뀌었는지는 문서 버전 이력의 「처리 내역」에서 확인하세요.
            </Banner>
          </Card>
          {/* 지정되지 않은 참여자도 검토를 제출하고 정족수에 산입되므로(`G-4`), 지정 리뷰어만
              그리는 우측 패널만으로는 판정이 다 보이지 않는다. 용어 이름표는 넘기지 않는다 —
              이 화면의 코멘트는 전부 전체 코멘트다. */}
          <ReviewList
            reviews={reviews ?? []}
            comments={comments ?? []}
            reviewers={reviewers ?? []}
            members={(members ?? []).map((member) => ({ memberId: member.id, name: member.name }))}
          />
        </ColFlex>

        <PrThread>
          <ReviewerPanel
            reviewRequestId={reviewRequestId}
            members={(members ?? []).map((member) => ({
              memberId: member.id,
              name: member.name,
            }))}
            excludeMemberId={reviewRequest?.requesterId}
            canEdit={canManageReviewers}
          />

          {threadComments.length === 0 && pending.length === 0 && (
            <p className="text-xs text-text-tertiary">아직 코멘트가 없습니다.</p>
          )}
          {threadComments.map((comment) => {
            const name = nameByMemberId.get(comment.authorId) ?? '—'
            return (
              <CommentCard
                key={comment.id}
                name={name}
                initial={name.charAt(0)}
                tone={toneFor(comment.authorId)}
                time={toRelativeTime(comment.createdAt)}
                text={comment.content}
                mine={comment.authorId === currentMember?.id}
              />
            )
          })}
          {pending.map((comment, idx) => (
            <CommentCard
              key={'pending-' + idx}
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
        </PrThread>
      </TwoCol>
    </div>
  )
}
