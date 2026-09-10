// Mock data for the 유비쿼터스 언어 사전 (Ubiquitous Language Dictionary) prototype.
// Frontend-only — nothing here is persisted or fetched from a server.

const WORKSPACES = [
  { id: 'potenup_be', name: 'POTENUP_BE', dictBadge: '사전집 r7', dictBadgeTone: 'accent', members: '5 / 5', docs: 6, activity: '방금 전', clickable: true },
  { id: 'potenup_fe', name: 'POTENUP_FE', dictBadge: '사전집 r3 · 초안', dictBadgeTone: 'warn', members: '4 / 5', docs: 12, activity: '2시간 전', clickable: false },
  { id: 'new_biz_tf', name: '신규 사업 TF', dictBadge: '아직 사전집이 없습니다', dictBadgeTone: 'neutral', members: '2 / 5', docs: 3, activity: '5일 전', clickable: false },
];

const DOCUMENTS = [
  { id: 'doc-plan', name: '기획(Planning) 정책 정의서', desc: '신규 획득 정책과 회원 상태 전환 규칙 정의', ver: 'r4', badge: 'danger', label: '정책', author: '박기획', editor: '이서연', modified: '2026-08-29 14:20' },
  { id: 'doc-price', name: '구독 상품 요금제 개편안', desc: '플랜 업그레이드 및 프로모션 정책 개편', ver: 'r7', badge: 'none', label: '요금제', author: '박준호', editor: '박준호', modified: '2026-08-20 09:10' },
  { id: 'doc-campaign', name: '2026 상반기 캠페인 실행안', desc: '프로모션 및 리워드 포인트 지급 기준', ver: 'r7', badge: 'none', label: '캠페인', author: '최유진', editor: '최유진', modified: '2026-08-31 17:45' },
  { id: 'doc-retention', name: '리텐션 지표 정의서', desc: '활성 일수 및 구독 해지 지표 산출 기준', ver: 'r6', badge: 'warn', label: '리텐션', author: '한소희', editor: '한소희', modified: '2026-08-18 11:02' },
  { id: 'doc-payment', name: '결제 API 명세 v2', desc: '결제 실패 및 환불 처리 API 정의', ver: 'r7', badge: 'none', label: '결제', author: '정민재', editor: '김개발', modified: '2026-08-22 16:30' },
  { id: 'doc-member', name: '회원 도메인 설계 노트', desc: '구독자 상태 모델 및 이벤트 설계 초안', ver: '—', badge: 'none', label: '도메인', author: '김도현', editor: '김도현', modified: '2026-09-01 10:15' },
];

// The document driving the review/history screens throughout this prototype.
const FOCUS_DOC = DOCUMENTS[0]; // 기획(Planning) 정책 정의서

const MEMBERS = [
  { initial: '민', name: '민뱅', email: 'idabc1234@gmail.com', role: 'OWNER', joined: '2026-08-24', removable: false },
  { initial: '김', name: '김개발', email: 'kim.dev@potenup.io', role: 'ADMIN', joined: '2026-08-24', removable: true },
  { initial: '이', name: '이백엔드', email: 'lee.be@potenup.io', role: 'ADMIN', joined: '2026-08-25', removable: true },
  { initial: '박', name: '박기획', email: 'park.pm@potenup.io', role: 'MEMBER', joined: '2026-08-26', removable: true },
  { initial: '최', name: '최마케팅', email: 'choi.mkt@potenup.io', role: 'MEMBER', joined: '2026-08-28', removable: true },
];

