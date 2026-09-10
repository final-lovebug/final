import { delay } from '../../../shared/lib/delay'
import {
  CANDIDATE_TERM_FIXTURES,
  DRAFT_DICTIONARY_ID,
  type CandidateTermListItem,
} from '../model/fixtures'

export interface CreateCandidateTermInput {
  form: string
  type: '동의어' | '동형이의' | '표기 변형'
  ownerId: string
  ownerName: string
}

// 사전집 초안 화면의 "+" 로 직접 등록하는 후보어. 문서에서 자동 추출된 게 아니라 사람이
// 바로 적은 것이라 occurrenceCount/occurredDocumentIds/quotes는 비워둔다.
export async function createCandidateTerm(
  input: CreateCandidateTermInput,
): Promise<CandidateTermListItem> {
  await delay(200)

  const now = new Date().toISOString()
  const created: CandidateTermListItem = {
    id: `cand-${crypto.randomUUID()}`,
    draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: input.form,
    words: [input.form],
    type: input.type,
    occurrenceCount: 0,
    occurredDocumentIds: [],
    proposedDefinition: '',
    ownerName: input.ownerName,
    quotes: [],
    status: 'PENDING',
    createdAt: now,
    createdBy: input.ownerId,
    updatedAt: now,
  }

  CANDIDATE_TERM_FIXTURES.push(created)
  return created
}
