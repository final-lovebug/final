import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Avatar,
  Banner,
  Button,
  Card,
  ColFlex,
  CommentCard,
  DataTable,
  Markdown,
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
import { commentsForTerm } from '../../features/review/model/reviewTimeline'
import { useSuggestionTermsOfDraft } from '../../features/document/hooks/useSuggestionTermsOfDraft'
import { placeSuggestionsInProposedBody } from '../../features/document/model/suggestionSegments'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useWorkspaceMembers } from '../../features/member/hooks/useWorkspaceMembers'
import { useWorkspace } from '../../features/workspace/hooks/useWorkspace'
import { useAuthStore } from '../../shared/stores/authStore'
import type { DraftComment } from '../../features/review/api/submitReview'
import type { SuggestionTerm } from '../../features/document/model/types'
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
 * 로딩·에러 처리, 「리뷰 마무리」 팝오버가 그것이다.
 *
 * **용어별 코멘트도 같은 모양으로 붙였다.** 본문에는 치환 결과만 남아 「고객 → 회원」처럼
 * 무엇이 무엇으로 바뀌었는지가 사라지므로, 개정안이 물고 있는 초안의 **제안어 목록을 표로
 * 세우고 행마다 코멘트를 달게 한다.** 코멘트의 `targetItemId`는 **제안어 id**다 — 사전집이
 * 후보어 id를 넣는 자리와 같고 서버는 이 값을 해석하지 않으므로 계약 변경이 없다.
 * 본문 위 하이라이트는 여전히 그리지 않는다(위 문단의 `D-61`).
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
  // 개정안이 물고 있는 초안의 제안어 — 본문에는 바뀐 결과만 들어 있어 무엇이 무엇으로
  // 바뀌었는지가 남지 않는다. 리뷰어가 그것 없이 판단할 수는 없다. **문서의 최신 초안이
  // 아니라 이 개정안의 초안**을 본다 — 리뷰가 도는 동안 새 대조가 돌 수 있다.
  const { data: suggestionTerms } = useSuggestionTermsOfDraft(revision?.draftDocumentId ?? null)
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
  const [selectedTermId, setSelectedTermId] = useState<string | null>(null)

  const nameByMemberId = new Map((members ?? []).map((member) => [member.id, member.name]))
  // 응답은 답글이 children으로 중첩된 트리다 — 답글 UI는 만들지 않지만(`REVIEW_UI_FIX_PLAN`
  // 1-1) 서버에 달려 있는 답글까지 세고 보여야 한다.
  const allComments = flattenComments(comments ?? [])

  const terms = suggestionTerms ?? []
  const selectedTerm = terms.find((term) => term.id === selectedTermId) ?? terms[0]
  const proposedBody = revision?.proposedBody ?? document?.content ?? ''
  // 교정이 끝난 본문 위에 제안어를 얹는다 — 자리를 확인하지 못한 것은 빠지므로
  // 표에 있는 건수와 다를 수 있다(아래 안내 문구가 그 차이를 말한다).
  const placedTerms = placeSuggestionsInProposedBody(proposedBody, terms)
  const bodyDecorations = placedTerms.map((item) => ({
    start: item.start,
    end: item.end,
    render: (text: string) => renderTermSpan(item.suggestion, text),
  }))
  const termLabel = (term: SuggestionTerm) =>
    term.status === 'APPLY_SUGGESTION'
      ? `${term.originTerm} → ${term.suggestionTerm}`
      : `${term.originTerm} (유지)`
  // 선택한 제안어에 달린 코멘트 — 판정(승인/변경요청)과 함께 보여준다.
  const termComments = selectedTerm
    ? commentsForTerm(reviews ?? [], comments ?? [], selectedTerm.id)
    : []
  const pendingForSelected = pending.filter(
    (comment) => comment.targetItemId === selectedTerm?.id,
  )

  /**
   * 본문에 얹는 제안어 표시. 적용은 **GitHub diff의 빨강**(연한 빨강 배경 + 진한 빨강 글자)이고
   * 무시는 회색 점선이다. 초록 배경은 본문 위에서 거의 보이지 않아 빨강으로 바꿨다.
   * **누르면 우측이 그 용어의 코멘트 스레드로 바뀐다** — 표의 행을 누르는 것과 같은 동작이다.
   * 선택 표시는 배경을 갈아끼우지 않고 테두리(outline)로 얹어 빨강이 그대로 남게 한다.
   */
  function renderTermSpan(term: SuggestionTerm, text: string) {
    const applied = term.status === 'APPLY_SUGGESTION'
    const selected = term.id === selectedTerm?.id
    return (
      <span
        onClick={() => setSelectedTermId(term.id)}
        title={
          applied
            ? `적용됨 — "${term.originTerm}" → "${term.suggestionTerm}"`
            : `유지됨 — ${term.rejectReason ?? '사유 없음'}`
        }
        className={cx(
          'cursor-pointer rounded-xs border-b-[1.5px] px-[2px]',
          applied
            ? 'border-diff-removed-border bg-diff-removed-bg font-semibold text-diff-removed'
            : 'border-dashed border-text-faint text-text-secondary',
          selected && 'font-bold outline-2 outline-offset-1 outline-accent',
        )}
      >
        {text}
      </span>
    )
  }

  function stashComment() {
    if (!commentDraft.trim() || !selectedTerm) return
    setPending((previous) => [
      ...previous,
      { content: commentDraft.trim(), targetItemId: selectedTerm.id },
    ])
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
                  addressedCommentIds: allComments.map((comment) => comment.id),
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
            <Markdown source={proposedBody} decorations={bodyDecorations} />
            <Banner tone="neutral" className="mt-6 text-[11.5px]">
              교정에서 수용한 치환이 이미 반영된 본문입니다. <b>빨강</b>은 제안어로 바뀐 자리,
              <b> 회색 점선</b>은 검토했지만 그대로 둔 자리입니다 — 누르면 오른쪽에서 그 용어에
              코멘트를 남길 수 있습니다.
              {placedTerms.length < terms.length && (
                <>
                  {' '}
                  제안 {terms.length}건 중 {placedTerms.length}건만 본문에서 자리를 확인했습니다.
                  나머지는 아래 표에서 보세요.
                </>
              )}
            </Banner>
          </Card>
          {/* 「무엇이 무엇으로 바뀌었나」를 세우는 표. 행을 누르면 우측이 그 용어의
              코멘트 스레드로 바뀐다 — 사전집 개정안의 후보어 표와 같은 조작이다. */}
          <Card className="overflow-hidden">
            <DataTable>
              <thead>
                <tr>
                  <Th>용어 변경</Th>
                  <Th>처리</Th>
                  <Th>코멘트</Th>
                </tr>
              </thead>
              <tbody>
                {terms.length === 0 && (
                  <Tr>
                    <Td colSpan={3} className="py-6 text-center text-[11px] text-text-quaternary">
                      이 개정안에는 용어 제안이 없습니다 — 본문이 교정 전과 같습니다.
                    </Td>
                  </Tr>
                )}
                {terms.map((term) => {
                  const applied = term.status === 'APPLY_SUGGESTION'
                  const pendingCount = pending.filter(
                    (comment) => comment.targetItemId === term.id,
                  ).length
                  const total =
                    allComments.filter((comment) => comment.targetItemId === term.id).length +
                    pendingCount
                  return (
                    <Tr
                      key={term.id}
                      clickable
                      onClick={() => setSelectedTermId(term.id)}
                      className={cx(
                        term.id === selectedTerm?.id &&
                          'bg-accent-bg-strong shadow-[inset_3px_0_0_var(--color-accent)]',
                      )}
                    >
                      <Td>
                        <span className="font-bold text-text">{term.originTerm}</span>
                        <span className="mx-[6px] text-text-quaternary">→</span>
                        <span
                          className={cx(
                            applied
                              ? 'font-bold text-success'
                              : 'text-text-quaternary line-through',
                          )}
                        >
                          {term.suggestionTerm}
                        </span>
                      </Td>
                      <Td>
                        <Pill tone={applied ? 'success' : 'neutral'}>
                          {applied ? '적용' : '무시'}
                        </Pill>
                        {!applied && term.rejectReason && (
                          <span className="ml-2 text-[11px] text-text-tertiary">
                            {term.rejectReason}
                          </span>
                        )}
                      </Td>
                      <Td className={cx(total ? 'font-semibold text-warn' : 'text-text-quaternary')}>
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
          {/* 지정되지 않은 참여자도 검토를 제출하고 정족수에 산입되므로(`G-4`), 지정 리뷰어만
              그리는 우측 패널만으로는 판정이 다 보이지 않는다. 용어 이름표는 넘기지 않는다 —
              이 화면의 코멘트는 전부 전체 코멘트다. */}
          <ReviewList
            reviews={reviews ?? []}
            comments={comments ?? []}
            reviewers={reviewers ?? []}
            members={(members ?? []).map((member) => ({ memberId: member.id, name: member.name }))}
            termNameById={new Map(terms.map((term) => [term.id, termLabel(term)]))}
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
            excludeMemberId={reviewRequest?.requesterId}
            canEdit={canManageReviewers}
          />

          {/* 우측은 **선택한 용어 하나의 스레드**다. 특정 용어에 매이지 않는 의견은
              「리뷰 마무리」의 전체 코멘트로 남긴다 — 사전집 개정안과 같은 규칙이다. */}
          {terms.length === 0 && (
            <p className="text-xs text-text-tertiary">
              코멘트를 달 용어가 없습니다. 의견은 「리뷰 마무리」의 전체 코멘트로 남기세요.
            </p>
          )}
          {selectedTerm && (
            <>
              <p className="text-[12.5px] font-bold">
                {termLabel(selectedTerm)} · 코멘트{' '}
                {termComments.length + pendingForSelected.length}
              </p>
              {termComments.length === 0 && pendingForSelected.length === 0 && (
                <p className="text-[11px] text-text-quaternary">
                  이 용어에 달린 코멘트가 없습니다.
                </p>
              )}
              {termComments.map(({ comment, verdict }) => {
                const name = nameByMemberId.get(comment.authorId) ?? '—'
                return (
                  <Card key={comment.id} className="p-[14px]">
                    <div className="mb-2 flex items-center gap-2">
                      <Avatar initial={name.charAt(0)} tone={toneFor(comment.authorId)} size={22} />
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
              {pendingForSelected.map((comment, idx) => (
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
                  placeholder="이 용어에 댓글 남기기… (리뷰 마무리에서 함께 제출됩니다)"
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
