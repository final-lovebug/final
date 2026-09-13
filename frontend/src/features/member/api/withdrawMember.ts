import { httpClient } from '../../../shared/api/httpClient'

// 회원 탈퇴(docs/API.md "회원 탈퇴", DELETE /api/members/me). 로컬 상태만
// WITHDRAWN으로 바꾸는 소프트 삭제다 — 소셜 연동 해제(Unlink)는 별도로 호출하지 않는다
// (docs/DOMAIN.md 인증·회원가입 정책). 성공 시 204.
export async function withdrawMember(): Promise<void> {
  await httpClient.delete<void>('/api/members/me')
}
