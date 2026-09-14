package com.ubidict.backend.reviewrequest.service.model;

import java.util.List;

public record PerformReexamineCommand(
        Long reviewRequestId, String proposedBody, List<Long> addressedCommentIds, Long actorId) {

    public PerformReexamineCommand {
        addressedCommentIds = addressedCommentIds == null ? List.of() : List.copyOf(addressedCommentIds);
    }
}