const REVISIONS = [
  { id: 'r7', grade: '새 지적', tone: 'success', date: '2026-09-01', author: '김개발', summary: '용어 5개 추가 (구독 일시정지 외 4건)', current: true, emphasize: false },
  { id: 'r6', grade: '영향 없음', tone: 'neutral', date: '2026-08-31', author: '김개발', summary: '정의 문구 3건 수정', current: false, emphasize: false },
  { id: 'r5', grade: '재검사', tone: 'danger', date: '2026-08-29', author: '민뱅', summary: '대표어 변경 — 이용자 → 구독자(Subscriber)', current: false, emphasize: true },
  { id: 'r4', grade: '새 지적', tone: 'success', date: '2026-08-28', author: '민뱅', summary: '용어 3개 추가 · 마케팅 매핑 2건 보완', current: false, emphasize: false },
  { id: 'r3', grade: '공식 전환', tone: 'accent', date: '2026-08-28', author: '민뱅', summary: '공식 전환 — 초안 단계 종료', current: false, emphasize: false },
  { id: 'r2', grade: '새 지적', tone: 'success', date: '2026-08-27', author: '이백엔드', summary: '용어 4개 추가', current: false, emphasize: false },
  { id: 'r1', grade: '영향 없음', tone: 'neutral', date: '2026-08-26', author: '김개발', summary: '정의 문구 6건 수정', current: false, emphasize: false },
  { id: 'r0', grade: '—', tone: 'neutral', date: '2026-08-25', author: '민뱅', summary: '첫 후보 승인분 등재 (초안)', current: false, emphasize: false },
];

const TERMS = [
  { name: '구독자 (Subscriber)', def: '서비스를 이용 중인 사용자', modified: '08-30' },
  { name: '무료 체험 (Free Trial)', def: '과금 전 제공되는 시험 이용 기간', modified: '08-18' },
  { name: '구독 활성화 (Subscription Activation)', def: '유료 구독이 정상 등록된 상태', modified: '08-18' },
  { name: '구독 해지 (Subscription Cancellation)', def: '구독 관계가 종료된 상태', modified: '08-15' },
  { name: '플랜 업그레이드 (Plan Upgrade)', def: '상위 요금제로 전환', modified: '08-12' },
  { name: '콘텐츠 조회 (Content View)', def: '콘텐츠 상세 페이지 열람', modified: '08-10' },
  { name: '리워드 포인트 (Reward Point)', def: '적립·사용 가능한 보상 포인트', modified: '08-09' },
  { name: '프로모션 (Promotion)', def: '한정 기간 제공되는 할인·혜택', modified: '08-09' },
  { name: '결제 실패 (Payment Failure)', def: '결제 승인이 거절되거나 처리되지 않은 상태', modified: '08-05' },
  { name: '활성 일수 (Active Days)', def: '최근 기간 내 실제 로그인·이용한 날수', modified: '08-31' },
];

// Highlighted term whose "구독자가 쓰인 문서" reverse index used to live in the
// 사전집 side panel before it moved behind the MVP2 flag.
const TERM_IN_FOCUS = '구독자 (Subscriber)';

