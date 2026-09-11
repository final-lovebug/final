package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.Reviewer;
import java.time.OffsetDateTime;

public record ReviewerResult(
        Long reviewerId, Long reviewRequestId, Long memberId, OffsetDateTime assignedAt, Long createdBy) {
    public static ReviewerResult from(Reviewer r) {
        return new ReviewerResult(
                r.getId(), r.getReviewRequestId(), r.getMemberId(), r.getAssignedAt(), r.getCreatedBy());
    }
}
