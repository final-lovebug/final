package com.ubidict.backend.workspace.service.model;

public record UpdateRuleSetCommand(
        Long workspaceId, int requiredDocumentReviewerCount, int requiredDictionaryReviewerCount, Long memberId) {}
