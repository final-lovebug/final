// ui/data.js REVISIONS/DICT_DIFF_R6_R7 이전. "새 지적/영향 없음/재검사/공식 전환" 같은
// 등급(grade)은 docs/DOMAIN.md에 없는 화면 전용 분류다 — DictionaryVersion을 그대로 쓰지 않고
// 이 화면만을 위한 타임라인 아이템 타입을 별도로 둔다.
export type DictionaryRevisionGrade = '새 지적' | '영향 없음' | '재검사' | '공식 전환' | '—'
export type DictionaryRevisionTone = 'success' | 'neutral' | 'danger' | 'accent'

export interface DictionaryRevisionTimelineItem {
  id: string
  grade: DictionaryRevisionGrade
  tone: DictionaryRevisionTone
  date: string
  author: string
  summary: string
  current: boolean
  emphasize: boolean
}

export const DICTIONARY_REVISION_TIMELINE: DictionaryRevisionTimelineItem[] = [
  { id: 'r7', grade: '새 지적', tone: 'success', date: '2026-09-01', author: '김개발', summary: '용어 5개 추가 (구독 일시정지 외 4건)', current: true, emphasize: false },
  { id: 'r6', grade: '영향 없음', tone: 'neutral', date: '2026-08-31', author: '김개발', summary: '정의 문구 3건 수정', current: false, emphasize: false },
  { id: 'r5', grade: '재검사', tone: 'danger', date: '2026-08-29', author: '민뱅', summary: '대표어 변경 — 이용자 → 구독자(Subscriber)', current: false, emphasize: true },
  { id: 'r4', grade: '새 지적', tone: 'success', date: '2026-08-28', author: '민뱅', summary: '용어 3개 추가 · 마케팅 매핑 2건 보완', current: false, emphasize: false },
  { id: 'r3', grade: '공식 전환', tone: 'accent', date: '2026-08-28', author: '민뱅', summary: '공식 전환 — 초안 단계 종료', current: false, emphasize: false },
  { id: 'r2', grade: '새 지적', tone: 'success', date: '2026-08-27', author: '이백엔드', summary: '용어 4개 추가', current: false, emphasize: false },
  { id: 'r1', grade: '영향 없음', tone: 'neutral', date: '2026-08-26', author: '김개발', summary: '정의 문구 6건 수정', current: false, emphasize: false },
  { id: 'r0', grade: '—', tone: 'neutral', date: '2026-08-25', author: '민뱅', summary: '첫 후보 승인분 등재 (초안)', current: false, emphasize: false },
]

export interface DictionaryVersionDiff {
  added: string[]
  changed: string[]
  removed: string[]
}

export const DICTIONARY_DIFF_R6_R7: DictionaryVersionDiff = {
  added: ['구독 일시정지(Subscription Pause)', '결제 환불(Payment Refund)', '콘텐츠 추천', '콘텐츠 평가', '기기 초과'],
  changed: ['리워드 포인트 매핑 보완', '프로모션 정의 문구 수정', '활성 일수 매핑 수정'],
  removed: [],
}
