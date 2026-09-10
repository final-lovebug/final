import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_FIXTURES, type DocumentListItem } from '../model/fixtures'
import type { DocumentId } from '../../../shared/types/ids'

export async function fetchDocument(
  documentId: DocumentId,
): Promise<DocumentListItem | undefined> {
  await delay()
  return DOCUMENT_FIXTURES.find((doc) => doc.id === documentId)
}
