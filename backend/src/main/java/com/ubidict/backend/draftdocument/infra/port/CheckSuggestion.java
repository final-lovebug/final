package com.ubidict.backend.draftdocument.infra.port;

import com.ubidict.backend.common.domain.TextRange;

public record CheckSuggestion(TextRange anchor, String originTerm, String suggestionTerm) {}
