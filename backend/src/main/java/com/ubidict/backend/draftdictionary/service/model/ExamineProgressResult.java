package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import java.util.List;

public record ExamineProgressResult(
        long total, long pending, long kept, long approved, long merged, long rejected, long onHold) {

    public static ExamineProgressResult from(List<CandidateTerm> terms) {
        return new ExamineProgressResult(
                terms.size(),
                terms.stream().filter(CandidateTerm::isPending).count(),
                terms.stream().filter(CandidateTerm::isKept).count(),
                terms.stream().filter(CandidateTerm::isRegistrationApproved).count(),
                terms.stream().filter(CandidateTerm::isMergedAsSynonym).count(),
                terms.stream().filter(CandidateTerm::isRejected).count(),
                terms.stream().filter(CandidateTerm::isOnHold).count());
    }
}
