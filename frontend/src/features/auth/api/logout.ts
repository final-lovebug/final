import { httpClient } from '../../../shared/api/httpClient'

// docs/API.md "로그아웃"(POST /api/auth/logout). refresh token 쿠키를 서버에서 폐기한다.
// 쿠키가 없어도 204(멱등)이므로 이미 로그아웃된 상태에서 불러도 안전하다.
export async function logout(): Promise<void> {
  await httpClient.post<void>('/api/auth/logout')
}
