import { httpClient } from '../../../shared/api/httpClient'
import type { AsyncJobStatus } from '../../../shared/api/jobPolling'
import type {
  DocumentId,
  DraftDocumentId,
  MemberId,
} from '../../../shared/types/ids'

// 비동기 문서 대조 작업(docs/API.md «비동기 문서 대조», DD-5)의 타입·접수·폴링.
// 접수(POST)와 폴링(GET)이 같은 응답 형식을 쓰므로 한 파일에 모은다.

interface CheckJobApiResponse {
  checkJobId: number
  documentId: number
  status: AsyncJobStatus
  draftDocumentId: number | null
  failureReason: string | null
  requestedBy: number
  createdAt: string
  updatedAt: string
}

export interface CheckJob {
  id: string
  documentId: DocumentId
  status: AsyncJobStatus
  /** SUCCEEDED일 때만 채워진다 — 대조 결과로 만들어진 문서 초안 */
  draftDocumentId: DraftDocumentId | null
  /** FAILED일 때만 채워진다 */
  failureReason: string | null
  requestedBy: MemberId
  createdAt: string
  updatedAt: string
}

function toCheckJob(response: CheckJobApiResponse): CheckJob {
  return {
    id: String(response.checkJobId),
    documentId: String(response.documentId),
    status: response.status,
    draftDocumentId:
      response.draftDocumentId === null ? null : String(response.draftDocumentId),
    failureReason: response.failureReason,
    requestedBy: String(response.requestedBy),
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
  }
}

/**
 * 문서 대조 작업 접수(`POST /api/draft-documents/checks`, DD-5).
 *
 * 최신 확정 본문을 활성 사전집과 대조해 문서 초안 + 제안어를 만든다. `202 Accepted`와
 * 함께 작업 상태를 즉시 돌려주고, 실제 대조는 외부 AI 워커가 수행한다(D-66·D-68).
 *
 * **요청자는 인증 주체에서 해석되므로 보내지 않는다**(NFR-USR-001). 같은 문서에 진행 중인
 * 초안·리뷰가 있거나 같은 워크스페이스에 진행 중인 사전 초안이 있으면 백엔드가 거절한다.
 */
export async function createCheckJob(documentId: DocumentId): Promise<CheckJob> {
  const response = await httpClient.post<CheckJobApiResponse>('/api/draft-documents/checks', {
    documentId: Number(documentId),
  })
  return toCheckJob(response)
}

/**
 * 문서 대조 작업 상태 조회(`GET /api/draft-documents/checks/{checkJobId}`).
 * 폴링 전용이다 — 간격·상한은 `shared/api/jobPolling.ts`가 정한다.
 */
export async function fetchCheckJob(checkJobId: string): Promise<CheckJob> {
  const response = await httpClient.get<CheckJobApiResponse>(
    `/api/draft-documents/checks/${checkJobId}`,
  )
  return toCheckJob(response)
}
