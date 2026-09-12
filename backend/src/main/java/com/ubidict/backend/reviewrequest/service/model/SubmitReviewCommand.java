package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import java.util.List;

public record SubmitReviewCommand(
        Long reviewRequestId, Long memberId, int targetRound, ReviewVerdict verdict, List<NewComment> comments) {

    public SubmitReviewCommand {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }

    public SubmitReviewCommand(Long reviewRequestId, Long memberId, int targetRound, ReviewVerdict verdict) {
        this(reviewRequestId, memberId, targetRound, verdict, List.of());
    }

    public record NewComment(String content, TextRange anchor, Long targetItemId, Long parentId) {}
}
