package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import java.time.OffsetDateTime;

public record CheckJobResponse(
        Long checkJobId,
        Long documentId,
        CheckJobStatus status,
        Long draftDocumentId,
        String failureReason,
        Long requestedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CheckJobResponse from(CheckJobResult result) {
        return new CheckJobResponse(
                result.checkJobId(),
                result.documentId(),
                result.status(),
                result.draftDocumentId(),
                result.failureReason(),
                result.requestedBy(),
                result.createdAt(),
                result.updatedAt());
    }
}
