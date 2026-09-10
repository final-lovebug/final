import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { LoginPage } from '../pages/LoginPage'
import { WorkspacesPage } from '../pages/WorkspacesPage'
import { DocumentListPage } from '../pages/document/DocumentListPage'
import { DocumentUploadPage } from '../pages/document/DocumentUploadPage'
import { DocumentDetailPage } from '../pages/document/DocumentDetailPage'
import { DocumentHistoryPage } from '../pages/document/DocumentHistoryPage'
import { TermExtractionPage } from '../pages/dictionary/TermExtractionPage'
import { DictionaryDraftPage } from '../pages/dictionary/DictionaryDraftPage'
import { DictionaryPage } from '../pages/dictionary/DictionaryPage'
import { DictionaryHistoryPage } from '../pages/dictionary/DictionaryHistoryPage'
import { DocumentReviewPage } from '../pages/review/DocumentReviewPage'
import { DocumentReviewThreadPage } from '../pages/review/DocumentReviewThreadPage'
import { DictionaryRevisionPage } from '../pages/review/DictionaryRevisionPage'
import { SettingsLayout } from '../pages/settings/SettingsLayout'
import { SettingsMembersPage } from '../pages/settings/SettingsMembersPage'
import { SettingsRulesetPage } from '../pages/settings/SettingsRulesetPage'
import { SettingsNotificationsPage } from '../pages/settings/SettingsNotificationsPage'
import { SettingsLabelsPage } from '../pages/settings/SettingsLabelsPage'

// ui/main.js의 CONTENT_RENDERERS/state.screen 매핑을 react-router 라우트 트리로 옮긴 것.
// 각 화면 ↔ 라우트 ↔ 담당 도메인 근거는 frontend/docs/ARCHITECTURE.md 매핑표 참고.
// 절대 경로 문자열은 이 파일이 아니라 src/shared/config/routes.ts가 기준이다 —
// 여기서는 상대 세그먼트만 쓰고, 네비게이션(Link/NavLink)에서는 routes.*()를 쓴다.
export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage />, handle: { title: '로그인' } },
  {
    path: '/workspaces',
    element: <WorkspacesPage />,
    handle: { title: '워크스페이스 선택' },
  },
  {
    path: '/workspaces/:workspaceId',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="documents" replace /> },
      {
        path: 'documents',
        element: <DocumentListPage />,
        handle: { title: '문서' },
      },
      {
        path: 'documents/upload',
        element: <DocumentUploadPage />,
        handle: { title: '문서' },
      },
      {
        path: 'documents/:documentId',
        element: <DocumentDetailPage />,
        handle: { title: '문서' },
      },
      {
        path: 'documents/:documentId/history',
        element: <DocumentHistoryPage />,
        handle: { title: '문서 버전 이력' },
      },
      {
        path: 'documents/:documentId/extract',
        element: <TermExtractionPage />,
        handle: { title: '사전집 초안' },
      },
      {
        path: 'documents/:documentId/review',
        element: <DocumentReviewPage />,
        handle: { title: '문서 검토' },
      },
      {
        path: 'documents/:documentId/review/:reviewId',
        element: <DocumentReviewThreadPage />,
        handle: { title: '문서 개정안 리뷰' },
      },
      {
        path: 'dictionary',
        element: <DictionaryPage />,
        handle: { title: '사전집' },
      },
      {
        path: 'dictionary/draft',
        element: <DictionaryDraftPage />,
        handle: { title: '사전집 초안' },
      },
      {
        path: 'dictionary/history',
        element: <DictionaryHistoryPage />,
        handle: { title: '사전집 리비전 이력' },
      },
      {
        path: 'dictionary/revisions/:revisionId',
        element: <DictionaryRevisionPage />,
        handle: { title: '사전집 개정안' },
      },
      {
        path: 'settings',
        element: <SettingsLayout />,
        handle: { title: '설정' },
        children: [
          { index: true, element: <Navigate to="members" replace /> },
          { path: 'members', element: <SettingsMembersPage /> },
          { path: 'ruleset', element: <SettingsRulesetPage /> },
          { path: 'notifications', element: <SettingsNotificationsPage /> },
          { path: 'labels', element: <SettingsLabelsPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/login" replace /> },
])
