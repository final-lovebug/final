package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import java.time.OffsetDateTime;

public record ReviewResult(
        Long reviewId,
        Long reviewRequestId,
        Long memberId,
        int targetRound,
        ReviewVerdict verdict,
        OffsetDateTime submittedAt,
        OffsetDateTime createdAt) {

    public static ReviewResult from(Review review) {
        return new ReviewResult(
                review.getId(),
                review.getReviewRequestId(),
                review.getMemberId(),
                review.getTargetRound(),
                review.getVerdict(),
                review.getSubmittedAt(),
                review.getCreatedAt());
    }
}
