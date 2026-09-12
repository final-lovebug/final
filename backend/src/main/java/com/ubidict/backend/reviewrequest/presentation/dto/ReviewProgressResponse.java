package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.ReviewProgressResult;

public record ReviewProgressResponse(
        int requiredReviewerCount, int approvedCount, int changesRequestedCount, boolean reviseEligible) {

    public static ReviewProgressResponse from(ReviewProgressResult result) {
        return new ReviewProgressResponse(
                result.requiredReviewerCount(),
                result.approvedCount(),
                result.changesRequestedCount(),
                result.reviseEligible());
    }
}
