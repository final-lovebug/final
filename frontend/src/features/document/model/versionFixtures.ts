import type { DocumentVersion } from './types'

// ui/data.js DOC_HISTORY_VERSIONS 이전. 원본 프로토타입은 FOCUS_DOC(doc-plan) 한 문서
// 기준으로만 버전 이력을 보여줬다 — 다른 문서는 이력이 없는 것으로 둔다.
export const DOCUMENT_VERSION_FIXTURES: Record<string, DocumentVersion[]> = {
  'doc-plan': [
    {
      id: 'doc-plan-v3',
      documentId: 'doc-plan',
      versionNo: 3,
      body: '치환 제안 12건 중 적용 9 · 무시 2 · 직접입력 1',
      publishedAt: '2026-09-01T11:30:00.000Z',
      dictionaryVersionNo: 4,
      createdAt: '2026-09-01T11:30:00.000Z',
      createdBy: 'member-mock-park-pm',
      updatedAt: '2026-09-01T11:30:00.000Z',
    },
    {
      id: 'doc-plan-v2',
      documentId: 'doc-plan',
      versionNo: 2,
      body: '2절 비즈니스 규칙 문단 추가',
      publishedAt: '2026-08-31T09:05:00.000Z',
      dictionaryVersionNo: 4,
      createdAt: '2026-08-31T09:05:00.000Z',
      createdBy: 'member-mock-park-pm',
      updatedAt: '2026-08-31T09:05:00.000Z',
    },
    {
      id: 'doc-plan-v1',
      documentId: 'doc-plan',
      versionNo: 1,
      body: '최초 업로드',
      publishedAt: '2026-08-28T14:20:00.000Z',
      createdAt: '2026-08-28T14:20:00.000Z',
      createdBy: 'member-mock-park-pm',
      updatedAt: '2026-08-28T14:20:00.000Z',
    },
  ],
}
