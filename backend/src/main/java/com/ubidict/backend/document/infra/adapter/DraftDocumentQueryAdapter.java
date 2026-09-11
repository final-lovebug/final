package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.document.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "real")
public class DraftDocumentQueryAdapter implements DraftDocumentQueryPort {
    private final DraftDocumentRepository draftDocumentRepository;

    @Override
    public boolean hasOngoingDraft(Long documentId) {
        return draftDocumentRepository.findAllByDocumentIdAndDeletedAtIsNullOrderByCreatedAtDesc(documentId).stream()
                .anyMatch(draft -> draft.getStatus() != DraftDocumentStatus.REVISED);
    }
}
