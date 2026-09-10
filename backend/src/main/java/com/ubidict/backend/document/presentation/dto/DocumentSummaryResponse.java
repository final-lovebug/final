package com.ubidict.backend.document.presentation.dto;

import com.ubidict.backend.document.service.model.DocumentSummaryResult;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 목록 응답. 본문을 담지 않는다 — 10,000자 × N을 목록에 실을 이유가 없다.
 */
public record DocumentSummaryResponse(
        Long documentId,
        String title,
        int currentVersionNo,
        boolean outdated,
        List<String> labels,
        Long uploaderId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static DocumentSummaryResponse from(DocumentSummaryResult result) {
        return new DocumentSummaryResponse(
                result.documentId(),
                result.title(),
                result.currentVersionNo(),
                result.outdated(),
                result.labels(),
                result.uploaderId(),
                result.createdAt(),
                result.updatedAt());
    }
}
