package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.draftdictionary.domain.CandidateTermType;

public record EditCandidateTermCommand(
        Long candidateTermId,
        String form,
        String proposedDefinition,
        String proposedEnglishName,
        Long memberId,
        CandidateTermType type) {
    /** 분류를 건드리지 않고 수정한다. */
    public EditCandidateTermCommand(
            Long candidateTermId, String form, String proposedDefinition, String proposedEnglishName, Long memberId) {
        this(candidateTermId, form, proposedDefinition, proposedEnglishName, memberId, null);
    }
}
