package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.service.model.SubmitReviewCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record SubmitReviewRequest(
        @PositiveOrZero int targetRound, @NotNull ReviewVerdict verdict, List<@Valid AddCommentRequest> comments) {

    public SubmitReviewCommand toCommand(Long reviewRequestId, Long memberId) {
        List<SubmitReviewCommand.NewComment> newComments = comments == null
                ? List.of()
                : comments.stream()
                        .map(comment -> new SubmitReviewCommand.NewComment(
                                comment.content(), comment.anchor(), comment.targetItemId(), comment.parentId()))
                        .toList();
        return new SubmitReviewCommand(reviewRequestId, memberId, targetRound, verdict, newComments);
    }
}
