import { useEffect, useRef, useState, type ReactNode } from 'react'
import { NavLink, useNavigate, useParams } from 'react-router-dom'
import { Avatar, LogoMark } from '../shared/ui'
import { cx } from '../shared/lib/cx'
import { routes } from '../shared/config/routes'
import { useAuthStore } from '../shared/stores/authStore'
import { useWorkspace } from '../features/workspace/hooks/useWorkspace'
import { logout as logoutRequest } from '../features/auth/api/logout'
import { withdrawMember } from '../features/member/api/withdrawMember'
import { CURRENT_DICTIONARY_REVISION_ID } from '../features/review/model/fixtures'

// ui/main.js renderSidebar() 이식.
//
// **(2026-09-10 정정)** Phase 3에서는 "문서" 그룹의 초안/개정안, "사전집" 그룹의 개정안이
// 특정 문서/리비전 id가 있어야 진입 가능하다는 이유로 사이드바에서 뺐었는데, 이건 틀린
// 판단이었다. 필요한 건 "그 id로 바로 가는 링크"가 아니라 "그 단계에 있는 항목들의 목록
// 화면"이다. 문서는 여러 개가 동시에 초안/개정안 단계에 있을 수 있어 목록 화면을 새로
// 만들었고, 사전집은 "사전집당 진행 중인 등재 흐름은 1개"(docs/DOMAIN.md)라 목록 없이
// `'current'` sentinel로 바로 연결한다.
//
// **(2026-09-14 디자인 정합)** 프로토타입에 있던 접이식 그룹(chevron)을 되살리고,
// 머리말의 워크스페이스 이름과 발치의 사용자·권한을 실제 데이터로 채웠다 — 그동안
// "워크스페이스"·"민뱅"·"OWNER"가 하드코딩돼 있었다.
export function Sidebar() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const { data: workspace } = useWorkspace(workspaceId)
  const currentMember = useAuthStore((state) => state.currentMember)

  return (
    <div className="flex min-h-screen w-[248px] shrink-0 flex-col border-r border-border-soft bg-surface">
      <div className="flex items-center justify-between border-b border-border-soft px-[18px] pb-4 pt-[18px]">
        <div className="flex min-w-0 items-center gap-2">
          <LogoMark size={24}>{workspace?.name.charAt(0) ?? 'U'}</LogoMark>
          <span className="truncate font-display text-[13.5px] font-bold">
            {workspace?.name ?? '워크스페이스'}
          </span>
        </div>
      </div>

      <nav className="flex flex-1 flex-col gap-4 overflow-auto px-3 py-[14px]">
        <SidebarGroup title="문서">
          <SidebarItem to={routes.documents(workspaceId)} label="문서" end />
          <SidebarItem to={routes.documentDrafts(workspaceId)} label="초안" />
          <SidebarItem to={routes.documentReviewRequests(workspaceId)} label="개정안" />
        </SidebarGroup>

        <SidebarGroup title="사전집">
          <SidebarItem to={routes.dictionary(workspaceId)} label="사전집" end />
          <SidebarItem to={routes.dictionaryDraft(workspaceId)} label="초안" />
          <SidebarItem
            to={routes.dictionaryRevision(workspaceId, CURRENT_DICTIONARY_REVISION_ID)}
            label="개정안"
          />
          <SidebarItem to={routes.dictionaryHistory(workspaceId)} label="리비전 이력" />
        </SidebarGroup>

        <div className="mt-auto">
          <SidebarItem
            to={routes.settings(workspaceId)}
            label="설정"
            trailing={<span className="text-[10px] font-medium text-text-faint">ADMIN+</span>}
          />
        </div>
      </nav>

      <SidebarFooter
        displayName={currentMember?.displayName ?? '—'}
        permission={workspace?.myPermission}
      />
    </div>
  )
}

/**
 * 사용자 카드 + 계정 메뉴.
 *
 * 프로토타입 탑바에는 로그아웃·탈퇴가 없었다(목업이라 세션 개념 자체가 없었다). 실제로는
 * 필요하므로 여기로 옮겼다 — 탑바 오른쪽을 ui대로 유지하면서 계정 동작이 사용자 이름 옆에
 * 붙는 편이 찾기도 쉽다.
 */
function SidebarFooter({
  displayName,
  permission,
}: {
  displayName: string
  permission?: string
}) {
  const navigate = useNavigate()
  const clearAuth = useAuthStore((state) => state.logout)
  const [menuOpen, setMenuOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    function handleClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  async function handleLogout() {
    try {
      await logoutRequest()
    } catch {
      // 서버 로그아웃 요청이 실패해도(네트워크 오류 등) 로컬 세션은 정리한다 — 다시
      // 로그인하면 되고, 어차피 서버 refresh token은 7일 뒤 만료된다.
    } finally {
      clearAuth()
      navigate(routes.login(), { replace: true })
    }
  }

  async function handleWithdraw() {
    if (!window.confirm('정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) return

    try {
      await withdrawMember()
    } catch {
      window.alert('탈퇴 처리 중 문제가 발생했습니다. 다시 시도해 주세요.')
      return
    }

    try {
      await logoutRequest()
    } catch {
      // 탈퇴 자체는 이미 끝났으니, 로그아웃(refresh token 폐기) 실패는 무시하고 진행한다.
    } finally {
      clearAuth()
      navigate(routes.login(), { replace: true })
    }
  }

  return (
    <div ref={rootRef} className="relative border-t border-border-soft">
      {menuOpen && (
        <div className="absolute bottom-[calc(100%-4px)] left-3 right-3 overflow-hidden rounded-md border border-border bg-surface shadow-pop">
          <button
            type="button"
            onClick={handleLogout}
            className="block w-full cursor-pointer px-3 py-[10px] text-left text-[12.5px] font-semibold text-text-secondary hover:bg-bg"
          >
            로그아웃
          </button>
          <button
            type="button"
            onClick={handleWithdraw}
            className="block w-full cursor-pointer border-t border-border-soft px-3 py-[10px] text-left text-[12.5px] font-semibold text-danger hover:bg-bg"
          >
            회원 탈퇴
          </button>
        </div>
      )}

      <button
        type="button"
        onClick={() => setMenuOpen((open) => !open)}
        aria-expanded={menuOpen}
        className="flex w-full cursor-pointer items-center gap-[10px] px-[18px] py-[14px] text-left hover:bg-bg"
      >
        <Avatar initial={displayName.charAt(0)} tone="accent" size={30} />
        <span className="min-w-0 flex-1 truncate text-[12.5px] font-semibold">
          {displayName}
        </span>
        {permission && (
          <span className="rounded-[5px] bg-accent-bg px-[7px] py-[2px] text-[10px] font-semibold text-accent-strong">
            {permission}
          </span>
        )}
      </button>
    </div>
  )
}

/** ui의 `.sidebar-group-header` — 제목을 누르면 접힌다. */
function SidebarGroup({ title, children }: { title: string; children: ReactNode }) {
  const [open, setOpen] = useState(true)

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="flex w-full cursor-pointer select-none items-center justify-between px-2 pb-[6px] pt-1"
        aria-expanded={open}
      >
        <span className="text-[11px] font-bold uppercase tracking-[0.04em] text-text-quaternary">
          {title}
        </span>
        <span
          className={cx(
            'inline-block text-[9px] text-text-faint transition-transform',
            !open && '-rotate-90',
          )}
        >
          ▾
        </span>
      </button>
      {open && <div className="flex flex-col gap-1">{children}</div>}
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
