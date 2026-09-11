package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.document.infra.port.DraftDocumentQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "stub", matchIfMissing = true)
public class DraftDocumentQueryStub implements DraftDocumentQueryPort {
    @Override
    public boolean hasOngoingDraft(Long documentId) {
        return false;
    }
}
