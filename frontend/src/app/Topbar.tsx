import { useMatches } from 'react-router-dom'
import { Avatar, Pill } from '../shared/ui'

interface RouteHandle {
  title?: string
}

// ui/main.js renderTopbar() 이식. 제목은 SCREEN_TITLES 같은 별도 표를 다시 만들지 않고,
// 각 라우트 정의(src/app/router.tsx)의 handle.title을 그대로 읽어온다 — 라우트와 제목이
// 어긋날 일이 없다. 알림 패널(bell)과 아바타 스택은 실제 데이터가 붙는 Phase 5~6에서 채운다.
export function Topbar() {
  const matches = useMatches()
  const handle = [...matches]
    .reverse()
    .map((match) => match.handle as RouteHandle | undefined)
    .find((h) => h?.title)

  return (
    <div className="flex h-16 shrink-0 items-center justify-between border-b border-border-soft bg-surface px-7">
      <div className="font-display text-[17px] font-bold">
        {handle?.title ?? '문서'}
      </div>
      <div className="flex items-center gap-4">
        <Pill tone="accent">사전집 r7</Pill>
        <div className="flex">
          <Avatar initial="" tone="accent" size={26} className="border-2 border-surface" />
          <Avatar initial="" tone="warn" size={26} className="-ml-2 border-2 border-surface" />
          <Avatar initial="" tone="success" size={26} className="-ml-2 border-2 border-surface" />
        </div>
        <button
          type="button"
          className="relative flex h-8 w-8 items-center justify-center rounded-sm hover:bg-bg"
          aria-label="알림"
        >
          <span className="block h-4 w-4 rounded-[50%_50%_50%_4px] border-[1.6px] border-text-tertiary" />
          <span className="absolute right-[7px] top-[6px] h-[7px] w-[7px] rounded-full border-[1.5px] border-surface bg-red-500" />
        </button>
      </div>
    </div>
  )
}
