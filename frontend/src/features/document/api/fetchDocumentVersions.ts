import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_VERSION_FIXTURES } from '../model/versionFixtures'
import type { DocumentVersion } from '../model/types'
import type { DocumentId } from '../../../shared/types/ids'

export async function fetchDocumentVersions(
  documentId: DocumentId,
): Promise<DocumentVersion[]> {
  await delay()
  return DOCUMENT_VERSION_FIXTURES[documentId] ?? []
}
