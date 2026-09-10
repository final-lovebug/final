// docs/DOMAIN.md "ReviewRequest" 섹션 이식.
// Reexamine(재교정)·Revise(반영)는 "수행 이력" 레코드에 가까워, 목록/상세 화면을 실제로
// 만드는 Phase 6에서 필요해지면 추가한다(지금은 ReviewRequest.status 값으로 상태만 표현).
// RevisedTerm은 docs/DOMAIN.md 328~340줄 표가 SuggestionTerm과 필드가 완전히 동일해
// 문서상 중복(복사 붙여넣기로 보임)으로 판단, 별도 타입으로 옮기지 않았다.
//
// model 계층 예외: CandidateTerm은 dictionary 도메인 타입이지만, RevisionDictionary가
// 실제로 그 값을 그대로 포함하는 도메인 관계라 여기서 import한다 — hooks/api/components는
// 여전히 다른 도메인을 직접 참조하지 않는다(frontend/docs/ARCHITECTURE.md 참고).
import type { CandidateTerm } from '../../dictionary/model/types'
import type { TextRange } from '../../../shared/types/common'
import type {
  CommentId,
  DictionaryId,
  DocumentId,
  DraftDictionaryId,
  DraftDocumentId,
  MemberId,
  ReviewId,
  ReviewRequestId,
  ReviewerId,
  RevisionDictionaryId,
  RevisionDocumentId,
  WorkspaceId,
} from '../../../shared/types/ids'

export type ReviewRequestType = 'DOCUMENT' | 'DICTIONARY'

// 리뷰대기 / 리뷰중 / 변경요청 / 승인 / 반영완료 / 반려 / 취소
export type ReviewRequestStatus =
  | 'PENDING'
  | 'IN_REVIEW'
  | 'CHANGES_REQUESTED'
  | 'APPROVED'
  | 'MERGED'
  | 'REJECTED'
  | 'CANCELLED'

export interface ReviewRequest {
  id: ReviewRequestId
  workspaceId: WorkspaceId
  type: ReviewRequestType
  /** 유형에 따라 RevisionDocumentId | RevisionDictionaryId */
  revisionId: string
  title: string
  requesterId: MemberId
  reviewers: Reviewer[]
  status: ReviewRequestStatus
  approvedAt?: string
  revisedAt?: string
  description?: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface RevisionDocument {
  id: RevisionDocumentId
  reviewRequestId: ReviewRequestId
  documentId: DocumentId
  baseVersionNo: number
  draftDocumentId: DraftDocumentId
  proposedBody: string
  /** 0이 최초 제출 */
  reexamineRound: number
  /** Revise 후 채워짐 */
  resultVersionNo?: number
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface RevisionDictionary {
  id: RevisionDictionaryId
  reviewRequestId: ReviewRequestId
  dictionaryId: DictionaryId
  baseVersionNo: number
  draftDictionaryId: DraftDictionaryId
  proposedTerms: CandidateTerm[]
  reexamineRound: number
  resultVersionNo?: number
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface Reviewer {
  id: ReviewerId
  reviewRequestId: ReviewRequestId
  /** 워크스페이스 참여자여야 함 */
  memberId: MemberId
  required: boolean
  assignedAt: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export type ReviewVerdict = 'APPROVE' | 'REQUEST_CHANGES' | 'REJECT' // 승인 / 변경요청 / 반대

export interface Review {
  id: ReviewId
  reviewRequestId: ReviewRequestId
  reviewerId: ReviewerId
  /** 어느 재교정 회차를 봤는지 */
  targetRound: number
  verdict: ReviewVerdict
  comments?: Comment[]
  submittedAt: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface Comment {
  id: CommentId
  reviewId: ReviewId
  /** 사람만 */
  authorId: MemberId
  content: string
  /** 전체 대상이면 없음 */
  anchor?: TextRange
  /** 사전 리비전에서는 후보어 단위 */
  targetItemId?: string
  /** 답글 */
  parentId?: CommentId
  resolved: boolean
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}
