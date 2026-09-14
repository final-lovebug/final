package com.ubidict.backend.draftdocument.infra.port;

public record DocumentSnapshot(Long documentId, Long workspaceId, int currentVersionNo, String body) {}
