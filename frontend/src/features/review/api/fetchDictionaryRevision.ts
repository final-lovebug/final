import { httpClient } from '../../../shared/api/httpClient'
import type { Comment, ReviewRequestStatus } from '../model/types'
import type { RevisionChangeType, RevisionDictionaryTermRow } from '../model/fixtures'
import {
  flattenComments,
  toComment,
  type CommentApiResponse,
  type RevisionApiResponse,
  type ReviewRequestApiResponse,
} from './reviewApi'
import type { WorkspaceId } from '../../../shared/types/ids'

/** 라우트가 개정안 id 자리에 넘기는 sentinel — "이 워크스페이스의 진행 중인 사전 개정안". */
export const CURRENT_REVISION = 'current'

/** 더 이상 검토가 오갈 수 없는 상태들. `current` 해석에서 제외한다. */
const TERMINAL_STATUSES = new Set(['REVISED', 'CANCELED'])

interface CandidateTermApiResponse {
  candidateTermId: number
  form: string
  origin: 'EXTRACTED' | 'EXISTING'
  status: string
  proposedDefinition: string | null
}

interface PageResponse<T> {
  content: T[]
}

/**
 * `'current'`를 실제 리뷰 요청 id로 바꾼다.
 *
 * 사전집당 진행 중인 개정안은 1개라는 정책(`docs/DOMAIN.md`)에 기대어 사이드바·사전집
 * 화면이 목록 없이 바로 들어온다. 실 API에는 그런 지름길이 없어 여기서 조회로 푼다.
 */
async function resolveReviewRequestId(
  workspaceId: WorkspaceId,
  reviewRequestId: string,
): Promise<string> {
  if (reviewRequestId !== CURRENT_REVISION) return reviewRequestId

  const response = await httpClient.get<PageResponse<ReviewRequestApiResponse>>(
    `/api/review-requests?workspaceId=${workspaceId}&type=DICTIONARY&page=0&size=20&sort=createdAt,desc`,
  )
  const active = response.content.find((request) => !TERMINAL_STATUSES.has(request.status))
  if (!active) {
    throw new Error('진행 중인 사전집 개정안이 없습니다.')
  }
  return String(active.reviewRequestId)
}

export interface DictionaryRevisionDetail {
  revisionId: string
  reviewRequestId: string
  draftDictionaryId: string
  reexamineRound: number
  title: string
  status: ReviewRequestStatus
  requesterId: string
  baseVersionNo: number
  rows: RevisionDictionaryTermRow[]
  comments: Comment[]
}

/**
 * 사전 개정안 상세(`GET /api/review-requests/{reviewRequestId}/revision-dictionaries`).
 *
 * **세 번 부른다.** 개정안 응답(`RevisionResponse`)에는 후보어 목록이 없고 `draftId`만
 * 있어서, 그 초안의 후보어를 따로 조회해야 한다. 코멘트도 개정안이 아니라 리뷰 요청에
 * 달리므로 또 따로다.
 *
 * **인자가 revisionId가 아니라 reviewRequestId다** — 실 엔드포인트가 리뷰 요청 스코프다.
 * 라우트가 넘기는 `'current'` sentinel은 조회로 풀어준다.
 */
export async function fetchDictionaryRevision(
  workspaceId: WorkspaceId,
  reviewRequestIdOrCurrent: string,
): Promise<DictionaryRevisionDetail> {
  const reviewRequestId = await resolveReviewRequestId(workspaceId, reviewRequestIdOrCurrent)
  const revisions = await httpClient.get<RevisionApiResponse[]>(
    `/api/review-requests/${reviewRequestId}/revision-dictionaries`,
  )
  // 재교정이 돌면 회차마다 행이 쌓인다 — 화면은 마지막 회차만 본다.
  const revision = revisions.reduce<RevisionApiResponse | undefined>(
    (latest, current) =>
      latest === undefined || current.reexamineRound > latest.reexamineRound ? current : latest,
    undefined,
  )
  if (!revision) {
    throw new Error('사전 개정안을 찾을 수 없습니다.')
  }

  const [candidates, commentResponses, reviewRequest] = await Promise.all([
    httpClient.get<PageResponse<CandidateTermApiResponse>>(
      `/api/draft-dictionaries/${revision.draftId}/candidate-terms?page=0&size=200&sort=form,asc`,
    ),
    httpClient.get<CommentApiResponse[]>(`/api/review-requests/${reviewRequestId}/comments`),
    httpClient.get<ReviewRequestApiResponse>(`/api/review-requests/${reviewRequestId}`),
  ])

  const comments = commentResponses.map(toComment)
  const commentCountByItemId = new Map<string, number>()
  for (const comment of flattenComments(comments)) {
    if (comment.targetItemId === undefined) continue
    commentCountByItemId.set(
      comment.targetItemId,
      (commentCountByItemId.get(comment.targetItemId) ?? 0) + 1,
    )
  }

  return {
    revisionId: String(revision.id),
    reviewRequestId,
    draftDictionaryId: String(revision.draftId),
    reexamineRound: revision.reexamineRound,
    title: reviewRequest.title,
    status: reviewRequest.status,
    requesterId: String(reviewRequest.requesterId),
    baseVersionNo: revision.baseVersionNo,
    rows: candidates.content.flatMap((candidate) => {
      const change = toChangeType(candidate)
      if (change === null) return []
      const id = String(candidate.candidateTermId)
      const comments = commentCountByItemId.get(id) ?? 0
      return [
        {
          candidateTermId: id,
          term: candidate.form,
          change,
          changeTone: change === '추가' ? ('success' as const) : ('warn' as const),
          comments,
          highlighted: comments > 0,
        },
      ]
    }),
    comments,
  }
}

/**
 * 개정안 표의 변경 유형(T-INT-12 결정 5).
 *
 * - `EXTRACTED` → 추가
 * - `EXISTING` 이면서 판정이 내려진 것 → 정의 수정
 * - `EXISTING` 이면서 `KEPT`(손대지 않은 승계분) → 변경이 아니므로 표에서 뺀다(`null`)
 *
 * 이전 버전과 정의를 비교하는 API가 없어 이보다 정확히는 판별할 수 없다 — 「삭제」 분류를
 * 만들지 않은 것과 같은 이유다.
 */
function toChangeType(candidate: CandidateTermApiResponse): RevisionChangeType | null {
  if (candidate.origin === 'EXTRACTED') return '추가'
  return candidate.status === 'KEPT' ? null : '정의 수정'
}