const CANDIDATES = [
  {
    id: 0, title: '이용 보류', words: ['이용 보류', '휴면 전환', 'suspend_account'],
    type: '동의어', occurrences: 23, docCount: 5, owner: '김개발',
    def: '이용자가 일정 기간 서비스를 쓰지 않도록 계정을 잠시 멈춘 상태.',
    quotes: [{ text: '이용을 보류 처리하면 로그인이 제한된다', source: '커머스기획안.md · 4절', split: false }],
  },
  {
    id: 1, title: '주문', words: ['주문'],
    type: '동형이의', occurrences: 47, docCount: 9, owner: '미지정',
    def: '결제가 완료되어 확정된 구매 건. 장바구니에 담긴 상태는 주문이 아니다.',
    quotes: [
      { text: '장바구니에 담으면 주문이 생성된다', source: '기획팀 · 커머스기획안.md · 3절 2문단', split: false },
      { text: '결제 승인 후 주문이 확정된다', source: '개발팀 · 결제API명세v2.md · 2.1 상태 전이', split: true },
    ],
    splitNote: '같은 말인데 기획팀은 장바구니 시점, 개발팀은 결제 완료 시점으로 쓰고 있습니다',
  },
  {
    id: 2, title: '결제 취소', words: ['결제 취소', '매출 차감', 'pg_refund'],
    type: '동의어', occurrences: 19, docCount: 4, owner: '이백엔드',
    def: '이미 승인된 결제를 취소하고 금액을 되돌리는 처리.',
    quotes: [{ text: '결제 취소 시 매출이 차감 처리된다', source: '결제API명세v2.md · 3절', split: false }],
  },
  {
    id: 3, title: '큐레이션', words: ['큐레이션', '개인화 타겟팅', 'get_suggested_list'],
    type: '동의어', occurrences: 14, docCount: 3, owner: '미지정',
    def: '이용자 행동 데이터를 기반으로 콘텐츠를 추천하는 기능.',
    quotes: [{ text: '큐레이션 결과는 최근 조회 기록을 반영한다', source: '캠페인실행안.md · 2절', split: false }],
  },
  {
    id: 4, title: '별점 리뷰', words: ['별점 리뷰', '유저 반응(UGC)', 'update_star_score'],
    type: '동의어', occurrences: 11, docCount: 3, owner: '미지정',
    def: '이용자가 콘텐츠에 남기는 평점 및 후기.',
    quotes: [{ text: '별점 리뷰는 5점 만점으로 집계된다', source: '리텐션지표정의서.md · 5절', split: false }],
  },
  {
    id: 5, title: '로그인', words: ['로그인', '로그 인', 'login'],
    type: '표기 변형', occurrences: 12, docCount: 5, owner: '김개발',
    def: '계정 인증을 통해 서비스에 접속하는 행위.',
    quotes: [{ text: '로그인 실패가 3회 누적되면 잠금된다', source: '회원도메인설계노트.md · 1절', split: false }],
  },
  {
    id: 6, title: '동시접속 제한', words: ['동시접속 제한', '계정 공유 차단', 'max_session_error'],
    type: '동의어', occurrences: 8, docCount: 2, owner: '미지정',
    def: '한 계정에서 허용된 동시 접속 수를 초과한 상태.',
    quotes: [{ text: '동시접속 제한을 초과하면 이전 세션이 종료된다', source: '회원도메인설계노트.md · 2절', split: false }],
  },
  {
    id: 7, title: '배송 완료', words: ['배송 완료', '발송 처리', 'shipped'],
    type: '동의어', occurrences: 7, docCount: 2, owner: '미지정',
    def: '주문한 상품이 이용자에게 전달 완료된 상태.',
    quotes: [{ text: '배송 완료 후 7일 이내 교환이 가능하다', source: '구독상품요금제개편안.md · 6절', split: false }],
  },
];

const LABELS = [
  { name: '정책', count: 1 }, { name: '요금제', count: 1 }, { name: '캠페인', count: 1 },
  { name: '리텐션', count: 1 }, { name: '결제', count: 1 }, { name: '도메인', count: 1 },
];

// 07 화면(문서 검토): 기획(Planning) 정책 정의서 본문 — 밑줄 친 부분이 치환 제안 대상.
// status: 'pending' (아직 처리 안 함, 파란 밑줄) | 'resolved' (이미 적용됨, 회색 점선)
const REVIEW_SUGGESTIONS = [
  { id: 's1', original: '이용자', suggestion: '구독자(Subscriber)', status: 'pending' },
  { id: 's2', original: '체험판', suggestion: '무료 체험(Free Trial)', status: 'pending' },
  { id: 's3', original: '가입완료', suggestion: '구독 활성화(Subscription Activation)', status: 'resolved', action: '적용' },
  { id: 's4', original: '오류', suggestion: '결제 실패(Payment Failure)', status: 'pending' },
  { id: 's5', original: '탈퇴', suggestion: '구독 해지(Subscription Cancellation)', status: 'pending' },
  { id: 's6', original: '요금제 변경', suggestion: '플랜 업그레이드(Plan Upgrade)', status: 'resolved', action: '적용' },
  { id: 's7', original: '마일리지', suggestion: '리워드 포인트(Reward Point)', status: 'pending' },
];

// 처리 내역 탭(리뷰어가 보는 화면) — 문서 검토가 실제로 어떻게 끝났는지의 기록.
const REVIEW_HISTORY = [
  { original: '이용자', result: '구독자(Subscriber)', action: 'applied' },
  { original: '체험판', result: '무료 체험(Free Trial)', action: 'applied' },
  { original: '가입완료', result: '구독 활성화(Subscription Activation)', action: 'applied' },
  { original: '오류', result: '결제 실패(Payment Failure)', action: 'ignored', reason: '이 문단은 결제 오류가 아니라 일반 오류입니다' },
  { original: '탈퇴', result: '구독 해지(Subscription Cancellation)', action: 'ignored', reason: '사유 없음' },
  { original: '마일리지', result: '리워드 포인트', action: 'manual', manualValue: '포인트' },
];

