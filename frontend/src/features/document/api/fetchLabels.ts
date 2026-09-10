import { delay } from '../../../shared/lib/delay'
import { LABEL_FIXTURES, type LabelListItem } from '../model/labelFixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

// Label 엔티티가 아직 워크스페이스에 속하는지도 확정되지 않아(docs/DOMAIN.md 미확정),
// 지금은 workspaceId로 필터링하지 않고 전체를 반환한다.
export async function fetchLabels(_workspaceId: WorkspaceId): Promise<LabelListItem[]> {
  await delay()
  return LABEL_FIXTURES
}
