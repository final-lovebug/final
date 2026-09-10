import { delay } from '../../../shared/lib/delay'
import { CANDIDATE_TERM_FIXTURES, type CandidateTermListItem } from '../model/fixtures'

export interface UpdateCandidateTermInput {
  candidateId: string
  /** words 중 표준어로 고를 것 */
  selectedWord?: string
  proposedDefinition?: string
}

export async function updateCandidateTerm(
  input: UpdateCandidateTermInput,
): Promise<CandidateTermListItem> {
  await delay(200)

  const target = CANDIDATE_TERM_FIXTURES.find((c) => c.id === input.candidateId)
  if (!target) throw new Error('후보를 찾을 수 없습니다.')

  if (input.selectedWord !== undefined) target.selectedWord = input.selectedWord
  if (input.proposedDefinition !== undefined) {
    target.proposedDefinition = input.proposedDefinition
  }
  target.updatedAt = new Date().toISOString()

  return target
}
