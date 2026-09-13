package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.Revise;
import java.time.OffsetDateTime;

public record ReviseResult(
        Long reviseId, Long reviewRequestId, int resultVersionNo, Long performedBy, OffsetDateTime performedAt) {

    public static ReviseResult from(Revise revise) {
        return new ReviseResult(
                revise.getId(),
                revise.getReviewRequestId(),
                revise.getResultVersionNo(),
                revise.getPerformedBy(),
                revise.getPerformedAt());
    }
}
