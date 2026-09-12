package com.ubidict.backend.document.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.document.infra.port.DraftDocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEditGuard {
    private final DraftDocumentQueryPort draftDocumentQueryPort;
    private final DraftDictionaryQueryPort draftDictionaryQueryPort;

    public void validate(Long documentId) {
        if (draftDocumentQueryPort.hasOngoingDraft(documentId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_DRAFT_IN_PROGRESS);
        }
        if (draftDictionaryQueryPort.isSourceOfOngoingDraft(documentId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT);
        }
    }
}
