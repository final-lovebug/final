import { delay } from '../../../shared/lib/delay'
import { CANDIDATE_TERM_FIXTURES, type CandidateTermListItem } from '../model/fixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

// 지금은 워크스페이스당 진행 중인 사전 초안이 1개뿐이라는 docs/DOMAIN.md 정책
// ("사전집당 진행 중인 등재 흐름은 1개")을 그대로 반영해 workspaceId로만 조회한다.
export async function fetchCandidates(
  workspaceId: WorkspaceId,
): Promise<CandidateTermListItem[]> {
  await delay()
  return workspaceId === 'potenup_be' ? CANDIDATE_TERM_FIXTURES : []
}
