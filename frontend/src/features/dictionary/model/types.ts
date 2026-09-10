// docs/DOMAIN.md "Dictionary"·"DraftDictionary" 섹션 이식.
// ~~TermVersion~~(DictionaryVersion으로 대체), ~~ExtractTerm~~(대조/추출 실행 기록. MVP1
// 결과물인 DraftDictionary만 있으면 되므로 제외)처럼 문서에서 취소선 처리된 항목은 옮기지 않았다.
import type {
  CandidateTermId,
  DictionaryId,
  DictionaryVersionId,
  DocumentId,
  DraftDictionaryId,
  MemberId,
  RevisionDictionaryId,
  TermId,
  WorkspaceId,
} from '../../../shared/types/ids'

export type DictionaryStatus = 'ACTIVE' | 'ARCHIVED' // 활성중 / 보관중

export interface Dictionary {
  id: DictionaryId
  workspaceId: WorkspaceId
  name: string
  currentVersionNo: number
  status: DictionaryStatus
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface Term {
  id: TermId
  dictionaryId: DictionaryId
  /** 사전집 내 유일. 제안어로 제시되는 값 */
  preferredForm: string
  /** 코드·DB 네이밍 기준 */
  englishName?: string
  definition: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface DictionaryVersion {
  id: DictionaryVersionId
  dictionaryId: DictionaryId
  versionNo: number
  /** 확정 후 불변 */
  terms: Term[]
  publishedAt: string
  originRevisionId?: RevisionDictionaryId
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export type DraftDictionaryStatus = 'IN_PROGRESS' | 'REVIEW_REQUESTED' // 교정중 / 리뷰요청됨

export interface DraftDictionary {
  id: DraftDictionaryId
  workspaceId: WorkspaceId
  /** 어디에 등재될지 */
  dictionaryId: DictionaryId
  /** 여러 문서에서 모을 수 있음 */
  sourceDocumentIds: DocumentId[]
  candidates: CandidateTerm[]
  status: DraftDictionaryStatus
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

// 대기 / 등재승인 / 동의어편입 / 거절 / 보류
export type CandidateTermStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'MERGED_AS_SYNONYM'
  | 'REJECTED'
  | 'ON_HOLD'

export interface CandidateTerm {
  id: CandidateTermId
  draftDictionaryId: DraftDictionaryId
  /** 문서에서 추출된 표현 */
  form: string
  /** AI 초안 또는 사람 작성 */
  proposedDefinition?: string
  proposedEnglishName?: string
  /** 어디서 몇 번 나왔는지의 근거 */
  occurredDocumentIds: DocumentId[]
  occurrenceCount: number
  /** 검토 시 판단 근거 */
  contextSnippets?: string[]
  status: CandidateTermStatus
  /** 승인 후 생성된 표준 용어 */
  resultTermId?: TermId
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}
