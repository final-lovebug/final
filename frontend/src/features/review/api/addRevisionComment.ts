import { delay } from '../../../shared/lib/delay'
import { DICTIONARY_REVISION_THREAD, type RevisionCommentListItem } from '../model/fixtures'

export interface AddRevisionCommentInput {
  revisionId: string
  authorName: string
  authorInitial: string
  text: string
}

// 지금은 리비전 코멘트 스레드가 "주문" 용어 하나에 대한 것으로 고정돼 있어(원본 ui/ 프로토타입과
// 동일한 범위), revisionId로 구분하지 않고 하나의 스레드에 그대로 추가한다.
export async function addRevisionComment(
  input: AddRevisionCommentInput,
): Promise<RevisionCommentListItem> {
  await delay(200)

  const created: RevisionCommentListItem = {
    initial: input.authorInitial,
    name: input.authorName,
    tone: 'accent',
    time: '방금 전',
    text: input.text,
  }

  DICTIONARY_REVISION_THREAD.push(created)
  return created
}
