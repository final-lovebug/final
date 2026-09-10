import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Avatar, Button, Card } from '../../shared/ui'
import { useReviewThreadComments } from '../../features/review/hooks/useReviewThreadComments'
import { useAddReviewThreadComment } from '../../features/review/hooks/useAddReviewThreadComment'
import { useDocument } from '../../features/document/hooks/useDocument'
import { useAuthStore } from '../../shared/stores/authStore'
import { DOCUMENT_REVIEW_REQUEST_ID } from '../../features/review/model/reviewRequestFixtures'

// ui/main.js renderReviewThreadScreen() 이식. 댓글 목록 조회·등록 모두 실제로 동작한다.
// Approve/Change request 액션은 백엔드가 없어 아직 붙이지 않았다.
//
// 본문 표시는 두 갈래다: 미리 만들어둔 doc-plan 리뷰(DOCUMENT_REVIEW_REQUEST_ID)는 원본
// ui/main.js의 term-flag 하이라이트를 그대로 보여주고, `DocumentReviewPage`에서 새로
// 생성된 리뷰(예: doc-retention)는 하이라이트 없이 문서 본문(content)만 보여준다 — 실제
// 하이라이트는 백엔드의 대조 결과 위치(anchor)가 있어야 재현 가능해서 지금은 생략했다.
export function DocumentReviewThreadPage() {
  const { documentId = '', reviewId = '' } = useParams<{
    documentId: string
    reviewId: string
  }>()
  const { data: comments } = useReviewThreadComments(reviewId)
  const addComment = useAddReviewThreadComment(reviewId)
  const { data: document } = useDocument(documentId)
  const currentMember = useAuthStore((state) => state.currentMember)
  const [commentDraft, setCommentDraft] = useState('')
  const isSeededDemoReview = reviewId === DOCUMENT_REVIEW_REQUEST_ID

  function handleSubmitComment() {
    if (!commentDraft.trim() || !currentMember) return
    addComment.mutate(
      {
        reviewId,
        authorId: currentMember.id,
        authorName: currentMember.displayName,
        authorInitial: currentMember.displayName.charAt(0),
        content: commentDraft.trim(),
      },
      { onSuccess: () => setCommentDraft('') },
    )
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-lg font-bold text-text">
          {isSeededDemoReview
            ? '이용자→구독자 등 치환 12건 반영'
            : `${document?.title ?? '문서'} 개정 반영`}
        </h1>
        <div className="flex gap-[10px]">
          <Button variant="outline">Change request</Button>
          <Button variant="primary">Approve</Button>
        </div>
      </div>

      <div className="flex items-start gap-5">
        <Card className="flex-1 p-[26px] text-sm leading-[2.1] text-[#2A2D33]">
          {isSeededDemoReview ? (
            <>
              새로운 신규 획득 정책 2026에 따라{' '}
              <span className="border-b-2 border-accent bg-accent-bg">이용자</span>를 대상으로
              다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은{' '}
              <span className="border-b-2 border-border-strong">체험판</span> 14일이 충분한
              시점을 기준으로 합니다. 이때 고객의 상태는{' '}
              <span className="border-b-2 border-border-strong">가입완료</span>로 자동
              전환되어야 합니다. 만약 이 과정에서{' '}
              <span className="border-b-2 border-danger bg-danger-bg">오류</span>가 발생할
              경우, 시스템은 즉시{' '}
              <span className="border-b-2 border-danger bg-danger-bg">탈퇴</span> 처리를
              진행하고 안내 메일을 발송해야 합니다.
            </>
          ) : (
            <p className="whitespace-pre-wrap">{document?.content}</p>
          )}
        </Card>

        <div className="flex w-[340px] shrink-0 flex-col gap-3">
          {comments?.length === 0 && (
            <p className="text-xs text-text-tertiary">아직 코멘트가 없습니다.</p>
          )}
          {comments?.map((comment) => (
            <Card
              key={comment.id}
              className={comment.mine ? 'border-[1.5px] border-accent p-[14px]' : 'p-[14px]'}
            >
              <div className="mb-2 flex items-center gap-2">
                <Avatar
                  initial={comment.authorInitial}
                  tone={comment.authorTone}
                  size={22}
                />
                <span className="text-[12.5px] font-bold">{comment.authorName}</span>
                <span className="text-[10.5px] text-text-quaternary">
                  {new Date(comment.createdAt).toLocaleString('ko-KR')}
                </span>
              </div>
              <p className="text-[12.5px] leading-[1.6] text-text-secondary">
                {comment.content}
              </p>
              {comment.mine && (
                <p className="mt-2 cursor-pointer text-[11px] font-semibold text-accent-strong">
                  삭제
                </p>
              )}
            </Card>
          ))}
          <div className="flex flex-col gap-2">
            <textarea
              value={commentDraft}
              onChange={(event) => setCommentDraft(event.target.value)}
              placeholder="댓글 남기기…"
              rows={2}
              className="rounded-[10px] border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] text-text placeholder:text-text-quaternary"
            />
            <Button
              size="sm"
              variant="primary"
              className="self-end"
              onClick={handleSubmitComment}
              disabled={addComment.isPending || !commentDraft.trim()}
            >
              등록
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
