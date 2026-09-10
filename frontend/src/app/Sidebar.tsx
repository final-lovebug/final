import type { ReactNode } from 'react'
import { NavLink, useParams } from 'react-router-dom'
import { Avatar } from '../shared/ui'
import { cx } from '../shared/lib/cx'
import { routes } from '../shared/config/routes'

// ui/main.js renderSidebar() 이식. 원본은 "문서" 그룹에 초안(reviewDoc)·개정안(reviewThread),
// "사전집" 그룹에 초안(draft)·개정안(revision)까지 퀵링크로 뒀지만, 그 화면들은 실제로는
// 특정 문서/리비전 id가 있어야 진입할 수 있어(frontend/docs/ARCHITECTURE.md 라우트 참고)
// 목록 없이 사이드바에서 바로 연결할 수 없다. 그래서 지금은 목록형 화면만 넣었고,
// 리뷰류 화면은 각 목록에서 드릴다운으로 들어가는 것으로 둔다. 실제 데이터가 붙는
// Phase 5~6에서 이 결정을 다시 볼 수 있다.
export function Sidebar() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()

  return (
    <div className="flex min-h-screen w-[248px] shrink-0 flex-col border-r border-border-soft bg-surface">
      <div className="flex items-center justify-between border-b border-border-soft px-[18px] py-4">
        <div className="flex items-center gap-2">
          <Avatar initial="P" tone="accent" size={24} />
          <span className="font-display text-[13.5px]">워크스페이스</span>
        </div>
        <span className="text-[11px] text-text-quaternary">▾</span>
      </div>

      <nav className="flex flex-1 flex-col gap-4 overflow-auto p-3">
        <SidebarGroup title="문서">
          <SidebarItem to={routes.documents(workspaceId)} label="문서 목록" />
        </SidebarGroup>
        <SidebarGroup title="사전집">
          <SidebarItem to={routes.dictionary(workspaceId)} label="사전집" />
          <SidebarItem to={routes.dictionaryDraft(workspaceId)} label="초안" />
          <SidebarItem
            to={routes.dictionaryHistory(workspaceId)}
            label="리비전 이력"
          />
        </SidebarGroup>
        <div className="mt-auto">
          <SidebarItem
            to={routes.settings(workspaceId)}
            label="설정"
            trailing={
              <span className="text-[10px] font-medium text-text-faint">
                ADMIN+
              </span>
            }
          />
        </div>
      </nav>

      <div className="flex items-center gap-[10px] border-t border-border-soft px-[18px] py-[14px]">
        <Avatar initial="민" tone="accent" size={30} />
        <div className="text-[12.5px] font-semibold">민뱅</div>
        <div className="ml-auto rounded-[5px] bg-accent-bg px-[7px] py-[2px] text-[10px] font-semibold text-accent-strong">
          OWNER
        </div>
      </div>
    </div>
  )
}

function SidebarGroup({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <div>
      <div className="flex items-center justify-between px-2 pb-[6px] pt-1">
        <span className="text-[11px] font-bold uppercase tracking-[0.04em] text-text-quaternary">
          {title}
        </span>
      </div>
      <div className="flex flex-col gap-1">{children}</div>
    </div>
  )
}

function SidebarItem({
  to,
  label,
  trailing,
}: {
  to: string
  label: string
  trailing?: ReactNode
}) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cx(
          'flex items-center gap-2 rounded-sm px-[10px] py-2 text-[13px] font-semibold text-text-secondary hover:bg-bg',
          isActive && 'bg-accent-bg text-accent-strong hover:bg-accent-bg',
        )
      }
    >
      <span className="flex-1">{label}</span>
      {trailing}
    </NavLink>
  )
}
