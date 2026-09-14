package com.ubidict.backend.dictionary.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ubidict.backend.dictionary.service.model.TermResult;

public record TermResponse(
        Long termId,
        String preferredForm,
        String englishName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String definition) {

    public static TermResponse from(TermResult result) {
        return new TermResponse(result.termId(), result.preferredForm(), result.englishName(), result.definition());
    }
}
