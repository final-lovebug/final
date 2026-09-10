package com.ubidict.backend.document.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.document.infra.DocumentVersionSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentVersionReader {

    private final DocumentVersionRepository documentVersionRepository;

    public DocumentVersion readCurrent(Document document) {
        return read(document, document.getCurrentVersionNo());
    }

    public DocumentVersion read(Document document, int versionNo) {
        return documentVersionRepository
                .findByDocumentIdAndVersionVersionNo(document.getId(), versionNo)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_VERSION_NOT_FOUND));
    }

    public List<DocumentVersionSummary> readHistory(Document document) {
        return documentVersionRepository.findSummariesByDocumentId(document.getId());
    }

    /**
     * 문서별 최신 확정 버전을 한 번에 읽는다. 목록 조회가 문서 수만큼 질의하지 않게 한다.
     */
    public Map<Long, DocumentVersionSummary> readCurrentSummaries(Collection<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return Map.of();
        }

        return documentVersionRepository.findCurrentSummaries(documentIds).stream()
                .collect(java.util.stream.Collectors.toMap(DocumentVersionSummary::documentId, Function.identity()));
    }
}
