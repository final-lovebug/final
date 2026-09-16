import { httpClient } from '../../../shared/api/httpClient'
import { fetchMemberNames } from '../../../shared/api/memberNames'
import { mapDocument, type DocumentApiResponse } from './fetchDocument'
import { DOCUMENT_CONTENT_MAX_LENGTH } from './createDocument'
import type { DocumentListItem } from '../model/fixtures'
import type { DocumentId, WorkspaceId } from '../../../shared/types/ids'

// 실제 백엔드 연동(docs/API.md «본문 편집», PATCH /api/workspaces/{workspaceId}/documents/
// {documentId}/content → 200).
//
// **대조·초안·개정안을 거치지 않고 즉시 새 버전을 발행한다**(`G-9`). 그래서 응답이 204가
// 아니라 200이다 — 오른 `currentVersionNo`와 뒤집힌 `aligned`를 화면이 알아야 한다.
// 새 버전은 `edited = true`가 되고 `dictionaryVersionNo`는 이전 버전 값을 승계한다
// (`G-10`·`G-11`) — 상세 화면의 배지가 「재검사 필요」로 바뀌는 이유가 이것이다.
//
// **진행 중인 초안이 걸린 문서는 409다.** 그 문서에 진행 중인 문서 초안이 있거나
// (`DOCUMENT_DRAFT_IN_PROGRESS`), 그 문서가 진행 중인 사전 초안의 원천 문서일 때
// (`DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT`)다. 무엇을 끝내야 편집할 수 있는지가 서로 달라
// 코드가 둘로 나뉘어 있으므로, 화면은 서버 메시지를 그대로 보여준다.
//
// 길이 제약은 서버도 검증하지만(400 COMMON_INVALID_REQUEST) 업로드와 같은 이유로 여기서
// 먼저 걸러낸다.
export async function editDocumentContent(
  workspaceId: WorkspaceId,
  documentId: DocumentId,
  content: string,
): Promise<DocumentListItem> {
  if (content.length > DOCUMENT_CONTENT_MAX_LENGTH) {
    throw new Error(
      `문서 본문은 ${DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}자를 넘을 수 없습니다.`,
    )
  }

  const response = await httpClient.patch<DocumentApiResponse>(
    `/api/workspaces/${workspaceId}/documents/${documentId}/content`,
    { content },
  )
  const nameByMemberId = await fetchMemberNames([response.uploaderId])

  return mapDocument(response, nameByMemberId.get(response.uploaderId) ?? '—')
}
