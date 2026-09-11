package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermOrigin;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record CandidateTermResult(
        Long candidateTermId,
        Long draftDictionaryId,
        CandidateTermOrigin origin,
        Long sourceTermId,
        String form,
        String proposedDefinition,
        String proposedEnglishName,
        Integer occurrenceCount,
        CandidateTermStatus status,
        List<Long> occurredDocumentIds,
        List<String> contextSnippets,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
    public static CandidateTermResult from(CandidateTerm e) {
        return new CandidateTermResult(
                e.getId(),
                e.getDraftDictionaryId(),
                e.getOrigin(),
                e.getSourceTermId(),
                e.getForm(),
                e.getProposedDefinition(),
                e.getProposedEnglishName(),
                e.getOccurrenceCount(),
                e.getStatus(),
                List.copyOf(e.getOccurredDocumentIds()),
                List.copyOf(e.getContextSnippets()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
