import type { Document, Label } from './types'

// ui/data.js DOCUMENTS 이전. 원본 mock은 author/editor를 이름 문자열로만 갖고 있고,
// 그 이름들이 MEMBERS 목록과 완전히 일치하지도 않는다(프로토타입 단계의 느슨한 목데이터).
// Document 도메인 타입(ownerId/updaterId: MemberId)은 그대로 유지하되, 화면에 바로 쓸
// 이름은 DocumentListItem에 별도로 얹었다 — 실제로는 회원 도메인과 조인해서 나와야 할
// 값이라, 여기 있는 ownerName/updaterName은 백엔드 연동 시 없어질 목업 전용 편의 필드다.
export interface DocumentListItem extends Document {
  ownerName: string
  updaterName: string
  /** docs/DOMAIN.md 미확정(Label 엔티티 없음) — features/document/model/types.ts의 Label 참고 */
  label?: Label
  /** 재검사 필요 / 뒤처짐. outdated 판별 속성도 아직 미확정이라 화면 표시용 값만 둔다 */
  badge?: 'danger' | 'warn'
}

export const DOCUMENT_FIXTURES: DocumentListItem[] = [
  {
    id: 'doc-plan',
    workspaceId: 'potenup_be',
    title: '기획(Planning) 정책 정의서',
    content: '신규 획득 정책과 회원 상태 전환 규칙 정의',
    currentVersionNo: 4,
    ownerId: 'member-mock-park-pm',
    updaterId: 'member-mock-lee-seoyeon',
    ownerName: '박기획',
    updaterName: '이서연',
    label: { id: 'label-policy', name: '정책' },
    badge: 'danger',
    createdAt: '2026-08-24T00:00:00.000Z',
    createdBy: 'member-mock-park-pm',
    updatedAt: '2026-08-29T14:20:00.000Z',
  },
  {
    id: 'doc-price',
    workspaceId: 'potenup_be',
    title: '구독 상품 요금제 개편안',
    content: '플랜 업그레이드 및 프로모션 정책 개편',
    currentVersionNo: 7,
    ownerId: 'member-mock-park-junho',
    updaterId: 'member-mock-park-junho',
    ownerName: '박준호',
    updaterName: '박준호',
    label: { id: 'label-pricing', name: '요금제' },
    createdAt: '2026-08-15T00:00:00.000Z',
    createdBy: 'member-mock-park-junho',
    updatedAt: '2026-08-20T09:10:00.000Z',
  },
  {
    id: 'doc-campaign',
    workspaceId: 'potenup_be',
    title: '2026 상반기 캠페인 실행안',
    content: '프로모션 및 리워드 포인트 지급 기준',
    currentVersionNo: 7,
    ownerId: 'member-mock-choi-yujin',
    updaterId: 'member-mock-choi-yujin',
    ownerName: '최유진',
    updaterName: '최유진',
    label: { id: 'label-campaign', name: '캠페인' },
    createdAt: '2026-08-20T00:00:00.000Z',
    createdBy: 'member-mock-choi-yujin',
    updatedAt: '2026-08-31T17:45:00.000Z',
  },
  {
    id: 'doc-retention',
    workspaceId: 'potenup_be',
    title: '리텐션 지표 정의서',
    content: '활성 일수 및 구독 해지 지표 산출 기준',
    currentVersionNo: 6,
    ownerId: 'member-mock-han-sohee',
    updaterId: 'member-mock-han-sohee',
    ownerName: '한소희',
    updaterName: '한소희',
    label: { id: 'label-retention', name: '리텐션' },
    badge: 'warn',
    createdAt: '2026-08-10T00:00:00.000Z',
    createdBy: 'member-mock-han-sohee',
    updatedAt: '2026-08-18T11:02:00.000Z',
  },
  {
    id: 'doc-payment',
    workspaceId: 'potenup_be',
    title: '결제 API 명세 v2',
    content: '결제 실패 및 환불 처리 API 정의',
    currentVersionNo: 7,
    ownerId: 'member-mock-jung-minjae',
    updaterId: 'member-mock-kim-dev',
    ownerName: '정민재',
    updaterName: '김개발',
    label: { id: 'label-payment', name: '결제' },
    createdAt: '2026-08-12T00:00:00.000Z',
    createdBy: 'member-mock-jung-minjae',
    updatedAt: '2026-08-22T16:30:00.000Z',
  },
  {
    id: 'doc-member',
    workspaceId: 'potenup_be',
    title: '회원 도메인 설계 노트',
    content: '구독자 상태 모델 및 이벤트 설계 초안',
    currentVersionNo: 0,
    ownerId: 'member-mock-kim-dohyun',
    updaterId: 'member-mock-kim-dohyun',
    ownerName: '김도현',
    updaterName: '김도현',
    label: { id: 'label-domain', name: '도메인' },
    createdAt: '2026-09-01T00:00:00.000Z',
    createdBy: 'member-mock-kim-dohyun',
    updatedAt: '2026-09-01T10:15:00.000Z',
  },
]
