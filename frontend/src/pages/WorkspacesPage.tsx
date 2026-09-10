import { PagePlaceholder } from './PagePlaceholder'

// AppLayout(사이드바/탑바) 밖의 라우트 — 워크스페이스를 고르기 전이라 특정 워크스페이스의
// 사이드바를 보여줄 수 없다. 목데이터 연결은 Phase 5.
export function WorkspacesPage() {
  return (
    <div className="min-h-screen bg-bg p-6">
      <PagePlaceholder title="워크스페이스 선택" routeKey="workspaces (pre-shell)" />
    </div>
  )
}
