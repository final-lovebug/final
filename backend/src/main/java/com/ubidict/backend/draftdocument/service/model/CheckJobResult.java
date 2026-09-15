package com.ubidict.backend.draftdocument.service.model;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import java.time.OffsetDateTime;

public record CheckJobResult(
        Long checkJobId,
        Long documentId,
        CheckJobStatus status,
        Long draftDocumentId,
        String failureReason,
        Long requestedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CheckJobResult from(CheckJob checkJob) {
        return new CheckJobResult(
                checkJob.getId(),
                checkJob.getDocumentId(),
                checkJob.getStatus(),
                checkJob.getDraftDocumentId(),
                checkJob.getFailureReason(),
                checkJob.getRequestedBy(),
                checkJob.getCreatedAt(),
                checkJob.getUpdatedAt());
    }
}
