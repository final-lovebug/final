import type { AsyncJobStatus } from '../../../shared/api/jobPolling'
import type {
  DictionaryId,
  DocumentId,
  DraftDictionaryId,
  MemberId,
  WorkspaceId,
} from '../../../shared/types/ids'

// 용어 추출 작업(docs/API.md "용어 추출 작업 접수와 조회", DI-5)의 공통 타입·매핑.
// 접수(POST)와 폴링(GET)이 같은 응답 형식을 쓰므로 한 파일에 모은다.

/** 백엔드 `ExtractionJobResponse` 그대로. 숫자 id는 이 경계에서만 쓴다. */
export interface ExtractionJobApiResponse {
  extractionJobId: number
  workspaceId: number
  dictionaryId: number | null
  sourceDocumentIds: number[]
  status: AsyncJobStatus
  draftDictionaryId: number | null
  failureReason: string | null
  requestedBy: number
  createdAt: string
  updatedAt: string
}

export interface ExtractionJob {
  id: string
  workspaceId: WorkspaceId
  /** 첫 회차(기존 사전집 없음)면 null */
  dictionaryId: DictionaryId | null
  sourceDocumentIds: DocumentId[]
  status: AsyncJobStatus
  /** SUCCEEDED일 때만 채워진다 — 생성된 사전 초안 */
  draftDictionaryId: DraftDictionaryId | null
  /** FAILED일 때만 채워진다 */
  failureReason: string | null
  requestedBy: MemberId
  createdAt: string
  updatedAt: string
}

export function toExtractionJob(response: ExtractionJobApiResponse): ExtractionJob {
  return {
    id: String(response.extractionJobId),
    workspaceId: String(response.workspaceId),
    dictionaryId: response.dictionaryId === null ? null : String(response.dictionaryId),
    sourceDocumentIds: response.sourceDocumentIds.map(String),
    status: response.status,
    draftDictionaryId:
      response.draftDictionaryId === null ? null : String(response.draftDictionaryId),
    failureReason: response.failureReason,
    requestedBy: String(response.requestedBy),
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}
