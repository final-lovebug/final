import { httpClient } from '../../../shared/api/httpClient'
import { fetchAllPages } from '../../../shared/api/fetchAllPages'
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
import type { Page } from '../../../shared/types/common'

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

  const requests = await fetchAllPages<ReviewRequestApiResponse>((page, size) =>
    httpClient.get<Page<ReviewRequestApiResponse>>(
      `/api/review-requests?workspaceId=${workspaceId}&type=DICTIONARY&page=${page}&size=${size}&sort=createdAt,desc`,
    ),
  )
  const active = requests.find((request) => !TERMINAL_STATUSES.has(request.status))
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
    // `size=200`은 규격 상한(100)을 넘어 400이었다 — 개정안 화면이 열리지 않던 원인이다.
    // 개정안은 후보어 전체가 한 화면에 실려야 하므로 상한 크기로 끝까지 페이징한다.
    fetchAllPages<CandidateTermApiResponse>((page, size) =>
      httpClient.get<Page<CandidateTermApiResponse>>(
        `/api/draft-dictionaries/${revision.draftId}/candidate-terms?page=${page}&size=${size}&sort=form,asc`,
      ),
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
    // 표기 묶음(variantForms)의 비대표 표현은 행이 되지 않는다 — 대표 표기 form 하나만
    // 용어가 된다(`docs/plan/DRAFT_PLAN.md`).
    rows: candidates.map((candidate) => {
      const change = toChangeType(candidate)
      const id = String(candidate.candidateTermId)
      const comments = commentCountByItemId.get(id) ?? 0
      return {
        candidateTermId: id,
        term: candidate.form,
        change,
        changeTone: change === '추가' ? ('success' as const) : ('neutral' as const),
        comments,
        highlighted: comments > 0,
      }
    }),
    comments,
  }
}

/**
 * 개정안 표의 변경 유형(T-INT-12 결정 5 → `docs/plan/DRAFT_PLAN.md`로 개정).
 *
 * - `EXTRACTED`(추출·수동 등록된 신규) → 추가
 * - `EXISTING`(이전 사전집에서 승계) → 승계
 *
 * **판정 상태로 거르지 않는다.** 예전에는 `KEPT`인 승계분을 「변경 아님」으로 보아 표에서
 * 뺐지만(`null`), 판정이 사라진 뒤 모든 후보어가 `PENDING`이라 그 규칙으로는 승계분이 전부
 * 「정의 수정」으로 잘못 찍힌다. 정말 고쳤는지는 활성 사전집과 값을 비교해야 알 수 있고 목록
 * 응답에 정의가 없어(`D-41`) 그 비교를 할 수 없으므로, 사실만 적고 판단은 리뷰어에게 맡긴다.
 */
function toChangeType(candidate: CandidateTermApiResponse): RevisionChangeType {
  return candidate.origin === 'EXTRACTED' ? '추가' : '승계'
}
