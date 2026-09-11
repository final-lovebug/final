package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.service.model.EditSuggestionTermCommand;

public record EditSuggestionTermRequest(TextRange anchor, String originTerm, String suggestionTerm) {
    public EditSuggestionTermCommand toCommand(Long id, Long member) {
        return new EditSuggestionTermCommand(id, anchor, originTerm, suggestionTerm, member);
    }
}
