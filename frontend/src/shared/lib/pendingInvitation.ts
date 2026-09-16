// 로그아웃 상태에서 초대 링크를 열었을 때 토큰을 잠깐 들고 있는 자리.
//
// 로그인은 Google로 전체 페이지 리다이렉트했다가 /oauth/callback으로 돌아오는 흐름이라
// (LoginPage → OAuthCallbackPage) 리다이렉트 목적지를 메모리에 둘 수 없다. 탭을 닫으면
// 같이 사라지도록 localStorage가 아니라 sessionStorage를 쓴다.
//
// 사파리 프라이빗 모드 등에서 sessionStorage 접근이 던질 수 있으므로 전부 감싼다 —
// 실패하면 "로그인 후 워크스페이스 목록으로"라는 기존 동작으로 조용히 되돌아간다.

const STORAGE_KEY = 'pending-invitation-token'

export function savePendingInvitationToken(token: string): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, token)
  } catch {
    // 저장에 실패하면 로그인 후 초대 링크를 다시 열어야 한다.
  }
}

/** 읽으면서 지운다 — 한 번 쓰고 마는 값이라 남겨두면 다음 로그인까지 따라간다. */
export function takePendingInvitationToken(): string | null {
  try {
    const token = sessionStorage.getItem(STORAGE_KEY)
    sessionStorage.removeItem(STORAGE_KEY)
    return token
  } catch {
    return null
  }
}
