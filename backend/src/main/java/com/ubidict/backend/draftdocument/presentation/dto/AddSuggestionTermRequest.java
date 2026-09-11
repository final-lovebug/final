package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.service.model.AddSuggestionTermCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddSuggestionTermRequest(
        @NotNull TextRange anchor,
        @NotBlank String originTerm,
        @NotBlank String suggestionTerm) {
    public AddSuggestionTermCommand toCommand(Long id, Long member) {
        return new AddSuggestionTermCommand(id, anchor, originTerm, suggestionTerm, member);
    }
}
