package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.PerformReexamineCommand;
import java.util.List;

public record PerformReexamineRequest(String proposedBody, List<Long> addressedCommentIds) {

    public PerformReexamineCommand to(Long reviewRequestId, Long actorId) {
        return new PerformReexamineCommand(reviewRequestId, proposedBody, addressedCommentIds, actorId);
    }
}
