import type { RuleSet, Workspace } from './types'

// ui/data.js WORKSPACES 이전. 원본은 dictBadge/members/docs/activity 같은 화면 전용 파생
// 필드(사전집 상태, 참여자 수, 문서 수, 마지막 활동 시각 — 전부 다른 도메인을 조인해야 나오는
// 값)까지 섞여 있었는데, 그건 Workspace 도메인 모델이 아니라 목록 화면의 뷰 모델이다.
// 여기서는 도메인 필드만 남기고, 화면에 필요한 집계 값은 실제로 그 화면을 만드는 Phase 6에서
// (여러 도메인의 hooks를 조합하는 지점인 페이지 컴포넌트에서) 채운다.
export const WORKSPACE_FIXTURES: Workspace[] = [
  {
    id: 'potenup_be',
    name: 'POTENUP_BE',
    createdAt: '2026-08-24T00:00:00.000Z',
    createdBy: 'member-mock-owner',
    updatedAt: '2026-09-01T00:00:00.000Z',
  },
  {
    id: 'potenup_fe',
    name: 'POTENUP_FE',
    createdAt: '2026-08-20T00:00:00.000Z',
    createdBy: 'member-mock-owner',
    updatedAt: '2026-08-30T00:00:00.000Z',
  },
  {
    id: 'new_biz_tf',
    name: '신규 사업 TF',
    createdAt: '2026-08-15T00:00:00.000Z',
    createdBy: 'member-mock-owner',
    updatedAt: '2026-09-05T00:00:00.000Z',
  },
]

// ui/main.js renderSettingsRuleset()의 숫자값(2, 2) 이전. "승인 후 내용이 바뀌면 기존 승인
// 무효화"·"작성자 본인도 리뷰어로 셀 수 있음" 두 토글은 docs/DOMAIN.md RuleSet 표에 없는
// 값이라 도메인 타입에 넣지 않았다 — 지금은 화면 로컬 상태로만 다루고, 실제로 저장해야
// 한다면 백엔드 확정 후 RuleSet을 확장한다.
export const RULE_SET_FIXTURE: RuleSet = {
  id: 'ruleset-potenup-be',
  workspaceId: 'potenup_be',
  requiredDictionaryReviewerCount: 2,
  requiredDocumentReviewerCount: 2,
  createdAt: '2026-08-24T00:00:00.000Z',
  createdBy: 'member-mock-owner',
  updatedAt: '2026-08-24T00:00:00.000Z',
}
