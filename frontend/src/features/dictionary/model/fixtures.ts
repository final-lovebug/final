import type { CandidateTerm, Dictionary, Term } from './types'

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
 * 후보어 목록 행. 도메인 타입에 없는 화면 전용 정보(표기 묶음, 등록자 표시 이름)를 얹는다
 * — DocumentListItem과 같은 패턴.
 *
 * **인용문의 출처·분리 플래그는 없다(T-INT-11 결정 3).** 백엔드가 주는 것은
 * `contextSnippets: string[]`(CandidateTerm에 이미 있음)뿐이라 어느 문서 몇 문단에서
 * 나온 문장인지, 팀마다 다르게 쓰는지를 복원할 수 없다. 상세 패널은 스니펫 텍스트만 보여준다.
 */
export interface CandidateTermListItem extends CandidateTerm {
  /** 같은 개념으로 묶인 표기들. words[0]이 CandidateTerm.form과 같다(백엔드 variantForms). */
  words: string[]
  /** 등록자(createdBy) 표시 이름. 해석 못 하면 "—". */
  ownerName: string
  /** words 중 표준어로 고른 것. 없으면 words[0]을 기본값으로 쓴다(화면에서 fallback 처리). */
  selectedWord?: string
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
