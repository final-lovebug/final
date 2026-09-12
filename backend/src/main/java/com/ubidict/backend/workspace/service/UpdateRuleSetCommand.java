package com.ubidict.backend.workspace.service;

public record UpdateRuleSetCommand(
        Long workspaceId, int requiredDocumentReviewerCount, int requiredDictionaryReviewerCount, Long memberId) {}