const DOC_HISTORY_VERSIONS = [
  { id: 'v3', current: true, date: '09-01 11:30', author: '박기획', summary: '치환 제안 12건 중 적용 9 · 무시 2 · 직접입력 1', appliedDict: 'r4' },
  { id: 'v2', current: false, date: '08-31 09:05', author: '박기획', summary: '2절 비즈니스 규칙 문단 추가', appliedDict: 'r4' },
  { id: 'v1', current: false, date: '08-28 14:20', author: '박기획', summary: '최초 업로드', appliedDict: '—' },
];

const DICT_DIFF_R6_R7 = {
  added: ['구독 일시정지(Subscription Pause)', '결제 환불(Payment Refund)', '콘텐츠 추천', '콘텐츠 평가', '기기 초과'],
  changed: ['리워드 포인트 매핑 보완', '프로모션 정의 문구 수정', '활성 일수 매핑 수정'],
  removed: [],
};

// 사전집 개정안(20 · PR 스타일 리뷰) 표
const REVISION_ROWS = [
  { term: '구독 일시정지', change: '추가', changeTone: 'success', comments: 0 },
  { term: '주문', change: '추가', changeTone: 'success', comments: 2, commentNote: '뜻이 갈립니다', highlighted: true },
  { term: '활성 일수', change: '정의 수정', changeTone: 'warn', comments: 0 },
];

const REVISION_THREAD = [
  { initial: '김', name: '김개발', tone: 'accent', time: '40분 전', text: '기획팀은 장바구니 시점, 개발팀은 결제 완료 시점으로 씁니다. 표준어 정의를 결제 완료 기준으로 명확히 해주세요.', canConvert: true },
  { initial: '이', name: '이백엔드', tone: 'warn', time: '12분 전', text: '동의합니다. 결제 완료 시점으로 확정하죠.' },
];

const REVIEW_THREAD_COMMENTS = [
  { initial: '이', name: '이백엔드', tone: 'warn', time: '2시간 전', text: '여기 "오류"는 PG 응답 실패가 아니라 검증 오류입니다. 결제 실패로 치환하면 안 됩니다.', mine: true },
  { initial: '최', name: '최마케팅', tone: 'success', time: '10분 전', text: '마케팅 문서에서는 "혜택금"으로도 씁니다. 사전에 매핑 하나 더 추가해주세요.', mine: false },
];

const NOTIFICATIONS = [
  { id: 'n1', read: false, title: '기획(Planning) 정책 정의서 검토가 끝났습니다', meta: '제안 12건 · 12분 전', action: '확인', go: 'reviewDoc' },
  { id: 'n2', read: false, title: '사전집 r7이 발행되었습니다', meta: '새 지적 · 용어 5개 추가 · 1시간 전', action: '사전집 보기', go: 'dictionary' },
  { id: 'n3', read: true, title: '기획(Planning) 정책 정의서가 쓰는 용어가 바뀌었습니다', meta: '이용자 → 구독자(Subscriber) · r5 · 3일 전', action: '최신 사전집으로 갱신', go: 'docDetail' },
];

const NOTIF_CHANNELS = [
  { name: '인앱 알림', desc: '헤더 벨 아이콘에 표시됩니다', on: true, badge: null },
  { name: 'Slack', desc: '워크스페이스별 웹훅 URL을 등록하면 채널로 발송됩니다 · 구현은 URL 하나에 POST하는 것이 전부입니다', on: false, badge: 'MVP2' },
  { name: '메일', desc: null, on: false, badge: 'MVP2' },
  { name: '웹 push', desc: '브라우저를 닫아도 받습니다. 브라우저 알림 권한이 필요합니다', on: false, badge: 'MVP3' },
];

const NOTIF_MATRIX = [
  { trigger: '대조 완료' },
  { trigger: '사전집 발행' },
  { trigger: '사전집 차이' },
];
