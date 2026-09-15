package com.ubidict.backend.draftdictionary.service.model;

import java.util.List;

public record UpdateSourceDocumentsCommand(Long draftDictionaryId, List<Long> sourceDocumentIds, Long memberId) {}
