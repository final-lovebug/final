package com.ubidict.backend.reviewrequest.infra.port;

public record DraftDocumentSnapshot(Long draftDocumentId, Long documentId, int baseVersionNo, String draftBody) {}
