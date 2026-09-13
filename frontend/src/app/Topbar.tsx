import { useMatches, useNavigate } from 'react-router-dom'
import { logout as logoutRequest } from '../features/auth/api/logout'
import { withdrawMember } from '../features/member/api/withdrawMember'
import { useAuthStore } from '../shared/stores/authStore'
import { routes } from '../shared/config/routes'
import { Avatar, Button, Pill } from '../shared/ui'

interface RouteHandle {
  title?: string
}

// ui/main.js renderTopbar() 이식. 제목은 SCREEN_TITLES 같은 별도 표를 다시 만들지 않고,
// 각 라우트 정의(src/app/router.tsx)의 handle.title을 그대로 읽어온다 — 라우트와 제목이
// 어긋날 일이 없다. 알림 패널(bell)과 아바타 스택은 실제 데이터가 붙는 Phase 5~6에서 채운다.
export function Topbar() {
  const matches = useMatches()
  const navigate = useNavigate()
  const clearAuth = useAuthStore((state) => state.logout)
  const handle = [...matches]
    .reverse()
    .map((match) => match.handle as RouteHandle | undefined)
    .find((h) => h?.title)

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
    if (!window.confirm('정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      return
    }

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
        <Button variant="dangerText" size="sm" onClick={handleLogout}>
          로그아웃
        </Button>
        <Button variant="dangerText" size="sm" onClick={handleWithdraw}>
          회원 탈퇴
        </Button>
      </div>
    </div>
  )
}
