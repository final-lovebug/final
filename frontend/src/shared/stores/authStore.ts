import { create } from 'zustand'
import type { Member } from '../../features/member/model'

// 클라이언트 상태만 다룬다 — frontend/docs/ARCHITECTURE.md "상태 관리" 규칙: 서버에서 오는
// 데이터를 여기 복제해서 들고 있지 않는다. currentMember는 로그인 시 서버에서 받은 값을
// 세션 동안 그대로 들고 있는 예외(TanStack Query 캐시 대상이 아님).
//
// accessToken은 XSS 노출 방지를 위해 메모리(Zustand)에만 두고 localStorage에 저장하지
// 않는다(frontend/CLAUDE.md 백엔드 연동 체크리스트 3번). refresh token은 HttpOnly 쿠키라
// 애초에 여기서 다루지 않는다.
interface AuthState {
  isAuthenticated: boolean
  accessToken: string | null
  currentMember: Member | null
  /** OAuthCallbackPage가 토큰 교환 성공 직후 호출한다. */
  login: (accessToken: string) => void
  /** shared/api/httpClient가 access token 재발급 성공 시 호출한다. */
  setAccessToken: (accessToken: string) => void
  /** OAuthCallbackPage가 GET /api/members/me 응답으로 채운다. */
  setCurrentMember: (member: Member) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  accessToken: null,
  currentMember: null,
  login: (accessToken) => set({ isAuthenticated: true, accessToken }),
  setAccessToken: (accessToken) => set({ accessToken }),
  setCurrentMember: (member) => set({ currentMember: member }),
  logout: () => set({ isAuthenticated: false, accessToken: null, currentMember: null }),
}))
