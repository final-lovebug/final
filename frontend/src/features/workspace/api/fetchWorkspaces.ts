import { delay } from '../../../shared/lib/delay'
import { WORKSPACE_FIXTURES } from '../model/fixtures'
import type { Workspace } from '../model/types'

// 목업 구현체. 실제 백엔드 연동 시 이 함수 내부만 fetch/http client 호출로 바꾸면 된다 —
// 반환 타입(Promise<Workspace[]>)을 유지하는 한 features/workspace/hooks와 화면 코드는
// 손댈 필요가 없다 (frontend/docs/ARCHITECTURE.md 의존성 규칙 참고).
export async function fetchWorkspaces(): Promise<Workspace[]> {
  await delay()
  return WORKSPACE_FIXTURES
}
