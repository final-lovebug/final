package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.infra.port.WorkspacePolicyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDocumentAccessValidator {

    private final DocumentQueryPort documentQueryPort;
    private final WorkspacePolicyPort workspacePolicyPort;

    public DocumentSnapshot validateCreation(Long documentId, Long memberId) {
        DocumentSnapshot document = documentQueryPort
                .read(documentId)
                .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND));
        if (!workspacePolicyPort.isParticipant(document.workspaceId(), memberId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    public void validateAccess(DraftDocument draftDocument, Long memberId) {
        DocumentSnapshot document = documentQueryPort
                .read(draftDocument.getDocumentId())
                .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND));
        if (!workspacePolicyPort.isParticipant(document.workspaceId(), memberId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND);
        }
    }
}
