import type { Comment } from './types'
import { DOCUMENT_REVIEW_REQUEST_ID } from './reviewRequestFixtures'

// ui/data.js REVIEW_THREAD_COMMENTS/REVISION_THREAD/REVISION_ROWS 이전.
// mine/initial/tone처럼 화면에만 필요한 값은 Comment 도메인 타입에 없어 뷰 아이템으로 얹었다.
export interface CommentListItem extends Comment {
  authorName: string
  authorInitial: string
  authorTone: 'accent' | 'warn' | 'success'
  mine: boolean
}

export const DOCUMENT_REVIEW_THREAD_COMMENTS: CommentListItem[] = [
  {
    id: 'comment-1', reviewId: DOCUMENT_REVIEW_REQUEST_ID, authorId: 'member-mock-lee-be',
    authorName: '이백엔드', authorInitial: '이', authorTone: 'warn', mine: true,
    content: '여기 "오류"는 PG 응답 실패가 아니라 검증 오류입니다. 결제 실패로 치환하면 안 됩니다.',
    resolved: false,
    createdAt: '2026-09-01T00:00:00.000Z', createdBy: 'member-mock-lee-be', updatedAt: '2026-09-01T00:00:00.000Z',
  },
  {
    id: 'comment-2', reviewId: DOCUMENT_REVIEW_REQUEST_ID, authorId: 'member-mock-choi-mkt',
    authorName: '최마케팅', authorInitial: '최', authorTone: 'success', mine: false,
    content: '마케팅 문서에서는 "혜택금"으로도 씁니다. 사전에 매핑 하나 더 추가해주세요.',
    resolved: false,
    createdAt: '2026-09-01T02:00:00.000Z', createdBy: 'member-mock-choi-mkt', updatedAt: '2026-09-01T02:00:00.000Z',
  },
]

/** 사전집당 진행 중인 개정안은 1개뿐이라(docs/DOMAIN.md 정책) 목록 없이 이 id로 바로 간다. */
export const CURRENT_DICTIONARY_REVISION_ID = 'current'

export interface RevisionCommentListItem {
  initial: string
  name: string
  tone: 'accent' | 'warn'
  time: string
  text: string
  canConvert?: boolean
}

export const DICTIONARY_REVISION_THREAD: RevisionCommentListItem[] = [
  { initial: '김', name: '김개발', tone: 'accent', time: '40분 전', text: '기획팀은 장바구니 시점, 개발팀은 결제 완료 시점으로 씁니다. 표준어 정의를 결제 완료 기준으로 명확히 해주세요.', canConvert: true },
  { initial: '이', name: '이백엔드', tone: 'warn', time: '12분 전', text: '동의합니다. 결제 완료 시점으로 확정하죠.' },
]

export type RevisionChangeType = '추가' | '정의 수정' | '삭제'

export interface RevisionDictionaryTermRow {
  term: string
  change: RevisionChangeType
  changeTone: 'success' | 'warn' | 'danger'
  comments: number
  commentNote?: string
  highlighted?: boolean
}

export const DICTIONARY_REVISION_ROWS: RevisionDictionaryTermRow[] = [
  { term: '구독 일시정지', change: '추가', changeTone: 'success', comments: 0 },
  { term: '주문', change: '추가', changeTone: 'success', comments: 2, commentNote: '뜻이 갈립니다', highlighted: true },
  { term: '활성 일수', change: '정의 수정', changeTone: 'warn', comments: 0 },
]
