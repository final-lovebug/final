package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDocumentCreationPolicyValidator {

    private final DraftDocumentRepository draftDocumentRepository;
    private final ReviewRequestQueryPort reviewRequestQueryPort;
    private final DraftDictionaryQueryPort draftDictionaryQueryPort;

    public void validate(Long documentId, Long workspaceId) {
        if (draftDocumentRepository.existsByDocumentIdAndStatusNotAndDeletedAtIsNull(
                documentId, DraftDocumentStatus.REVISED)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXISTS);
        }
        if (reviewRequestQueryPort.hasOngoingDocumentReview(documentId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_UNDER_REVIEW);
        }
        if (draftDictionaryQueryPort.hasOngoingDraft(workspaceId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXISTS);
        }
    }
}
