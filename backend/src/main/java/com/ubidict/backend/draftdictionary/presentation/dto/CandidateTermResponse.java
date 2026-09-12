package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.domain.CandidateTermOrigin;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import java.time.OffsetDateTime;
import java.util.List;

public record CandidateTermResponse(
        Long candidateTermId,
        Long draftDictionaryId,
        CandidateTermOrigin origin,
        Long sourceTermId,
        String form,
        String proposedDefinition,
        String proposedEnglishName,
        Integer occurrenceCount,
        CandidateTermStatus status,
        Long handledBy,
        String rejectReason,
        Long mergeTargetTermId,
        Long resultTermId,
        List<Long> occurredDocumentIds,
        List<String> contextSnippets,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
    public static CandidateTermResponse from(CandidateTermResult r) {
        return new CandidateTermResponse(
                r.candidateTermId(),
                r.draftDictionaryId(),
                r.origin(),
                r.sourceTermId(),
                r.form(),
                r.proposedDefinition(),
                r.proposedEnglishName(),
                r.occurrenceCount(),
                r.status(),
                r.handledBy(),
                r.rejectReason(),
                r.mergeTargetTermId(),
                r.resultTermId(),
                r.occurredDocumentIds(),
                r.contextSnippets(),
                r.createdAt(),
                r.updatedAt());
    }
}
