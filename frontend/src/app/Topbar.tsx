import { useEffect, useRef, useState } from 'react'
import { useMatches, useParams } from 'react-router-dom'
import { Avatar, Pill } from '../shared/ui'
import type { AvatarTone } from '../shared/ui'
import { NotificationPanel } from './NotificationPanel'
import { useUnreadCount } from '../features/notification/hooks/useUnreadCount'
import { useWorkspaceMembers } from '../features/member/hooks/useWorkspaceMembers'
import { useDictionary } from '../features/dictionary/hooks/useDictionary'

interface RouteHandle {
  title?: string
}

/** ui의 avatar-stack은 색이 돌아가며 섞인다. 백엔드에 tone 개념이 없어 id로 정한다. */
const AVATAR_TONES: AvatarTone[] = ['accent', 'warn', 'success', 'danger', 'neutral']

function toneFor(memberId: string): AvatarTone {
  let hash = 0
  for (const char of memberId) hash = (hash + char.charCodeAt(0)) % AVATAR_TONES.length
  return AVATAR_TONES[hash]
}

// ui/main.js renderTopbar() 이식. 제목은 SCREEN_TITLES 같은 별도 표를 다시 만들지 않고,
// 각 라우트 정의(src/app/router.tsx)의 handle.title을 그대로 읽어온다 — 라우트와 제목이
// 어긋날 일이 없다.
//
// **(2026-09-14 디자인 정합)** 사전집 배지의 "r7"과 아바타 스택 3개가 하드코딩돼 있던 것을
// 실 데이터로 바꿨다. 로그아웃·회원 탈퇴 버튼은 프로토타입 탑바에 없던 것이라 사이드바
// 발치의 사용자 메뉴로 옮겼다 — 탑바 오른쪽은 ui대로 사전집 배지·참여자·알림 셋이다.
export function Topbar() {
  const { workspaceId = '' } = useParams<{ workspaceId: string }>()
  const matches = useMatches()
  const handle = [...matches]
    .reverse()
    .map((match) => match.handle as RouteHandle | undefined)
    .find((h) => h?.title)

  const { data: unreadCount } = useUnreadCount(workspaceId)
  const { data: members } = useWorkspaceMembers(workspaceId)
  // 첫 발행 전 워크스페이스는 사전집이 없어 404다 — 정상 상태이므로 배지를 감추기만 한다.
  const { data: activeDictionary } = useDictionary(workspaceId)

  const [panelOpen, setPanelOpen] = useState(false)
  const bellRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!panelOpen) return
    function handleClickOutside(event: MouseEvent) {
      if (bellRef.current && !bellRef.current.contains(event.target as Node)) {
        setPanelOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [panelOpen])

  const shownMembers = (members ?? []).slice(0, 5)
  const overflowCount = (members?.length ?? 0) - shownMembers.length

  return (
    <div className="relative flex h-16 shrink-0 items-center justify-between border-b border-border-soft bg-surface px-7">
      <div className="font-display text-[17px] font-bold">{handle?.title ?? '문서'}</div>

      <div className="flex items-center gap-4">
        {activeDictionary && (
          <Pill tone="accent">사전집 r{activeDictionary.dictionary.currentVersionNo}</Pill>
        )}

        <div className="flex">
          {shownMembers.map((member, index) => (
            <Avatar
              key={member.id}
              initial={member.initial}
              tone={toneFor(member.id)}
              size={26}
              title={member.name}
              className={index > 0 ? '-ml-2 border-2 border-surface' : 'border-2 border-surface'}
            />
          ))}
          {overflowCount > 0 && (
            <Avatar
              initial={`+${overflowCount}`}
              tone="neutral"
              size={26}
              className="-ml-2 border-2 border-surface"
            />
          )}
        </div>

        <div ref={bellRef} className="relative">
          <button
            type="button"
            className="relative flex h-8 w-8 cursor-pointer items-center justify-center rounded-sm hover:bg-bg"
            aria-label="알림"
            onClick={() => setPanelOpen((open) => !open)}
          >
            <span className="block h-4 w-4 rounded-[50%_50%_50%_4px] border-[1.6px] border-text-tertiary" />
            {Boolean(unreadCount) && (
              <span className="absolute right-[7px] top-[6px] h-[7px] w-[7px] rounded-full border-[1.5px] border-surface bg-[#EF4444]" />
            )}
          </button>
          {panelOpen && (
            <NotificationPanel workspaceId={workspaceId} onNavigate={() => setPanelOpen(false)} />
          )}
        </div>
      </div>
    </div>
  )
}
