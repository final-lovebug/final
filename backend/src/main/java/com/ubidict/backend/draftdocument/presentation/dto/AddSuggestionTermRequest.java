package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.AddSuggestionTermCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddSuggestionTermRequest(
        @NotNull @Valid TextRangeRequest anchor,
        @NotBlank String originTerm,
        @NotBlank String suggestionTerm) {
    public AddSuggestionTermCommand toCommand(Long id, Long member) {
        return new AddSuggestionTermCommand(id, anchor.to(), originTerm, suggestionTerm, member);
    }
}
