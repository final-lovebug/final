import { create } from 'zustand'
import type { Member } from '../../features/member/model'

// 클라이언트 상태(로그인 여부)만 다룬다 — frontend/docs/ARCHITECTURE.md "상태 관리" 규칙:
// 서버에서 오는 데이터를 여기 복제해서 들고 있지 않는다. currentMember는 로그인 응답으로
// 받은 값을 그대로 세션 동안 들고 있는 것이라 예외적으로 둔다(TanStack Query 캐시 대상이 아님).
//
// 실제 Google OAuth 연동은 이번 작업 범위 밖(frontend/docs/SPEC.md "백엔드 연동 범위" 참고).
// 실 연동 시 loginAsMock() 자리를 실제 토큰 교환 로직으로 바꾸면 된다.
interface AuthState {
  isAuthenticated: boolean
  currentMember: Member | null
  loginAsMock: () => void
  logout: () => void
}

const MOCK_MEMBER: Member = {
  id: 'member-mock-owner',
  email: 'idabc1234@gmail.com',
  displayName: '민뱅',
  status: 'ACTIVE',
  role: 'ADMIN',
  provider: 'GOOGLE',
  providerId: 'mock-provider-id',
  createdAt: '2026-08-24T00:00:00.000Z',
  updatedAt: '2026-08-24T00:00:00.000Z',
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  currentMember: null,
  loginAsMock: () => set({ isAuthenticated: true, currentMember: MOCK_MEMBER }),
  logout: () => set({ isAuthenticated: false, currentMember: null }),
}))
