package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.RejectSuggestionTermCommand;
import jakarta.validation.constraints.NotBlank;

public record RejectSuggestionTermRequest(@NotBlank String rejectReason) {

    public RejectSuggestionTermCommand toCommand(Long suggestionTermId, Long memberId) {
        return new RejectSuggestionTermCommand(suggestionTermId, rejectReason, memberId);
    }
}
