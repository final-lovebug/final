import type { Document } from './types'

// 목업 데이터는 걷어냈다(2026-09-14) — 남은 것은 문서 목록·상세가 함께 쓰는 **뷰 타입**이다.
// Document 도메인 타입은 그대로 두고, 다른 도메인과 조인해야 얻는 값(작성자 이름 등)만
// 여기에 얹는다.
export interface DocumentListItem extends Document {
  /** `uploaderId`를 `GET /api/members?ids=`로 해석한 이름. 못 찾으면 "—". */
  ownerName?: string
  /** **채우지 않는다** — 문서 응답에 최종 수정자 필드가 없다(T-INT-10). 화면은 "—"로 표시한다. */
  updaterName?: string
  /** 실 API가 주는 라벨 전부(최대 5개). 목록 화면이 전부 그리고, 라벨 필터도 이걸로 거른다.
   *
   * **(2026-09-16)** 대표 라벨 하나만 담던 `label`을 걷어냈다 — 목록이 첫 라벨만 보여주던
   * 시절의 잔재라, 다섯 개를 다 그리기 시작한 지금은 같은 값을 두 모양으로 들고 있을
   * 이유가 없다. */
  labels?: string[]
  /** 재검사 필요 / 뒤처짐. 실연동에서는 `aligned`/`edited`(G-12, D-31)로 유도해서 채운다 —
   * edited===true(직접 편집됨) → 'danger', aligned===false(사전집 갱신 후 안 맞춰짐)
   * → 'warn', aligned===true → 배지 없음 */
  badge?: 'danger' | 'warn'
}
