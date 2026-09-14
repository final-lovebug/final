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
  /** 실 API(`GET /api/workspaces/{id}/dictionary`)의 `dictionaryId`. 사전집 행 하나가
   * 확정된 버전 하나이므로 이 id는 "현재 활성 버전"을 가리킨다 — 용어 추출 접수가
   * 요구하는 값이 이것이다(T-INT-17). */
  id: DictionaryId
  workspaceId: WorkspaceId
  /** 사전집에는 "이름" 개념이 없다(워크스페이스당 활성 1개 + 보관 N개일 뿐 — docs/API.md
   * «알아 둘 것 셋»). 화면 표시용으로만 합성한다. */
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

// 대기 / 등재승인 / 동의어편입 / 거절 / 보류 / 승계유지
// 백엔드 CandidateTermStatus와 1:1이다(docs/API.md "후보어 등록·수정·삭제·목록").
export type CandidateTermStatus =
  | 'PENDING'
  | 'REGISTRATION_APPROVED'
  | 'MERGED_AS_SYNONYM'
  | 'REJECTED'
  | 'ON_HOLD'
  | 'KEPT'

/** 추출된 신규(EXTRACTED) / 이전 사전집에서 승계(EXISTING). */
export type CandidateTermOrigin = 'EXTRACTED' | 'EXISTING'

/**
 * 사람이 등록할 때 고른 분류. **추출이 만든 후보어에는 없다** — 추출 파이프라인이
 * HOMOGRAPH를 건너뛰어 variantForms로 유도할 수도 없다(T-INT-11 결정 1).
 */
export type CandidateTermType = 'SYNONYM' | 'HOMOGRAPH' | 'VARIANT'

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
  origin?: CandidateTermOrigin
  /** 사람이 고른 분류. 추출 생성분은 비어 있다. */
  type?: CandidateTermType
  /** 추출기가 같은 개념으로 묶어 돌려준 표기 변형들(대표 표기 form 포함). */
  variantForms?: string[]
  /** 판정을 내린 처리자. 등록자(createdBy)와 다르다. */
  handledBy?: MemberId
  rejectReason?: string
  mergeTargetTermId?: TermId
  /** 승인 후 생성된 표준 용어 */
  resultTermId?: TermId
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}
