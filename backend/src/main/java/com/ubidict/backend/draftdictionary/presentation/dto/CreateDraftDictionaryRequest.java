package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.service.model.CreateDraftDictionaryCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateDraftDictionaryRequest(
        @NotNull Long workspaceId,
        Long dictionaryId,
        @NotEmpty List<@NotNull Long> sourceDocumentIds) {

    public CreateDraftDictionaryCommand toCommand(Long memberId) {
        return new CreateDraftDictionaryCommand(workspaceId, dictionaryId, sourceDocumentIds, memberId);
    }
}
