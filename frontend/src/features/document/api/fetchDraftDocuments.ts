import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_FIXTURES } from '../model/fixtures'
import { SUGGESTION_FIXTURES } from '../model/suggestionFixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface DraftDocumentListItem {
  documentId: string
  title: string
  pendingCount: number
  updatedAt: string
}

// "초안" 사이드바 목록: 미해결(PENDING) 제안이 하나라도 남아있는 문서만 모은다.
// Document와 SuggestionTerm 두 도메인을 조합하는 뷰라 페이지가 아니라 api 계층에서
// 미리 join해서 내려준다 — 화면은 이 결과만 그리면 된다.
export async function fetchDraftDocuments(
  workspaceId: WorkspaceId,
): Promise<DraftDocumentListItem[]> {
  await delay()

  const pendingCountByDocumentId = new Map<string, number>()
  for (const suggestion of SUGGESTION_FIXTURES) {
    if (suggestion.status !== 'PENDING') continue
    const documentId = suggestion.draftDocumentId.replace(/^draft-/, '')
    pendingCountByDocumentId.set(
      documentId,
      (pendingCountByDocumentId.get(documentId) ?? 0) + 1,
    )
  }

  return DOCUMENT_FIXTURES.filter(
    (doc) => doc.workspaceId === workspaceId && pendingCountByDocumentId.has(doc.id),
  ).map((doc) => ({
    documentId: doc.id,
    title: doc.title,
    pendingCount: pendingCountByDocumentId.get(doc.id) ?? 0,
    updatedAt: doc.updatedAt,
  }))
}
