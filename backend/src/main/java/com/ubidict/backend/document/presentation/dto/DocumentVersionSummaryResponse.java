package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.DocumentVersionSummaryResult;
import java.time.OffsetDateTime;

public record DocumentVersionSummaryResponse(
        int versionNo, OffsetDateTime publishedAt, Integer dictionaryVersionNo, Long publishedBy) {

    public static DocumentVersionSummaryResponse from(DocumentVersionSummaryResult result) {
        return new DocumentVersionSummaryResponse(
                result.versionNo(), result.publishedAt(), result.dictionaryVersionNo(), result.publishedBy());
    }
}
