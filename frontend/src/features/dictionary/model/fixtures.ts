// 목업 데이터는 전부 걷어냈다(2026-09-14) — 남은 것은 실 API 응답을 화면이 쓰는 모양으로
// 합친 **뷰 타입** 둘이다. 이름이 fixtures인 것은 기존 import 경로를 유지하기 위함이다.
import type { CandidateTerm } from './types'

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
