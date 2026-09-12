package com.ubidict.backend.workspace.presentation;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateRuleSetRequest(
        @PositiveOrZero int requiredDocumentReviewerCount,
        @PositiveOrZero int requiredDictionaryReviewerCount) {}
