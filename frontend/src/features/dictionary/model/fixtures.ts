import type { CandidateTerm, Dictionary, DraftDictionary, Term } from './types'

// ui/data.js TERMS/CANDIDATES 이전. potenup_be 워크스페이스의 사전집 하나만 다룬다
// (원본 프로토타입도 사전집 화면 전체가 이 한 사전집 기준이었다).
export const DICTIONARY_FIXTURE: Dictionary = {
  id: 'dict-potenup-be',
  workspaceId: 'potenup_be',
  name: 'POTENUP_BE 사전집',
  currentVersionNo: 7,
  status: 'ACTIVE',
  createdAt: '2026-08-25T00:00:00.000Z',
  createdBy: 'member-mock-owner',
  updatedAt: '2026-09-01T00:00:00.000Z',
}

// ui/data.js TERMS의 "한글 (English)" 표기를 preferredForm/englishName으로 분리했다.
export const TERM_FIXTURES: Term[] = [
  { id: 'term-subscriber', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '구독자', englishName: 'Subscriber', definition: '서비스를 이용 중인 사용자', createdAt: '2026-08-25T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-30T00:00:00.000Z' },
  { id: 'term-free-trial', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '무료 체험', englishName: 'Free Trial', definition: '과금 전 제공되는 시험 이용 기간', createdAt: '2026-08-18T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-18T00:00:00.000Z' },
  { id: 'term-subscription-activation', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '구독 활성화', englishName: 'Subscription Activation', definition: '유료 구독이 정상 등록된 상태', createdAt: '2026-08-18T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-18T00:00:00.000Z' },
  { id: 'term-subscription-cancellation', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '구독 해지', englishName: 'Subscription Cancellation', definition: '구독 관계가 종료된 상태', createdAt: '2026-08-15T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-15T00:00:00.000Z' },
  { id: 'term-plan-upgrade', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '플랜 업그레이드', englishName: 'Plan Upgrade', definition: '상위 요금제로 전환', createdAt: '2026-08-12T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-12T00:00:00.000Z' },
  { id: 'term-content-view', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '콘텐츠 조회', englishName: 'Content View', definition: '콘텐츠 상세 페이지 열람', createdAt: '2026-08-10T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-10T00:00:00.000Z' },
  { id: 'term-reward-point', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '리워드 포인트', englishName: 'Reward Point', definition: '적립·사용 가능한 보상 포인트', createdAt: '2026-08-09T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-09T00:00:00.000Z' },
  { id: 'term-promotion', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '프로모션', englishName: 'Promotion', definition: '한정 기간 제공되는 할인·혜택', createdAt: '2026-08-09T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-09T00:00:00.000Z' },
  { id: 'term-payment-failure', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '결제 실패', englishName: 'Payment Failure', definition: '결제 승인이 거절되거나 처리되지 않은 상태', createdAt: '2026-08-05T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-05T00:00:00.000Z' },
  { id: 'term-active-days', dictionaryId: DICTIONARY_FIXTURE.id, preferredForm: '활성 일수', englishName: 'Active Days', definition: '최근 기간 내 실제 로그인·이용한 날수', createdAt: '2026-08-31T00:00:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-08-31T00:00:00.000Z' },
]

/** 강조해서 보여줄 용어. ui/data.js TERM_IN_FOCUS 이전. */
export const TERM_IN_FOCUS_ID = 'term-subscriber'

/**
 * ui/data.js CANDIDATES 이전. CandidateTerm 도메인 타입에는 없는 화면 전용 정보
 * (표기 변형 묶음, 분류, 담당자 표시 이름, 근거 인용문)를 얹었다 — DocumentListItem과 같은 패턴.
 */
export interface CandidateQuote {
  text: string
  source: string
  split: boolean
}

export interface CandidateTermListItem extends CandidateTerm {
  /** 동의어로 묶인 표기 변형들. words[0]이 CandidateTerm.form과 같다 */
  words: string[]
  /** 동의어 / 동형이의 / 표기 변형 */
  type: '동의어' | '동형이의' | '표기 변형'
  ownerName: string
  quotes: CandidateQuote[]
  splitNote?: string
  /** words 중 표준어로 고른 것. 없으면 words[0]을 기본값으로 쓴다(화면에서 fallback 처리). */
  selectedWord?: string
}

export const DRAFT_DICTIONARY_ID = 'draft-dict-potenup-be'

