package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import jakarta.validation.constraints.*;
import java.util.List;

public record AddCandidateTermRequest(
        @NotBlank String form,
        String proposedDefinition,
        String proposedEnglishName,
        List<Long> occurredDocumentIds,
        @PositiveOrZero int occurrenceCount,
        List<String> contextSnippets) {
    public AddCandidateTermCommand toCommand(Long id, Long member) {
        return new AddCandidateTermCommand(
                id,
                form,
                proposedDefinition,
                proposedEnglishName,
                occurredDocumentIds,
                occurrenceCount,
                contextSnippets,
                member);
    }
}
