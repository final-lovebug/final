package com.ubidict.backend.draftdocument.service.model;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import java.time.OffsetDateTime;

public record DraftDocumentResult(
        Long draftDocumentId,
        Long documentId,
        int baseVersionNo,
        String draftBody,
        DraftDocumentStatus status,
        Long requestedBy,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DraftDocumentResult from(DraftDocument draftDocument) {
        return new DraftDocumentResult(
                draftDocument.getId(),
                draftDocument.getDocumentId(),
                draftDocument.getBaseVersionNo(),
                draftDocument.getDraftBody(),
                draftDocument.getStatus(),
                draftDocument.getRequestedBy(),
                draftDocument.getCreatedBy(),
                draftDocument.getCreatedAt(),
                draftDocument.getUpdatedAt());
    }
}
