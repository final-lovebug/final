package com.ubidict.backend.draftdocument.service.model;

public record RejectSuggestionTermCommand(Long suggestionTermId, String rejectReason, Long memberId) {}
