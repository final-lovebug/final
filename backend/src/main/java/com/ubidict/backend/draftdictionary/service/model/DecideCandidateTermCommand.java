package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import java.util.EnumSet;
import java.util.Set;

public record DecideCandidateTermCommand(
        Long candidateTermId,
        Long memberId,
        CandidateTermStatus decision,
        String rejectReason,
        Long mergeTargetTermId) {

    private static final Set<CandidateTermStatus> SUPPORTED_DECISIONS = EnumSet.of(
            CandidateTermStatus.REGISTRATION_APPROVED,
            CandidateTermStatus.MERGED_AS_SYNONYM,
            CandidateTermStatus.REJECTED,
            CandidateTermStatus.ON_HOLD);

    public DecideCandidateTermCommand {
        if (decision == null || !SUPPORTED_DECISIONS.contains(decision)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_DECISION);
        }
    }
}
