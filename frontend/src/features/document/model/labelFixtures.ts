import type { Label } from './types'

// ui/data.js LABELS 이전. count(해당 라벨이 붙은 문서 건수)는 Label 자체 속성이 아니라
// 집계값이라 별도 뷰 타입으로 뒀다. Label 엔티티 자체가 docs/DOMAIN.md 467~469줄 기준
// 아직 "미확정"이라는 점은 types.ts의 Label 정의에 이미 적어뒀다.
export interface LabelListItem extends Label {
  documentCount: number
}

export const LABEL_FIXTURES: LabelListItem[] = [
  { id: 'label-policy', name: '정책', documentCount: 1 },
  { id: 'label-pricing', name: '요금제', documentCount: 1 },
  { id: 'label-campaign', name: '캠페인', documentCount: 1 },
  { id: 'label-retention', name: '리텐션', documentCount: 1 },
  { id: 'label-payment', name: '결제', documentCount: 1 },
  { id: 'label-domain', name: '도메인', documentCount: 1 },
]
