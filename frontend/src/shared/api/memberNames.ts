import { httpClient } from './httpClient'

interface MemberSummaryApiItem {
  memberId: number
  displayName: string
  email: string
}

/**
 * 회원 id → 표시 이름을 한 번에 해석한다(`GET /api/members?ids=1,2,3`, T-INT-18).
 *
 * 여러 도메인(문서 작성자, 후보어 등록자 등)이 같은 일을 하므로 여기 둔다 —
 * features끼리 서로 참조하지 않기 위한 자리이기도 하다(frontend/docs/ARCHITECTURE.md).
 *
 * **없는 id는 조용히 빠진다**(탈퇴 등). 호출하는 쪽이 fallback("—")을 정한다.
 * 목록 화면에서 행마다 단건 조회하면 N+1이 되므로 항상 이 배치 조회를 쓴다.
 */
export async function fetchMemberNames(
  memberIds: (number | null | undefined)[],
): Promise<Map<number, string>> {
  const ids = [...new Set(memberIds.filter((id): id is number => id !== null && id !== undefined))]
  if (ids.length === 0) return new Map()

  const summaries = await httpClient.get<MemberSummaryApiItem[]>(`/api/members?ids=${ids.join(',')}`)
  return new Map(summaries.map((summary) => [summary.memberId, summary.displayName]))
}
