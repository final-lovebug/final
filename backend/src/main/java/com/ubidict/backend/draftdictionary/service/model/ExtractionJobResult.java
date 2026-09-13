package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record ExtractionJobResult(
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

    public static ExtractionJobResult from(ExtractionJob extractionJob) {
        return new ExtractionJobResult(
                extractionJob.getId(),
                extractionJob.getWorkspaceId(),
                extractionJob.getDictionaryId(),
                List.copyOf(extractionJob.getSourceDocumentIds()),
                extractionJob.getStatus(),
                extractionJob.getDraftDictionaryId(),
                extractionJob.getFailureReason(),
                extractionJob.getRequestedBy(),
                extractionJob.getCreatedAt(),
                extractionJob.getUpdatedAt());
    }
}
