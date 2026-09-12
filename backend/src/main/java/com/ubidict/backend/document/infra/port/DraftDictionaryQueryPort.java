package com.ubidict.backend.document.infra.port;

public interface DraftDictionaryQueryPort {
    boolean isSourceOfOngoingDraft(Long documentId);
}
