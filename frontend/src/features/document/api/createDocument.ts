import { delay } from '../../../shared/lib/delay'
import { DOCUMENT_FIXTURES, type DocumentListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

export interface CreateDocumentInput {
  workspaceId: WorkspaceId
  title: string
  content: string
  ownerId: string
  ownerName: string
}

// docs/DOMAIN.md 정책: 업로드 파일 형식은 txt/md 뿐이고 본문은 10,000자 이내다.
// 실제 파일 업로드는 이번 목업 단계에서 다루지 않고(텍스트 입력만), 길이 제약만 재현한다.
export const DOCUMENT_CONTENT_MAX_LENGTH = 10_000

export async function createDocument(
  input: CreateDocumentInput,
): Promise<DocumentListItem> {
  await delay()

  if (input.content.length > DOCUMENT_CONTENT_MAX_LENGTH) {
    throw new Error(
      `문서 본문은 ${DOCUMENT_CONTENT_MAX_LENGTH.toLocaleString()}자를 넘을 수 없습니다.`,
    )
  }

  const now = new Date().toISOString()
  const created: DocumentListItem = {
    id: `doc-${crypto.randomUUID()}`,
    workspaceId: input.workspaceId,
    title: input.title,
    content: input.content,
    currentVersionNo: 0,
    ownerId: input.ownerId,
    updaterId: input.ownerId,
    ownerName: input.ownerName,
    updaterName: input.ownerName,
    createdAt: now,
    createdBy: input.ownerId,
    updatedAt: now,
  }

  // 실제 백엔드가 생기기 전까지만 메모리에 반영 — 새로고침하면 사라진다.
  DOCUMENT_FIXTURES.push(created)
  return created
}
