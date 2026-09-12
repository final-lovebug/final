import type { SuggestionTerm } from './types'

// ui/data.js REVIEW_SUGGESTIONS/REVIEW_HISTORY 이전. 원본은 doc-plan 문서 하나만 다뤘지만
// (ui/main.js FOCUS_DOC), 목록 화면(초안/개정안)이 의미 있으려면 문서가 최소 2개는
// 서로 다른 단계에 있어야 해서 doc-retention을 "아직 초안 진행 중" 상태로 추가했다.
//
// - doc-plan: 제안 7건 전부 처리 완료 → "초안 완료 → 리뷰 요청"까지 끝나 개정안 단계로
//   전이된 것으로 취급한다 (features/review/model/reviewRequestFixtures.ts에서 이어짐).
// - doc-retention: 제안 3건 모두 PENDING → 아직 초안 단계.
export const FOCUS_DRAFT_DOCUMENT_ID = 'draft-doc-plan'
export const RETENTION_DRAFT_DOCUMENT_ID = 'draft-doc-retention'

export const SUGGESTION_FIXTURES: SuggestionTerm[] = [
  { id: 's1', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '이용자', suggestionTerm: '구독자(Subscriber)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's2', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '체험판', suggestionTerm: '무료 체험(Free Trial)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's3', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '가입완료', suggestionTerm: '구독 활성화(Subscription Activation)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's4', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '오류', suggestionTerm: '결제 실패(Payment Failure)', status: 'KEEP_ORIGINAL', rejectReason: '이 문단은 결제 오류가 아니라 일반 오류입니다', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's5', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '탈퇴', suggestionTerm: '구독 해지(Subscription Cancellation)', status: 'KEEP_ORIGINAL', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's6', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '요금제 변경', suggestionTerm: '플랜 업그레이드(Plan Upgrade)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's7', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '마일리지', suggestionTerm: '리워드 포인트(Reward Point)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },

  { id: 'r1', draftDocumentId: RETENTION_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '이용 보류', suggestionTerm: '휴면 전환', status: 'PENDING', createdAt: '2026-09-05T00:00:00.000Z', createdBy: 'member-mock-han-sohee', updatedAt: '2026-09-05T00:00:00.000Z' },
  { id: 'r2', draftDocumentId: RETENTION_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '해지', suggestionTerm: '구독 해지(Subscription Cancellation)', status: 'PENDING', createdAt: '2026-09-05T00:00:00.000Z', createdBy: 'member-mock-han-sohee', updatedAt: '2026-09-05T00:00:00.000Z' },
  { id: 'r3', draftDocumentId: RETENTION_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '재구독', suggestionTerm: '구독 재활성화(Resubscription)', status: 'PENDING', createdAt: '2026-09-05T00:00:00.000Z', createdBy: 'member-mock-han-sohee', updatedAt: '2026-09-05T00:00:00.000Z' },
]

// "처리 내역" 탭/화면 전용 뷰. SuggestionTerm.status만으로는 "직접 입력값"·"무시 사유"를
// 표현할 수 없어(그 둘은 도메인 표에도 없는 화면 전용 서술) 별도 타입으로 뒀다.
export interface SuggestionHistoryItem {
  original: string
  result: string
  action: 'applied' | 'ignored' | 'manual'
  reason?: string
  manualValue?: string
}

export const SUGGESTION_HISTORY_FIXTURES: Record<string, SuggestionHistoryItem[]> = {
  'doc-plan': [
    { original: '이용자', result: '구독자(Subscriber)', action: 'applied' },
    { original: '체험판', result: '무료 체험(Free Trial)', action: 'applied' },
    { original: '가입완료', result: '구독 활성화(Subscription Activation)', action: 'applied' },
    { original: '오류', result: '결제 실패(Payment Failure)', action: 'ignored', reason: '이 문단은 결제 오류가 아니라 일반 오류입니다' },
    { original: '탈퇴', result: '구독 해지(Subscription Cancellation)', action: 'ignored', reason: '사유 없음' },
    { original: '마일리지', result: '리워드 포인트', action: 'manual', manualValue: '포인트' },
  ],
}

// DocumentReviewPage가 문서별로 본문을 조립할 때 쓰는 조각. 실제로는 문서 본문(content)에서
// 정확한 위치(anchor)를 찾아 제안을 얹어야 하지만, 대조(DictionaryContrast)가 MVP1에서
// 빠져 있어 지금은 문서별로 고정된 조각을 그대로 쓴다.
export type SuggestionParagraphPart = { text: string } | { suggestionId: string }

export const SUGGESTION_PARAGRAPHS: Record<string, SuggestionParagraphPart[]> = {
  'doc-plan': [
    { text: '새로운 신규 획득 정책 2026에 따라 ' },
    { suggestionId: 's1' },
    { text: '를 대상으로 다음과 같은 비즈니스 규칙을 적용합니다. 첫 번째 규칙은 ' },
    { suggestionId: 's2' },
    { text: ' 14일이 충분한 시점을 기준으로 합니다. 이때 고객의 상태는 ' },
    { suggestionId: 's3' },
    { text: '로 자동 전환되어야 합니다. 만약 이 과정에서 ' },
    { suggestionId: 's4' },
    { text: '가 발생할 경우, 시스템은 즉시 ' },
    { suggestionId: 's5' },
    { text: ' 처리를 진행하고 안내 메일을 발송해야 합니다. 두 번째 규칙은 기존 고객의 상향 가입을 유도하기 위한 정책입니다. ' },
    { suggestionId: 's6' },
    { text: '을 진행할 경우, 시스템은 혜택의 일환으로 ' },
    { suggestionId: 's7' },
    { text: ' 5,000원을 지급합니다.' },
  ],
  'doc-retention': [
    { text: '최근 리텐션 개선 프로젝트에 따라 ' },
    { suggestionId: 'r1' },
    { text: '된 계정은 활성 일수 계산에서 제외합니다. 가입일로부터 30일 이내 ' },
    { suggestionId: 'r2' },
    { text: '한 인원은 조기 이탈로 분류하며, ' },
    { suggestionId: 'r3' },
    { text: '한 경우 최초 가입일 기준으로 활성 일수를 재계산합니다.' },
  ],
}
