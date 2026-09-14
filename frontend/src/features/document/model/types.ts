// docs/DOMAIN.md "Document"·"DraftDocument" 섹션 이식.
// ~~DictionaryContrast~~(1차 MVP 제외 명시), ~~Examine~~(교정 이력 기록. MVP1 결과물인
// SuggestionTerm.status만 있으면 되므로 제외)은 옮기지 않았다.
import type { TextRange } from '../../../shared/types/common'
import type {
  DocumentId,
  DocumentVersionId,
  DraftDocumentId,
  LabelId,
  MemberId,
  RevisionDocumentId,
  SuggestionTermId,
  WorkspaceId,
} from '../../../shared/types/ids'

export interface Document {
  id: DocumentId
  workspaceId: WorkspaceId
  title: string
  /** 작업 중 최신 본문. 10,000자 이내(docs/DOMAIN.md 정책) */
  content: string
  currentVersionNo: number
  /** 실 API(`DocumentResponse`)는 작성자·최종 수정자를 구분하지 않고 `uploaderId` 하나만
   * 준다 — 목업 전용 구분이라 옵셔널로 둔다(실연동에서는 ownerId만 uploaderId로 채움). */
  ownerId?: MemberId
  updaterId?: MemberId
  /** G-12/D-31 — 사전집 기준에 맞춰져 있고 그 뒤로 편집되지 않았는지. 실 API에서만 옴. */
  aligned?: boolean
  /** 직접 편집으로 발행된 버전인지(G-9). 실 API에서만 옴. */
  edited?: boolean
  /** 최신 확정 버전이 기준으로 삼은 사전집 버전. 사전집이 없으면 null. */
  dictionaryVersionNo?: number | null
  createdAt: string
  createdBy?: MemberId
  updatedAt: string
}

export interface DocumentVersion {
  id: DocumentVersionId
  documentId: DocumentId
  versionNo: number
  /** 불변 스냅샷. 실 API의 버전 "목록" 응답엔 본문이 없다(docs/API.md 738행 —
   * 특정 버전 단건 조회에만 있음) — 실연동에서는 undefined, 화면은 "—"로 표시한다.
   * 본문이 꼭 필요하면 버전 클릭 시 단건 조회로 후속 과제화한다. */
  body?: string
  publishedAt: string
  originRevisionId?: RevisionDocumentId
  /** 발행 시점 기준 사전집 버전 */
  dictionaryVersionNo?: number
  /** true면 사람이 직접 고친 버전, false면 업로드본이거나 교정 반영본이다(docs/API.md). */
  edited?: boolean
  /** 발행자 이름. `publishedBy`(회원 id)를 배치 조회로 해석한 값이다. */
  publishedByName?: string
  createdAt?: string
  createdBy?: MemberId
  updatedAt?: string
}

/** 실 API(`DraftDocumentStatus`) 그대로 — 교정중 / 교정완료 / 리뷰요청됨 / 반영됨. */
export type DraftDocumentStatus =
  | 'EXAMINING'
  | 'EXAMINED'
  | 'REVIEW_REQUESTED'
  | 'REVISED'

export interface DraftDocument {
  id: DraftDocumentId
  documentId: DocumentId
  /** 어느 버전에서 갈라졌는지 */
  baseVersionNo: number
  /** 교정 반영이 누적되는 본문. 제안어의 anchor가 가리키는 기준 본문이 이것이다. */
  draftBody: string
  status: DraftDocumentStatus
  /** 교정할 사람 */
  requestedBy?: MemberId
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

// 처리 전 / 기존 용어 유지 / 제안 용어 적용.
// **실 API의 이름은 `PENDING`·`KEPT_ORIGIN`·`APPLIED_SUGGESTION`이다** — 화면 코드가 이미
// 아래 이름을 쓰고 있어 api 계층(`draftDocumentApi.ts`)에서 양방향으로 변환한다.
export type SuggestionTermStatus = 'PENDING' | 'KEEP_ORIGINAL' | 'APPLY_SUGGESTION'

export interface SuggestionTerm {
  id: SuggestionTermId
  draftDocumentId: DraftDocumentId
  /** 본문 내 시작·끝 */
  anchor: TextRange
  /** 문서에 실제로 쓰인 비표준 표현 */
  originTerm: string
  /** 사전집 참조. 표기 복사 금지 */
  suggestionTerm: string
  status: SuggestionTermStatus
  handledBy?: MemberId
  /** 인용·고유명사 등 */
  rejectReason?: string
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

/**
 * "처리 내역" 화면 전용 뷰. 판정이 끝난 제안어를 사람이 읽는 한 줄로 줄인 것이라
 * 도메인 표에는 없다 — `SuggestionTerm`에서 파생한다(`api/fetchSuggestionHistory.ts`).
 *
 * `action: 'manual'`(직접 입력)은 **실 API에서 나오지 않는다.** 백엔드의 제안어 상태 축은
 * `PENDING`·`KEPT_ORIGIN`·`APPLIED_SUGGESTION` 셋뿐이고 "제안 대신 다른 말로 바꿔 썼다"를
 * 표현하는 자리가 없다(초안 본문 직접 수정 `PATCH /api/draft-documents/{id}`은 제안어
 * 판정과 별개다). 목업 시절 화면이 그 경우를 그리고 있어 타입과 렌더링은 남겨 두지만
 * 지금 데이터로는 항상 'applied' 또는 'ignored'다.
 */
export interface SuggestionHistoryItem {
  original: string
  result: string
  action: 'applied' | 'ignored' | 'manual'
  reason?: string
  manualValue?: string
}

/**
 * ⚠️ docs/DOMAIN.md 467~469줄 "모델 반영 필요(미확정)": 백엔드에 Label 엔티티와 Document의
 * 라벨/outdated 판별 속성이 아직 정의되지 않았다. 아래는 `ui/data.js`(LABELS)를 화면에 붙이기
 * 위한 프론트엔드 임시 타입 — 백엔드 모델이 확정되면 이 파일을 다시 확인해야 한다.
 */
export interface Label {
  id: LabelId
  name: string
}
