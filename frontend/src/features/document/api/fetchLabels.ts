import { httpClient } from '../../../shared/api/httpClient'
import type { LabelListItem } from '../model/labelFixtures'
import type { WorkspaceId } from '../../../shared/types/ids'

interface LabelApiItem {
  labelId: number
  name: string
}

// 실제 백엔드 연동(docs/API.md "워크스페이스 라벨 목록 조회", GET /api/workspaces/
// {workspaceId}/labels). 배열 그대로 반환(페이징 없음). 라벨은 문서 생성/수정 시에만
// 자동 생기므로 독립 생성 API는 없다 — `documentCount`도 응답에 없어 0으로 둔다
// (T-INT-10, 2026-09-14 결정: 생성은 목업 유지, 조회만 실연동).
export async function fetchLabels(workspaceId: WorkspaceId): Promise<LabelListItem[]> {
  const items = await httpClient.get<LabelApiItem[]>(`/api/workspaces/${workspaceId}/labels`)
  return items.map((label) => ({
    id: String(label.labelId),
    name: label.name,
    documentCount: 0,
  }))
}
