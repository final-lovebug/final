package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.DocumentVersionResult;
import java.time.OffsetDateTime;

public record DocumentVersionResponse(
        int versionNo, String body, OffsetDateTime publishedAt, Integer dictionaryVersionNo, Long publishedBy) {

    public static DocumentVersionResponse from(DocumentVersionResult result) {
        return new DocumentVersionResponse(
                result.versionNo(),
                result.body(),
                result.publishedAt(),
                result.dictionaryVersionNo(),
                result.publishedBy());
    }
}
