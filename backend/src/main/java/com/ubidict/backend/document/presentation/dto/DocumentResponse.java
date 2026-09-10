package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.DocumentResult;
import java.time.OffsetDateTime;
import java.util.List;

public record DocumentResponse(
        Long documentId,
        Long workspaceId,
        String title,
        String content,
        int currentVersionNo,
        boolean outdated,
        Integer dictionaryVersionNo,
        List<String> labels,
        Long uploaderId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DocumentResponse from(DocumentResult result) {
        return new DocumentResponse(
                result.documentId(),
                result.workspaceId(),
                result.title(),
                result.content(),
                result.currentVersionNo(),
                result.outdated(),
                result.dictionaryVersionNo(),
                result.labels(),
                result.uploaderId(),
                result.createdAt(),
                result.updatedAt());
    }
}
