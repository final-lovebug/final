package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;

public record RevisionResult(
        Long id,
        Long reviewRequestId,
        Long targetId,
        int baseVersionNo,
        Long draftId,
        String proposedBody,
        int reexamineRound) {
    public static RevisionResult from(RevisionDocument r) {
        return new RevisionResult(
                r.getId(),
                r.getReviewRequestId(),
                r.getDocumentId(),
                r.getBaseVersionNo(),
                r.getDraftDocumentId(),
                r.getProposedBody(),
                r.getReexamineRound());
    }

    public static RevisionResult from(RevisionDictionary r) {
        return new RevisionResult(
                r.getId(),
                r.getReviewRequestId(),
                r.getDictionaryId(),
                r.getBaseVersionNo(),
                r.getDraftDictionaryId(),
                null,
                r.getReexamineRound());
    }
}
