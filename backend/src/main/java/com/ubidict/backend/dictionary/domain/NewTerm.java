package com.ubidict.backend.dictionary.domain;

/**
 * 새 사전집 버전에 등재할 용어 한 건. 저장 전 입력이라 식별자가 없다.
 *
 * <p>표기 비교가 일관되도록 만들어질 때 앞뒤 공백을 제거한다. 값 검증은 {@link Term#create}가 맡는다.
 */
public record NewTerm(String preferredForm, String englishName, String definition) {

    public NewTerm {
        preferredForm = Term.normalize(preferredForm);
        englishName = Term.normalize(englishName);
    }

    public Term toTerm(Long dictionaryId, Long createdBy) {
        return Term.create(dictionaryId, preferredForm, englishName, definition, createdBy);
    }
}
