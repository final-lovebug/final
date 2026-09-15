package com.ubidict.backend.draftdictionary.domain;

/**
 * 후보어 판정 상태.
 *
 * <p><b>새 경로는 이 값으로 아무것도 판단하지 않는다</b>({@code docs/plan/DRAFT_PLAN.md}). 사전집 초안이 단일 페이지로 바뀌면서 후보어별
 * 판정이 화면에서 사라졌고, 교정 완료·리뷰 요청의 준비 조건은 「모든 후보어가 대표어와 정의를 가졌는가」로, 발행 목록은 「초안에 남아 있는 전부」로 바뀌었다.
 * 따라서 새로 만들어지는 후보어는 {@link #PENDING}에 머무른다.
 *
 * <p><b>그래도 지우지 않는다</b> — 이미 판정이 기록된 초안 행이 DB에 있어 이 enum이 없으면 읽을 수 없다. 판정 API 5종도 같은 이유로 남아 있다
 * ({@code CandidateTermController}의 「사용 안 함」 주석 블록 참고).
 */
public enum CandidateTermStatus {
    PENDING,

    // 아래 다섯은 사용 안 함 — 과거 데이터를 읽기 위해서만 남아 있다.
    REGISTRATION_APPROVED,
    MERGED_AS_SYNONYM,
    REJECTED,
    ON_HOLD,
    KEPT
}
