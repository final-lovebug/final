package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDocumentReader {

    private final DraftDocumentRepository draftDocumentRepository;

    public DraftDocument read(Long draftDocumentId) {
        return draftDocumentRepository
                .findByIdAndDeletedAtIsNull(draftDocumentId)
                .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND));
    }

    public List<DraftDocument> readByDocumentId(Long documentId) {
        return draftDocumentRepository.findAllByDocumentIdAndDeletedAtIsNullOrderByCreatedAtDesc(documentId);
    }

    public Page<DraftDocument> search(Long documentId, DraftDocumentStatus status, Pageable pageable) {
        if (documentId != null && status != null)
            return draftDocumentRepository.findAllByDocumentIdAndStatusAndDeletedAtIsNull(documentId, status, pageable);
        if (documentId != null)
            return draftDocumentRepository.findAllByDocumentIdAndDeletedAtIsNull(documentId, pageable);
        if (status != null) return draftDocumentRepository.findAllByStatusAndDeletedAtIsNull(status, pageable);
        return draftDocumentRepository.findAllByDeletedAtIsNull(pageable);
    }
}
