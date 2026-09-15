package com.ubidict.backend.draftdictionary.domain;

/**
 * 후보어를 사람이 분류한 유형. 교정 화면에서 등록할 때 고른다.
 *
 * <p><b>nullable이다.</b> 추출기(ubidict-py)가 돌려주는 후보어에는 이 분류가 없다 — {@code HOMOGRAPH}는 추출 파이프라인이
 * 아예 건너뛰므로 {@link #VARIANT}/{@link #SYNONYM}만으로 유도할 수도 없다. 값이 없으면 화면은 미분류로 표시한다.
 */
public enum CandidateTermType {
    /** 같은 뜻을 다르게 적은 말들 */
    SYNONYM,
    /** 표기는 같은데 팀마다 다른 뜻으로 쓰는 말 */
    HOMOGRAPH,
    /** 같은 말의 표기 차이(띄어쓰기·영문 표기 등) */
    VARIANT
}
