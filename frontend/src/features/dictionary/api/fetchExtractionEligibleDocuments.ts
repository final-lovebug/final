import { delay } from '../../../shared/lib/delay'
import {
  EXTRACTION_ELIGIBLE_DOCUMENTS,
  type ExtractionEligibleDocument,
} from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export async function fetchExtractionEligibleDocuments(
  workspaceId: WorkspaceId,
): Promise<ExtractionEligibleDocument[]> {
  await delay()
  return workspaceId === 'potenup_be' ? EXTRACTION_ELIGIBLE_DOCUMENTS : []
}
