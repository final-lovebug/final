package com.ubidict.backend.document.service.model;

import com.ubidict.backend.document.domain.DocumentVersion;
import java.time.OffsetDateTime;

public record DocumentVersionResult(
        int versionNo,
        String body,
        OffsetDateTime publishedAt,
        Integer dictionaryVersionNo,
        boolean edited,
        Long publishedBy) {
    public static DocumentVersionResult from(DocumentVersion version) {
        return new DocumentVersionResult(
                version.versionNo(),
                version.getBody(),
                version.publishedAt(),
                version.getDictionaryVersionNo(),
                version.isEdited(),
                version.getCreatedBy());
    }
}
