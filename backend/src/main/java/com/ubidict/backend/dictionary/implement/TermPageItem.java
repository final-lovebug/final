package com.ubidict.backend.dictionary.implement;

/** 정의 본문을 제외한 사전집 용어 목록 항목이다. */
public record TermPageItem(Long termId, String preferredForm, String englishName) {}
