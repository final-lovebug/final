package com.ubidict.backend.draftdocument.service.model;

import com.ubidict.backend.common.domain.TextRange;

public record AddSuggestionTermCommand(
        Long draftDocumentId, TextRange anchor, String originTerm, String suggestionTerm, Long memberId) {}
