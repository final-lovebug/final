package com.ubidict.backend.dictionary.presentation.dto;

import com.ubidict.backend.dictionary.service.model.TermResult;

public record TermResponse(Long termId, String preferredForm, String englishName, String definition) {

    public static TermResponse from(TermResult result) {
        return new TermResponse(result.termId(), result.preferredForm(), result.englishName(), result.definition());
    }
}
