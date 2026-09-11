package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
}
