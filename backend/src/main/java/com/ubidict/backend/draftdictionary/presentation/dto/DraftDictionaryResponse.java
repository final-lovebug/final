package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import java.time.OffsetDateTime;
import java.util.List;

public record DraftDictionaryResponse(
        Long draftDictionaryId,
        Long workspaceId,
        Long dictionaryId,
        List<Long> sourceDocumentIds,
        DraftDictionaryStatus status,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DraftDictionaryResponse from(DraftDictionaryResult result) {
        return new DraftDictionaryResponse(
                result.draftDictionaryId(),
                result.workspaceId(),
                result.dictionaryId(),
                result.sourceDocumentIds(),
                result.status(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt());
    }
}
