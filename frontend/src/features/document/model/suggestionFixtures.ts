import type { SuggestionTerm } from './types'

// ui/data.js REVIEW_SUGGESTIONS/REVIEW_HISTORY 이전. 둘 다 doc-plan 문서 하나를 대상으로 한다
// (ui/main.js FOCUS_DOC와 같은 맥락). draftDocumentId는 아직 별도 DraftDocument 화면이 없어
// 문서 id 기반으로 고정값을 둔다.
export const FOCUS_DRAFT_DOCUMENT_ID = 'draft-doc-plan'

export const SUGGESTION_FIXTURES: SuggestionTerm[] = [
  { id: 's1', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '이용자', suggestionTerm: '구독자(Subscriber)', status: 'PENDING', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's2', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '체험판', suggestionTerm: '무료 체험(Free Trial)', status: 'PENDING', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's3', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '가입완료', suggestionTerm: '구독 활성화(Subscription Activation)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's4', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '오류', suggestionTerm: '결제 실패(Payment Failure)', status: 'PENDING', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's5', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '탈퇴', suggestionTerm: '구독 해지(Subscription Cancellation)', status: 'PENDING', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's6', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '요금제 변경', suggestionTerm: '플랜 업그레이드(Plan Upgrade)', status: 'APPLY_SUGGESTION', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
  { id: 's7', draftDocumentId: FOCUS_DRAFT_DOCUMENT_ID, anchor: { start: 0, end: 0 }, originTerm: '마일리지', suggestionTerm: '리워드 포인트(Reward Point)', status: 'PENDING', createdAt: '2026-08-29T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-29T00:00:00.000Z' },
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

export const SUGGESTION_HISTORY_FIXTURES: SuggestionHistoryItem[] = [
  { original: '이용자', result: '구독자(Subscriber)', action: 'applied' },
  { original: '체험판', result: '무료 체험(Free Trial)', action: 'applied' },
  { original: '가입완료', result: '구독 활성화(Subscription Activation)', action: 'applied' },
  { original: '오류', result: '결제 실패(Payment Failure)', action: 'ignored', reason: '이 문단은 결제 오류가 아니라 일반 오류입니다' },
  { original: '탈퇴', result: '구독 해지(Subscription Cancellation)', action: 'ignored', reason: '사유 없음' },
  { original: '마일리지', result: '리워드 포인트', action: 'manual', manualValue: '포인트' },
]
