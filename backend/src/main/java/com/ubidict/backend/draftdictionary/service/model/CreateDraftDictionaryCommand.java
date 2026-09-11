package com.ubidict.backend.draftdictionary.service.model;

import java.util.List;

public record CreateDraftDictionaryCommand(
        Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long memberId) {}
