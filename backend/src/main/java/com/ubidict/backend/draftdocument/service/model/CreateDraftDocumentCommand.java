package com.ubidict.backend.draftdocument.service.model;

public record CreateDraftDocumentCommand(Long documentId, int baseVersionNo, String draftBody, Long memberId) {}
