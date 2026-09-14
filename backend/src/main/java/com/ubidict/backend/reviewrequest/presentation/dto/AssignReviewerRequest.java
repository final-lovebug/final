package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.AssignReviewerCommand;
import jakarta.validation.constraints.NotNull;

public record AssignReviewerRequest(@NotNull Long memberId) {
    public AssignReviewerCommand toCommand(Long reviewRequestId, Long actorId) {
        return new AssignReviewerCommand(reviewRequestId, memberId, actorId);
    }
}
