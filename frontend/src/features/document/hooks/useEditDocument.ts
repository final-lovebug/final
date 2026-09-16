import { useMutation, useQueryClient } from '@tanstack/react-query'
import { editDocumentContent } from '../api/editDocumentContent'
import { updateDocument } from '../api/updateDocument'
import type { DocumentListItem } from '../model/fixtures'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

export interface EditDocumentInput {
  /** 본문이 바뀐 경우에만 준다. 주면 새 버전이 발행된다. */
  content?: string
  /** 제목·라벨 중 하나라도 바뀐 경우에만 준다. 라벨은 통째로 교체된다. */
  metadata?: { title: string; labels: string[] }
}

/**
 * 한 화면에서 받은 편집을 백엔드의 두 엔드포인트로 나눠 보낸다.
 *
 * **본문을 먼저 보낸다.** 본문 편집만 거절될 수 있기 때문이다(진행 중인 초안이 걸린 문서는
 * 409다 — `DOCUMENT_DRAFT_IN_PROGRESS`·`DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT`). 제목을 먼저
 * 바꾸면 그 409에서 「제목만 바뀐 문서」가 남는다. 순서를 뒤집어 두면 거절됐을 때 아무것도
 * 바뀌지 않은 상태로 끝나고, 사용자는 초안을 정리한 뒤 같은 폼을 다시 저장하면 된다.
 *
 * 반대 순서의 실패(본문은 발행됐는데 제목 수정이 실패)는 트랜잭션으로 묶을 수단이 없다 —
 * 두 요청이므로. 그때는 발행된 버전이 남고 화면이 오류를 띄우므로, 사용자가 제목만 다시
 * 저장할 수 있다.
 */
export function useEditDocument(workspaceId: WorkspaceId, documentId: DocumentId) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (input: EditDocumentInput): Promise<DocumentListItem | null> => {
      const published = input.content === undefined
        ? null
        : await editDocumentContent(workspaceId, documentId, input.content)

      if (input.metadata) {
        await updateDocument(workspaceId, documentId, input.metadata)
      }

      return published
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['document', workspaceId, documentId] })
      queryClient.invalidateQueries({ queryKey: ['documents', workspaceId] })
      // 본문 편집은 버전을 하나 늘린다 — 이력 화면이 열려 있으면 낡은 목록이 남는다.
      queryClient.invalidateQueries({ queryKey: ['document-versions', workspaceId, documentId] })
      // 없는 이름을 보내면 라벨이 새로 생긴다(독립 생성 API가 없다) — 라벨 목록도 함께 턴다.
      queryClient.invalidateQueries({ queryKey: ['labels', workspaceId] })
    },
  })
}
