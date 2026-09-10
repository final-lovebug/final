import { delay } from '../../../shared/lib/delay'
import { LABEL_FIXTURES, type LabelListItem } from '../model/labelFixtures'

// Label 엔티티가 docs/DOMAIN.md 기준 아직 미확정이라, 이 mutation도 실제 백엔드가 생기기
// 전까지만 메모리에 반영되는 임시 구현이다.
export async function createLabel(name: string): Promise<LabelListItem> {
  await delay(200)
  const created: LabelListItem = { id: `label-${crypto.randomUUID()}`, name, documentCount: 0 }
  LABEL_FIXTURES.push(created)
  return created
}
