import { NavLink, Outlet, useParams } from 'react-router-dom'
import { routes } from '../../shared/config/routes'
import { cx } from '../../shared/lib/cx'

// ui/main.js의 renderSettingsScreen(4개 탭: members/ruleset/notif/labels)을
// 중첩 라우트로 옮긴 것. 각 탭은 src/pages/settings/Settings*Page.tsx.
const TABS = [
  { key: 'members', label: '멤버' },
  { key: 'ruleset', label: '승인 규칙' },
  { key: 'notifications', label: '알림' },
  { key: 'labels', label: '라벨' },
] as const

export function SettingsLayout() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()

  const tabHref: Record<(typeof TABS)[number]['key'], string> = {
    members: routes.settingsMembers(workspaceId),
    ruleset: routes.settingsRuleset(workspaceId),
    notifications: routes.settingsNotifications(workspaceId),
    labels: routes.settingsLabels(workspaceId),
  }

  return (
    <div>
      <h1 className="mb-1 font-display text-[19px] font-bold text-text">설정</h1>
      <nav className="mb-[22px] flex gap-[22px] border-b border-border-soft">
        {TABS.map((tab) => (
          <NavLink
            key={tab.key}
            to={tabHref[tab.key]}
            className={({ isActive }) =>
              cx(
                'border-b-2 border-transparent pb-[9px] text-[13px] font-semibold text-text-quaternary',
                isActive && 'border-accent text-text',
              )
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}
