package com.ubidict.backend.document.service.model;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 단건용. 본문과 기준 사전집 버전은 <b>최신 확정 버전</b>에서 온다.
 */
public record DocumentResult(
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

    public static DocumentResult of(
            Document document, DocumentVersion currentVersion, List<String> labels, Integer activeDictionaryVersionNo) {
        return new DocumentResult(
                document.getId(),
                document.getWorkspaceId(),
                document.getTitle(),
                currentVersion.getBody(),
                document.getCurrentVersionNo(),
                currentVersion.isOutdated(activeDictionaryVersionNo),
                currentVersion.getDictionaryVersionNo(),
                labels,
                document.getCreatedBy(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
