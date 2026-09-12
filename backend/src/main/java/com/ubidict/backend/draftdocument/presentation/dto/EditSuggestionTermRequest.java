package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.EditSuggestionTermCommand;
import jakarta.validation.Valid;

public record EditSuggestionTermRequest(@Valid TextRangeRequest anchor, String originTerm, String suggestionTerm) {
    public EditSuggestionTermCommand toCommand(Long id, Long member) {
        return new EditSuggestionTermCommand(
                id, anchor == null ? null : anchor.to(), originTerm, suggestionTerm, member);
    }
}
