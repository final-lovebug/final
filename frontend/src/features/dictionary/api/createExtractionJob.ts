import { httpClient } from '../../../shared/api/httpClient'
import {
  toExtractionJob,
  type ExtractionJob,
  type ExtractionJobApiResponse,
} from './extractionJobApi'
import type { DictionaryId, DocumentId, WorkspaceId } from '../../../shared/types/ids'

export interface CreateExtractionJobInput {
  workspaceId: WorkspaceId
  /** 현재 활성 사전집. 아직 사전집이 없는 첫 회차는 null이다(docs/API.md). */
  dictionaryId: DictionaryId | null
  sourceDocumentIds: DocumentId[]
}

/**
 * 용어 추출 작업 접수(`POST /api/draft-dictionaries/extractions`, DI-5).
 *
 * `202 Accepted`와 함께 작업 상태를 즉시 돌려준다 — 실제 추출은 외부 AI 워커가 하고
 * 완료는 백엔드가 콜백으로 받는다(D-66·D-68). 화면은 반환된 `id`로 폴링한다.
 *
 * **요청자는 인증 주체에서 해석되므로 보내지 않는다**(NFR-USR-001). 워크스페이스
 * 관리자 이상만 접수할 수 있고, 같은 워크스페이스에 진행 중인 초안·추출 작업이 있거나
 * 대상 문서가 하나도 남지 않으면 백엔드가 거절한다(`ApiError`로 올라온다).
 */
export async function createExtractionJob(
  input: CreateExtractionJobInput,
): Promise<ExtractionJob> {
  const response = await httpClient.post<ExtractionJobApiResponse>(
    '/api/draft-dictionaries/extractions',
    {
      workspaceId: Number(input.workspaceId),
      dictionaryId: input.dictionaryId === null ? null : Number(input.dictionaryId),
      sourceDocumentIds: input.sourceDocumentIds.map(Number),
    },
  )
  return toExtractionJob(response)
}
