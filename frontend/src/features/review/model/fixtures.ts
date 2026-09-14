import { CURRENT_REVISION } from '../api/fetchDictionaryRevision'

// 리뷰 화면이 쓰는 뷰 타입. 실 연동(T-INT-12) 이후 목업 배열은 전부 걷어냈고, 화면 전용
// 표현(색·개수 등 도메인 타입에 없는 값)만 남았다.

/**
 * 사전집당 진행 중인 개정안은 1개라는 정책(docs/DOMAIN.md)에 기대어 사이드바·사전집
 * 화면이 목록 없이 바로 들어온다. 실 API에는 그런 지름길이 없어 라우트 파라미터로는
 * 이 sentinel을 넘기고 `fetchDictionaryRevision`이 조회로 푼다.
 */
export const CURRENT_DICTIONARY_REVISION_ID = CURRENT_REVISION

export type RevisionChangeType = '추가' | '정의 수정'

export interface RevisionDictionaryTermRow {
  /** 코멘트를 달 때 targetItemId로 쓴다. */
  candidateTermId: string
  term: string
  change: RevisionChangeType
  changeTone: 'success' | 'warn'
  comments: number
  commentNote?: string
  highlighted?: boolean
}
