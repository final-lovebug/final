package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.Reexamine;
import java.time.OffsetDateTime;
import java.util.List;

public record ReexamineResult(
        Long reexamineId,
        Long reviewRequestId,
        int round,
        Long performedBy,
        List<Long> addressedCommentIds,
        OffsetDateTime performedAt) {

    public static ReexamineResult from(Reexamine reexamine) {
        return new ReexamineResult(
                reexamine.getId(),
                reexamine.getReviewRequestId(),
                reexamine.getRound(),
                reexamine.getPerformedBy(),
                reexamine.getAddressedCommentIds(),
                reexamine.getPerformedAt());
    }
}
