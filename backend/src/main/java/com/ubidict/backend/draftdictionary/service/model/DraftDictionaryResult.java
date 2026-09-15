package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record DraftDictionaryResult(
        Long draftDictionaryId,
        Long workspaceId,
        Long dictionaryId,
        List<Long> sourceDocumentIds,
        DraftDictionaryStatus status,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DraftDictionaryResult from(DraftDictionary draftDictionary) {
        return new DraftDictionaryResult(
                draftDictionary.getId(),
                draftDictionary.getWorkspaceId(),
                draftDictionary.getDictionaryId(),
                List.copyOf(draftDictionary.getSourceDocumentIds()),
                draftDictionary.getStatus(),
                draftDictionary.getCreatedBy(),
                draftDictionary.getCreatedAt(),
                draftDictionary.getUpdatedAt());
    }
}
