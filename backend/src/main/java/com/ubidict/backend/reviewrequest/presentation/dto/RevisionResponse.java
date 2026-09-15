package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.RevisionResult;

public record RevisionResponse(
        Long id,
        Long reviewRequestId,
        Long targetId,
        int baseVersionNo,
        Long draftId,
        String proposedBody,
        int reexamineRound) {
    public static RevisionResponse from(RevisionResult r) {
        return new RevisionResponse(
                r.id(),
                r.reviewRequestId(),
                r.targetId(),
                r.baseVersionNo(),
                r.draftId(),
                r.proposedBody(),
                r.reexamineRound());
    }
}
