package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.ReviewerResult;
import java.time.OffsetDateTime;

public record ReviewerResponse(
        Long reviewerId, Long reviewRequestId, Long memberId, OffsetDateTime assignedAt, Long createdBy) {
    public static ReviewerResponse from(ReviewerResult r) {
        return new ReviewerResponse(r.reviewerId(), r.reviewRequestId(), r.memberId(), r.assignedAt(), r.createdBy());
    }
}
