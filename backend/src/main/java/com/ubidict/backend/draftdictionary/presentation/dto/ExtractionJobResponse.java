package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import java.time.OffsetDateTime;
import java.util.List;

public record ExtractionJobResponse(
        Long extractionJobId,
        Long workspaceId,
        Long dictionaryId,
        List<Long> sourceDocumentIds,
        ExtractionJobStatus status,
        Long draftDictionaryId,
        String failureReason,
        Long requestedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ExtractionJobResponse from(ExtractionJobResult result) {
        return new ExtractionJobResponse(
                result.extractionJobId(),
                result.workspaceId(),
                result.dictionaryId(),
                result.sourceDocumentIds(),
                result.status(),
                result.draftDictionaryId(),
                result.failureReason(),
                result.requestedBy(),
                result.createdAt(),
                result.updatedAt());
    }
}
