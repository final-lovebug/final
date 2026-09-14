import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  approveCandidateTerm,
  holdCandidateTerm,
  rejectCandidateTerm,
  mergeCandidateTermAsSynonym,
} from '../api/decideCandidateTerm'

export type CandidateDecision =
  | { kind: 'approve'; candidateId: string; ownerName?: string }
  | { kind: 'hold'; candidateId: string; ownerName?: string }
  | { kind: 'reject'; candidateId: string; rejectReason: string; ownerName?: string }
  | { kind: 'merge'; candidateId: string; mergeTargetTermId: string; ownerName?: string }

// 후보어 판정 4종을 한 mutation으로 묶는다 — 화면에서는 버튼만 다를 뿐 같은 자리에서
// 같은 목록을 무효화한다. 엔드포인트가 갈리는 건 api 쪽에서 처리한다.
export function useDecideCandidateTerm(workspaceId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (decision: CandidateDecision) => {
      switch (decision.kind) {
        case 'approve':
          return approveCandidateTerm(decision.candidateId, decision.ownerName)
        case 'hold':
          return holdCandidateTerm(decision.candidateId, decision.ownerName)
        case 'reject':
          return rejectCandidateTerm(
            decision.candidateId,
            decision.rejectReason,
            decision.ownerName,
          )
        case 'merge':
          return mergeCandidateTermAsSynonym(
            decision.candidateId,
            decision.mergeTargetTermId,
            decision.ownerName,
          )
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dictionary-candidates', workspaceId] })
    },
  })
}
