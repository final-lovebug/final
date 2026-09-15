import { CURRENT_REVISION } from '../api/fetchDictionaryRevision'

// 리뷰 화면이 쓰는 뷰 타입. 실 연동(T-INT-12) 이후 목업 배열은 전부 걷어냈고, 화면 전용
// 표현(색·개수 등 도메인 타입에 없는 값)만 남았다.

/**
 * 사전집당 진행 중인 개정안은 1개라는 정책(docs/DOMAIN.md)에 기대어 사이드바·사전집
 * 화면이 목록 없이 바로 들어온다. 실 API에는 그런 지름길이 없어 라우트 파라미터로는
 * 이 sentinel을 넘기고 `fetchDictionaryRevision`이 조회로 푼다.
 */
export const CURRENT_DICTIONARY_REVISION_ID = CURRENT_REVISION

/**
 * 개정안 표의 변경 유형.
 *
 * **`승계`가 늘었다**(`docs/plan/DRAFT_PLAN.md`). 판정이 사라지면서 이전 사전집에서 넘어온
 * 후보어(`EXISTING`)도 전부 개정안 행이 되는데, 그것을 「정의 수정」으로 적으면 손대지 않은
 * 용어까지 고친 것처럼 보인다. 정말 고쳤는지는 활성 사전집과 값을 비교해야 알 수 있고 그
 * 조회가 없으므로(`D-41`로 목록에 정의가 없다), 사실만 적는다 — 「이전 버전에서 넘어왔다」.
 */
export type RevisionChangeType = '추가' | '정의 수정' | '승계'

export interface RevisionDictionaryTermRow {
  /** 코멘트를 달 때 targetItemId로 쓴다. */
  candidateTermId: string
  term: string
  change: RevisionChangeType
  changeTone: 'success' | 'warn' | 'neutral'
  comments: number
  commentNote?: string
  highlighted?: boolean
}
