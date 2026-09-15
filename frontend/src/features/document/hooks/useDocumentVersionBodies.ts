import { useQuery } from '@tanstack/react-query'
import { fetchDocumentVersionBody } from '../api/fetchDocumentVersions'
import { diffWords } from '../model/textDiff'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

/**
 * 두 버전의 본문을 읽어 인라인 diff를 만든다.
 *
 * 본문은 목록 응답에 없어 버전마다 단건 조회가 필요하다 — 그래서 두 버전이 다 정해졌을
 * 때만 돈다. diff 계산은 순수 함수(`model/textDiff.ts`)이고 그 한계도 거기 적혀 있다.
 */
export function useDocumentVersionDiff(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
  fromVersionNo: number | null,
  toVersionNo: number | null,
) {
  return useQuery({
    queryKey: ['document-version-diff', workspaceId, documentId, fromVersionNo, toVersionNo],
    queryFn: async () => {
      const [before, after] = await Promise.all([
        fetchDocumentVersionBody(workspaceId, documentId, fromVersionNo!),
        fetchDocumentVersionBody(workspaceId, documentId, toVersionNo!),
      ])
      return { before, after, diff: diffWords(before, after) }
    },
    enabled:
      workspaceId !== '' &&
      documentId !== '' &&
      fromVersionNo !== null &&
      toVersionNo !== null &&
      fromVersionNo !== toVersionNo,
  })
}
