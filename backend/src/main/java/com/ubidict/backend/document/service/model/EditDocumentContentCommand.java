package com.ubidict.backend.document.service.model;

public record EditDocumentContentCommand(Long workspaceId, Long documentId, String content, Long memberId) {}
