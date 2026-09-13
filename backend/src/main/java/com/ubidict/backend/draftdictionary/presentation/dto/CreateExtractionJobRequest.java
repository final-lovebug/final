package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.service.model.CreateExtractionJobCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateExtractionJobRequest(
        @NotNull Long workspaceId,
        Long dictionaryId,
        @NotEmpty List<@NotNull Long> sourceDocumentIds) {

    public CreateExtractionJobCommand toCommand(Long memberId) {
        return new CreateExtractionJobCommand(workspaceId, dictionaryId, sourceDocumentIds, memberId);
    }
}
