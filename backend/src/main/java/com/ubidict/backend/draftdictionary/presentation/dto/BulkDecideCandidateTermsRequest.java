package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.service.model.BulkDecideCandidateTermsCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkDecideCandidateTermsRequest(
        @NotEmpty List<@NotNull Long> candidateTermIds,
        @NotNull CandidateTermStatus decision,
        String rejectReason,
        Long mergeTargetTermId) {
    public BulkDecideCandidateTermsCommand toCommand(Long id, Long member) {
        return new BulkDecideCandidateTermsCommand(
                id, candidateTermIds, member, decision, rejectReason, mergeTargetTermId);
    }
}
