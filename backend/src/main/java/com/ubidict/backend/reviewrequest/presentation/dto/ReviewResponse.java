package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.service.model.ReviewResult;
import java.time.OffsetDateTime;

public record ReviewResponse(
        Long reviewId,
        Long reviewRequestId,
        Long memberId,
        int targetRound,
        ReviewVerdict verdict,
        OffsetDateTime submittedAt,
        OffsetDateTime createdAt) {

    public static ReviewResponse from(ReviewResult result) {
        return new ReviewResponse(
                result.reviewId(),
                result.reviewRequestId(),
                result.memberId(),
                result.targetRound(),
                result.verdict(),
                result.submittedAt(),
                result.createdAt());
    }
}
