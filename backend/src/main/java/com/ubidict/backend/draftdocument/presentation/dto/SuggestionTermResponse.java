package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermResult;

public record SuggestionTermResponse(
        Long id,
        Long draftDocumentId,
        TextRange anchor,
        String originTerm,
        String suggestionTerm,
        SuggestionTermStatus status) {
    public static SuggestionTermResponse from(SuggestionTermResult r) {
        return new SuggestionTermResponse(
                r.id(), r.draftDocumentId(), r.anchor(), r.originTerm(), r.suggestionTerm(), r.status());
    }
}