export const CANDIDATE_TERM_FIXTURES: CandidateTermListItem[] = [
  {
    id: 'cand-0', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '이용 보류', words: ['이용 보류', '휴면 전환', 'suspend_account'], type: '동의어',
    occurrenceCount: 23, occurredDocumentIds: ['doc-plan', 'doc-member'],
    proposedDefinition: '이용자가 일정 기간 서비스를 쓰지 않도록 계정을 잠시 멈춘 상태.',
    ownerName: '김개발', status: 'PENDING',
    quotes: [{ text: '이용을 보류 처리하면 로그인이 제한된다', source: '커머스기획안.md · 4절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-1', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '주문', words: ['주문'], type: '동형이의',
    occurrenceCount: 47, occurredDocumentIds: ['doc-plan', 'doc-price'],
    proposedDefinition: '결제가 완료되어 확정된 구매 건. 장바구니에 담긴 상태는 주문이 아니다.',
    ownerName: '미지정', status: 'PENDING',
    quotes: [
      { text: '장바구니에 담으면 주문이 생성된다', source: '기획팀 · 커머스기획안.md · 3절 2문단', split: false },
      { text: '결제 승인 후 주문이 확정된다', source: '개발팀 · 결제API명세v2.md · 2.1 상태 전이', split: true },
    ],
    splitNote: '같은 말인데 기획팀은 장바구니 시점, 개발팀은 결제 완료 시점으로 쓰고 있습니다',
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-2', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '결제 취소', words: ['결제 취소', '매출 차감', 'pg_refund'], type: '동의어',
    occurrenceCount: 19, occurredDocumentIds: ['doc-payment'],
    proposedDefinition: '이미 승인된 결제를 취소하고 금액을 되돌리는 처리.',
    ownerName: '이백엔드', status: 'PENDING',
    quotes: [{ text: '결제 취소 시 매출이 차감 처리된다', source: '결제API명세v2.md · 3절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-3', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '큐레이션', words: ['큐레이션', '개인화 타겟팅', 'get_suggested_list'], type: '동의어',
    occurrenceCount: 14, occurredDocumentIds: ['doc-campaign'],
    proposedDefinition: '이용자 행동 데이터를 기반으로 콘텐츠를 추천하는 기능.',
    ownerName: '미지정', status: 'PENDING',
    quotes: [{ text: '큐레이션 결과는 최근 조회 기록을 반영한다', source: '캠페인실행안.md · 2절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-4', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '별점 리뷰', words: ['별점 리뷰', '유저 반응(UGC)', 'update_star_score'], type: '동의어',
    occurrenceCount: 11, occurredDocumentIds: ['doc-retention'],
    proposedDefinition: '이용자가 콘텐츠에 남기는 평점 및 후기.',
    ownerName: '미지정', status: 'PENDING',
    quotes: [{ text: '별점 리뷰는 5점 만점으로 집계된다', source: '리텐션지표정의서.md · 5절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-5', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '로그인', words: ['로그인', '로그 인', 'login'], type: '표기 변형',
    occurrenceCount: 12, occurredDocumentIds: ['doc-member'],
    proposedDefinition: '계정 인증을 통해 서비스에 접속하는 행위.',
    ownerName: '김개발', status: 'PENDING',
    quotes: [{ text: '로그인 실패가 3회 누적되면 잠금된다', source: '회원도메인설계노트.md · 1절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-6', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '동시접속 제한', words: ['동시접속 제한', '계정 공유 차단', 'max_session_error'], type: '동의어',
    occurrenceCount: 8, occurredDocumentIds: ['doc-member'],
    proposedDefinition: '한 계정에서 허용된 동시 접속 수를 초과한 상태.',
    ownerName: '미지정', status: 'PENDING',
    quotes: [{ text: '동시접속 제한을 초과하면 이전 세션이 종료된다', source: '회원도메인설계노트.md · 2절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
  {
    id: 'cand-7', draftDictionaryId: DRAFT_DICTIONARY_ID,
    form: '배송 완료', words: ['배송 완료', '발송 처리', 'shipped'], type: '동의어',
    occurrenceCount: 7, occurredDocumentIds: ['doc-price'],
    proposedDefinition: '주문한 상품이 이용자에게 전달 완료된 상태.',
    ownerName: '미지정', status: 'PENDING',
    quotes: [{ text: '배송 완료 후 7일 이내 교환이 가능하다', source: '구독상품요금제개편안.md · 6절', split: false }],
    createdAt: '2026-09-03T14:20:00.000Z', createdBy: 'member-mock-owner', updatedAt: '2026-09-03T14:20:00.000Z',
  },
]

export const DRAFT_DICTIONARY_FIXTURE: DraftDictionary = {
  id: DRAFT_DICTIONARY_ID,
  workspaceId: 'potenup_be',
  dictionaryId: DICTIONARY_FIXTURE.id,
  sourceDocumentIds: ['doc-plan', 'doc-price', 'doc-campaign', 'doc-retention', 'doc-payment', 'doc-member'],
  candidates: CANDIDATE_TERM_FIXTURES,
  status: 'IN_PROGRESS',
  createdAt: '2026-09-03T14:20:00.000Z',
  createdBy: 'member-mock-owner',
  updatedAt: '2026-09-03T14:20:00.000Z',
}

/**
 * ui/main.js renderExtractScreen()의 인라인 목데이터 이전. docs/DOMAIN.md에는 "최종본 여부"
 * 같은 상태가 없어(=outdated 판별과 마찬가지로 미확정 영역) 화면 전용 값으로만 둔다.
 */
export interface ExtractionEligibleDocument {
  documentId: string
  title: string
  eligible: boolean
  reason?: string
}

export const EXTRACTION_ELIGIBLE_DOCUMENTS: ExtractionEligibleDocument[] = [
  { documentId: 'doc-plan', title: '기획(Planning) 정책 정의서', eligible: false, reason: 'AI 검토, 최종본 아님' },
  { documentId: 'doc-price', title: '구독 상품 요금제 개편안', eligible: true },
  { documentId: 'doc-campaign', title: '2026 상반기 캠페인 실행안', eligible: false, reason: '초안 단계' },
  { documentId: 'doc-retention', title: '리텐션 지표 정의서', eligible: true },
  { documentId: 'doc-payment', title: '결제 API 명세 v2', eligible: false, reason: '리뷰 단계, 최종본 아님' },
  { documentId: 'doc-member', title: '회원 도메인 설계 노트', eligible: false, reason: '임시 단계' },
]
