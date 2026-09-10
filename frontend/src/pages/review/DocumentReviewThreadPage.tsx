import { useParams } from 'react-router-dom'
import { Avatar, Button, Card } from '../../shared/ui'
import { useReviewThreadComments } from '../../features/review/hooks/useReviewThreadComments'

// ui/main.js renderReviewThreadScreen() 이식. 본문의 term-flag 밑줄은 정적 표시만 하고
// (실제 하이라이트는 대조 결과 데이터가 있어야 함), 댓글 목록은 목데이터로 실제 렌더링한다.
// 댓글 작성/삭제, Approve/Change request 액션은 백엔드가 없어 아직 붙이지 않았다.
export function DocumentReviewThreadPage() {
  const { reviewId = '' } = useParams<{ reviewId: string }>()
  const { data: comments } = useReviewThreadComments(reviewId)

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="font-display text-lg font-bold text-text">
          이용자→구독자 등 치환 12건 반영
        </h1>
        <div className="flex gap-[10px]">
          <Button variant="outline">Change request</Button>
          <Button variant="primary">Approve</Button>
        </div>
      </div>

      <div className="flex items-start gap-5">
        <Card className="flex-1 p-[26px] text-sm leading-[2.1] text-[#2A2D33]">
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
        </Card>

        <div className="flex w-[340px] shrink-0 flex-col gap-3">
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
          <div className="rounded-[10px] border border-border-strong bg-surface-muted px-3 py-[10px] text-[12.5px] text-text-quaternary">
            댓글 남기기…
          </div>
        </div>
      </div>
    </div>
  )
}
