import { useQuery } from '@tanstack/react-query'
import { fetchDictionary } from '../api/fetchDictionary'
import type { WorkspaceId } from '../../../shared/types/ids'

/**
 * 사전집 한 벌. `versionNo`를 주면 그 보관 버전을, 안 주면 활성 사전집을 읽는다.
 *
 * 사전집이 아직 없는 워크스페이스는 404를 던진다 — 첫 발행 전의 정상 상태이므로
 * 호출하는 쪽이 "없음"으로 다룬다(docs/API.md «알아 둘 것 셋»).
 */
export function useDictionary(workspaceId: WorkspaceId, versionNo?: number) {
  return useQuery({
    queryKey: ['dictionary', workspaceId, versionNo ?? 'active'],
    queryFn: () => fetchDictionary(workspaceId, versionNo),
    enabled: workspaceId !== '',
  })
}
