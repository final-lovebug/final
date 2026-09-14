package com.ubidict.backend.document.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.document.infra.port.DraftDocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEditGuard {

    private static final Logger log = LoggerFactory.getLogger(DocumentEditGuard.class);
    private final DraftDocumentQueryPort draftDocumentQueryPort;
    private final DraftDictionaryQueryPort draftDictionaryQueryPort;

    public void validate(Long documentId) {
        if (draftDocumentQueryPort.hasOngoingDraft(documentId)) {
            log.warn(
                    "[DocumentEditGuard.validate] Document edit blocked documentId={}, reason=documentDraft",
                    documentId);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_DRAFT_IN_PROGRESS);
        }
        if (draftDictionaryQueryPort.isSourceOfOngoingDraft(documentId)) {
            log.warn(
                    "[DocumentEditGuard.validate] Document edit blocked documentId={}, reason=dictionaryDraft",
                    documentId);
            throw new BusinessException(DocumentErrorCode.DOCUMENT_SOURCE_OF_DICTIONARY_DRAFT);
        }
    }
}
