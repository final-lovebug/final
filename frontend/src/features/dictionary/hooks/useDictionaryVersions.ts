import { useQuery } from '@tanstack/react-query'
import {
  fetchDictionaryVersionDiff,
  fetchDictionaryVersions,
} from '../api/fetchDictionaryVersions'
import type { WorkspaceId } from '../../../shared/types/ids'

export function useDictionaryVersions(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['dictionary-versions', workspaceId],
    queryFn: () => fetchDictionaryVersions(workspaceId),
    enabled: workspaceId !== '',
  })
}

/** 두 버전이 정해졌을 때만 돈다 — 버전이 하나뿐인 워크스페이스에서는 비교할 것이 없다. */
export function useDictionaryVersionDiff(
  workspaceId: WorkspaceId,
  fromVersionNo: number | null,
  toVersionNo: number | null,
) {
  return useQuery({
    queryKey: ['dictionary-version-diff', workspaceId, fromVersionNo, toVersionNo],
    queryFn: () => fetchDictionaryVersionDiff(workspaceId, fromVersionNo!, toVersionNo!),
    enabled: workspaceId !== '' && fromVersionNo !== null && toVersionNo !== null,
  })
}
