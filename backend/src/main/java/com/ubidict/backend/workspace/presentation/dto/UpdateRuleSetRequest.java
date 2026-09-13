package com.ubidict.backend.workspace.presentation.dto;

import com.ubidict.backend.workspace.service.model.UpdateRuleSetCommand;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateRuleSetRequest(
        @PositiveOrZero int requiredDocumentReviewerCount,
        @PositiveOrZero int requiredDictionaryReviewerCount) {

    public UpdateRuleSetCommand toCommand(Long workspaceId, Long memberId) {
        return new UpdateRuleSetCommand(
                workspaceId, requiredDocumentReviewerCount, requiredDictionaryReviewerCount, memberId);
    }
}
