package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import java.time.OffsetDateTime;

public record DraftDocumentResponse(
        Long draftDocumentId,
        Long documentId,
        int baseVersionNo,
        String draftBody,
        DraftDocumentStatus status,
        Long requestedBy,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DraftDocumentResponse from(DraftDocumentResult result) {
        return new DraftDocumentResponse(
                result.draftDocumentId(),
                result.documentId(),
                result.baseVersionNo(),
                result.draftBody(),
                result.status(),
                result.requestedBy(),
                result.createdBy(),
                result.createdAt(),
                result.updatedAt());
    }
}
