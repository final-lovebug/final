import type { Label } from './types'

// documentCount(해당 라벨이 붙은 문서 건수)는 Label 자체 속성도 API 응답도 아니라
// 집계값이다 — 설정 화면이 문서 목록의 labels로 직접 센다. Label 엔티티 자체가
// docs/DOMAIN.md 467~469줄 기준 아직 "미확정"이라는 점은 types.ts에 적어뒀다.
export interface LabelListItem extends Label {
  documentCount: number
}
