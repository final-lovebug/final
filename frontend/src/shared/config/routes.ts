// 라우트 경로 상수/빌더. react-router 라우트 트리(src/app/router.tsx)의 상대 경로 문자열과
// 여기 각 함수가 만드는 절대 경로는 항상 같은 구조를 가리켜야 한다 — 라우트 트리를 바꾸면
// 이 파일도 함께 바꾼다. 화면-라우트 매핑 근거는 frontend/docs/ARCHITECTURE.md 참고.
export const routes = {
  login: () => '/login',
  workspaces: () => '/workspaces',

  documents: (workspaceId: string) => `/workspaces/${workspaceId}/documents`,
  documentUpload: (workspaceId: string) =>
    `/workspaces/${workspaceId}/documents/upload`,
  documentDetail: (workspaceId: string, documentId: string) =>
    `/workspaces/${workspaceId}/documents/${documentId}`,
  documentHistory: (workspaceId: string, documentId: string) =>
    `/workspaces/${workspaceId}/documents/${documentId}/history`,
  termExtraction: (workspaceId: string, documentId: string) =>
    `/workspaces/${workspaceId}/documents/${documentId}/extract`,
  documentReview: (workspaceId: string, documentId: string) =>
    `/workspaces/${workspaceId}/documents/${documentId}/review`,
  documentReviewThread: (
    workspaceId: string,
    documentId: string,
    reviewId: string,
  ) => `/workspaces/${workspaceId}/documents/${documentId}/review/${reviewId}`,

  dictionary: (workspaceId: string) => `/workspaces/${workspaceId}/dictionary`,
  dictionaryDraft: (workspaceId: string) =>
    `/workspaces/${workspaceId}/dictionary/draft`,
  dictionaryHistory: (workspaceId: string) =>
    `/workspaces/${workspaceId}/dictionary/history`,
  dictionaryRevision: (workspaceId: string, revisionId: string) =>
    `/workspaces/${workspaceId}/dictionary/revisions/${revisionId}`,

  settings: (workspaceId: string) => `/workspaces/${workspaceId}/settings`,
  settingsMembers: (workspaceId: string) =>
    `/workspaces/${workspaceId}/settings/members`,
  settingsRuleset: (workspaceId: string) =>
    `/workspaces/${workspaceId}/settings/ruleset`,
  settingsNotifications: (workspaceId: string) =>
    `/workspaces/${workspaceId}/settings/notifications`,
  settingsLabels: (workspaceId: string) =>
    `/workspaces/${workspaceId}/settings/labels`,
} as const
