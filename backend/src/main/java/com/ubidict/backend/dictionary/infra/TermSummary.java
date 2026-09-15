package com.ubidict.backend.dictionary.infra;

/** 목록 조회에서 정의 본문을 제외해 필요한 열만 읽는 프로젝션이다. */
public interface TermSummary {

    Long getId();

    String getPreferredForm();

    String getEnglishName();
}
