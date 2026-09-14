import type { ReactNode } from 'react'
import { NavLink, useParams } from 'react-router-dom'
import { Avatar } from '../shared/ui'
import { cx } from '../shared/lib/cx'
import { routes } from '../shared/config/routes'
import { CURRENT_DICTIONARY_REVISION_ID } from '../features/review/model/fixtures'

// ui/main.js renderSidebar() 이식. **(2026-09-10 정정)** Phase 3에서는 "문서" 그룹의
// 초안(reviewDoc)/개정안(reviewThread), "사전집" 그룹의 개정안(revision)이 특정 문서/
// 리비전 id가 있어야 진입 가능하다는 이유로 사이드바에서 뺐었는데, 이건 틀린 판단이었다.
// 필요한 건 "그 id로 바로 가는 링크"가 아니라 "그 단계에 있는 항목들의 목록 화면"이다.
// 문서는 여러 개가 동시에 초안/개정안 단계에 있을 수 있어 목록 화면(DocumentDraftListPage,
// DocumentReviewRequestListPage)을 새로 만들었고, 사전집은 "사전집당 진행 중인 등재
// 흐름은 1개"(docs/DOMAIN.md 정책)라 목록 없이 현재 개정안 하나로 바로 연결한다.
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
          <SidebarItem to={routes.documents(workspaceId)} label="문서" end />
          <SidebarItem to={routes.documentDrafts(workspaceId)} label="초안" />
          <SidebarItem
            to={routes.documentReviewRequests(workspaceId)}
            label="개정안"
          />
        </SidebarGroup>
        <SidebarGroup title="사전집">
          <SidebarItem to={routes.dictionary(workspaceId)} label="사전집" end />
          <SidebarItem to={routes.dictionaryDraft(workspaceId)} label="초안" />
          <SidebarItem
            to={routes.dictionaryRevision(workspaceId, CURRENT_DICTIONARY_REVISION_ID)}
            label="개정안"
          />
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
  end,
}: {
  to: string
  label: string
  trailing?: ReactNode
  /** 정확히 이 경로일 때만 활성 표시. 하위 경로(초안/개정안 등)를 가진 목록형 항목에 준다 —
      안 주면 NavLink가 하위 경로에서도 활성으로 표시해 형제 항목과 동시에 켜져 보인다. */
  end?: boolean
}) {
  return (
    <NavLink
      to={to}
      end={end}
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
