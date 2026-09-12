package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.service.model.SubmitReviewCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitReviewRequest(
        @PositiveOrZero int targetRound, @NotNull ReviewVerdict verdict) {

    public SubmitReviewCommand toCommand(Long reviewRequestId, Long memberId) {
        return new SubmitReviewCommand(reviewRequestId, memberId, targetRound, verdict);
    }
}
