package com.ubidict.backend.dictionary.service.model;

import com.ubidict.backend.dictionary.domain.Term;

public record TermResult(Long termId, String preferredForm, String englishName, String definition) {

    public static TermResult from(Term term) {
        return new TermResult(term.getId(), term.getPreferredForm(), term.getEnglishName(), term.getDefinition());
    }
}
