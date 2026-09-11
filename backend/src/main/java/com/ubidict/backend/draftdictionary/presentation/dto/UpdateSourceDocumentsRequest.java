package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.service.model.UpdateSourceDocumentsCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateSourceDocumentsRequest(@NotEmpty List<@NotNull Long> sourceDocumentIds) {

    public UpdateSourceDocumentsCommand toCommand(Long draftDictionaryId, Long memberId) {
        return new UpdateSourceDocumentsCommand(draftDictionaryId, sourceDocumentIds, memberId);
    }
}
