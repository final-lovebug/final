import { useQuery } from '@tanstack/react-query'
import { fetchDictionaryDraft } from '../api/fetchDictionaryDraft'
import type { WorkspaceId } from '../../../shared/types/ids'

/** 사전집 초안 한 벌(초안 정보 + 작성자 이름 + 후보어 + 리뷰 요청 준비 여부). */
export function useDictionaryDraft(workspaceId: WorkspaceId) {
  return useQuery({
    queryKey: ['dictionary-draft', workspaceId],
    queryFn: () => fetchDictionaryDraft(workspaceId),
    enabled: workspaceId !== '',
  })
}
