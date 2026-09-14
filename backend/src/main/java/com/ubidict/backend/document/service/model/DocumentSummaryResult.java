package com.ubidict.backend.document.service.model;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.infra.DocumentVersionSummary;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 목록용. 본문을 담지 않는다.
 */
public record DocumentSummaryResult(
        Long documentId,
        String title,
        int currentVersionNo,
        boolean aligned,
        boolean edited,
        Integer dictionaryVersionNo,
        List<String> labels,
        Long uploaderId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
    public static DocumentSummaryResult of(
            Document document,
            DocumentVersionSummary currentVersion,
            List<String> labels,
            Integer activeDictionaryVersionNo) {
        return new DocumentSummaryResult(
                document.getId(),
                document.getTitle(),
                document.getCurrentVersionNo(),
                currentVersion != null && currentVersion.isAligned(activeDictionaryVersionNo),
                currentVersion != null && currentVersion.edited(),
                currentVersion == null ? null : currentVersion.dictionaryVersionNo(),
                labels,
                document.getCreatedBy(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
