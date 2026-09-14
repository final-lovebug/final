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
  /** 실 API(`GET /api/workspaces/{id}/dictionary`, docs/API.md 857행)는 사전집을 별도
   * id·이름을 가진 리소스로 응답하지 않는다(워크스페이스당 활성 1개 + 보관 N개일 뿐,
   * "이름"이라는 개념 자체가 없다 — API.md 818행). 실연동에서는 `workspaceId` 기반으로
   * 합성한다. */
  id: DictionaryId
  workspaceId: WorkspaceId
  name: string
  currentVersionNo: number
  status: DictionaryStatus
  /** 실 API는 `publishedAt`/`publishedBy`만 준다 — 아래 두 필드로 따로 받는다. */
  publishedAt?: string
  publishedBy?: MemberId
  createdAt?: string
  createdBy?: MemberId
  updatedAt?: string
}

export interface Term {
  id: TermId
  /** 실 API 목록 응답의 항목(`TermSummary`)엔 이 필드가 없다(용어가 어느 사전집인지는
   * 조회 경로로 이미 알고 있어 안 실어 보낸다) — 목업 전용, 실연동 시 채우지 않는다. */
  dictionaryId?: DictionaryId
  /** 사전집 내 유일. 제안어로 제시되는 값 */
  preferredForm: string
  /** 코드·DB 네이밍 기준 */
  englishName?: string
  /** 실 API 목록 응답은 이 필드를 의도적으로 뺀다(`D-41` — definition 본문을 안 싣는다,
   * 단건 조회 엔드포인트도 없음). 실연동에서는 항상 비어 있다 — 화면은 "—"로 표시한다. */
  definition?: string
  createdAt?: string
  createdBy?: MemberId
  updatedAt?: string
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
