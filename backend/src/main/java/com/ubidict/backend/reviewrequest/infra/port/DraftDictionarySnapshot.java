package com.ubidict.backend.reviewrequest.infra.port;

public record DraftDictionarySnapshot(Long draftDictionaryId, Long workspaceId, Long dictionaryId) {}
