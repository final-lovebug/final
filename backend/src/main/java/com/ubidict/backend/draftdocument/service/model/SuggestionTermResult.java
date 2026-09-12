package com.ubidict.backend.draftdocument.service.model;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.domain.*;
import java.time.OffsetDateTime;

public record SuggestionTermResult(
        Long id,
        Long draftDocumentId,
        TextRange anchor,
        String originTerm,
        String suggestionTerm,
        SuggestionTermStatus status,
        Long handledBy,
        String rejectReason,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
    public static SuggestionTermResult from(SuggestionTerm t) {
        return new SuggestionTermResult(
                t.getId(),
                t.getDraftDocumentId(),
                t.getAnchor(),
                t.getOriginTerm(),
                t.getSuggestionTerm(),
                t.getStatus(),
                t.getHandledBy(),
                t.getRejectReason(),
                t.getCreatedBy(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
