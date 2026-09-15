import { httpClient } from '../../../shared/api/httpClient'
import {
  toExtractionJob,
  type ExtractionJob,
  type ExtractionJobApiResponse,
} from './extractionJobApi'

/**
 * 용어 추출 작업 상태 조회(`GET /api/draft-dictionaries/extractions/{extractionJobId}`).
 * 폴링 전용이다 — 간격·상한은 `shared/api/jobPolling.ts`가 정한다.
 */
export async function fetchExtractionJob(extractionJobId: string): Promise<ExtractionJob> {
  const response = await httpClient.get<ExtractionJobApiResponse>(
    `/api/draft-dictionaries/extractions/${extractionJobId}`,
  )
  return toExtractionJob(response)
}
