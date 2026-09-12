package com.ubidict.backend.document.infra.port;

public interface DraftDocumentQueryPort {
    boolean hasOngoingDraft(Long documentId);
}
