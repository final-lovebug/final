package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import java.util.List;

public record BulkDecideCandidateTermsCommand(
        Long draftDictionaryId,
        List<Long> candidateTermIds,
        Long memberId,
        CandidateTermStatus decision,
        String rejectReason,
        Long mergeTargetTermId) {

    public BulkDecideCandidateTermsCommand {
        if (candidateTermIds == null
                || candidateTermIds.isEmpty()
                || candidateTermIds.stream().anyMatch(id -> id == null)) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        new DecideCandidateTermCommand(0L, memberId, decision, rejectReason, mergeTargetTermId);
        candidateTermIds = List.copyOf(candidateTermIds);
    }

    public DecideCandidateTermCommand toSingle(Long id) {
        return new DecideCandidateTermCommand(id, memberId, decision, rejectReason, mergeTargetTermId);
    }
}
