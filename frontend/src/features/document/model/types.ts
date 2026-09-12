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
  ownerId: MemberId
  updaterId: MemberId
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export interface DocumentVersion {
  id: DocumentVersionId
  documentId: DocumentId
  versionNo: number
  /** 불변 스냅샷 */
  body: string
  publishedAt: string
  originRevisionId?: RevisionDocumentId
  /** 발행 시점 기준 사전집 버전 */
  dictionaryVersionNo?: number
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

export type DraftDocumentStatus = 'IN_PROGRESS' | 'DONE' // 교정 중 / 교정완료

export interface DraftDocument {
  id: DraftDocumentId
  documentId: DocumentId
  /** 어느 버전에서 갈라졌는지 */
  baseVersionNo: number
  /** 교정 반영이 누적되는 본문 */
  draftBody: string
  status: DraftDocumentStatus
  /** 교정할 사람 */
  requestedBy?: MemberId
  createdAt: string
  createdBy: MemberId
  updatedAt: string
}

// 처리 전 / 기존 용어 유지 / 제안 용어 적용
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
 * ⚠️ docs/DOMAIN.md 467~469줄 "모델 반영 필요(미확정)": 백엔드에 Label 엔티티와 Document의
 * 라벨/outdated 판별 속성이 아직 정의되지 않았다. 아래는 `ui/data.js`(LABELS)를 화면에 붙이기
 * 위한 프론트엔드 임시 타입 — 백엔드 모델이 확정되면 이 파일을 다시 확인해야 한다.
 */
export interface Label {
  id: LabelId
  name: string
}
