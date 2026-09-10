import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

// ui/main.js renderAppShell() 이식. 로그인/워크스페이스 선택 화면은 이 레이아웃 밖에 있고,
// /workspaces/:workspaceId 하위 라우트만 사이드바+탑바로 감싼다 (router.tsx 참고).
// 인증 여부 확인(비로그인 시 /login으로 리다이렉트)은 Zustand 인증 스토어가 생기는
// Phase 5에서 추가한다 — 지금은 라우팅 뼈대만 잡는다.
export function AppLayout() {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="flex-1 overflow-auto p-7">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
