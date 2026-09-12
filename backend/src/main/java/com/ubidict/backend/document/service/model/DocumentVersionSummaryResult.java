package com.ubidict.backend.document.service.model;

import com.ubidict.backend.document.infra.DocumentVersionSummary;
import java.time.OffsetDateTime;

/**
 * 버전 이력용. 본문을 담지 않는다.
 */
public record DocumentVersionSummaryResult(
        int versionNo, OffsetDateTime publishedAt, Integer dictionaryVersionNo, boolean edited, Long publishedBy) {
    public static DocumentVersionSummaryResult from(DocumentVersionSummary summary) {
        return new DocumentVersionSummaryResult(
                summary.versionNo(),
                summary.publishedAt(),
                summary.dictionaryVersionNo(),
                summary.edited(),
                summary.publishedBy());
    }
}
