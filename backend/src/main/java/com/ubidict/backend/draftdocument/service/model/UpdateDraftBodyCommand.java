package com.ubidict.backend.draftdocument.service.model;

public record UpdateDraftBodyCommand(Long draftDocumentId, String draftBody, Long memberId) {}
