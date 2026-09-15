import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

// ui/main.js renderAppShell() 이식. 로그인/워크스페이스 선택 화면은 이 레이아웃 밖에 있고,
// /workspaces/:workspaceId 하위 라우트만 사이드바+탑바로 감싼다 (router.tsx 참고).
// 비로그인 시 리다이렉트는 RequireAuth가 맡는다.
//
// 본문 여백은 ui의 `.content`(28px 32px, 1400px 이하에서 24px)를 그대로 따른다.
export function AppLayout() {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="flex-1 overflow-auto p-6 xl:px-8 xl:py-7">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
