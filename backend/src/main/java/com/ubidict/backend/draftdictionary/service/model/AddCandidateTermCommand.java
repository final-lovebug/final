package com.ubidict.backend.draftdictionary.service.model;

import java.util.List;

public record AddCandidateTermCommand(
        Long draftDictionaryId,
        String form,
        String proposedDefinition,
        String proposedEnglishName,
        List<Long> occurredDocumentIds,
        int occurrenceCount,
        List<String> contextSnippets,
        List<String> variantForms,
        Long memberId) {
    public AddCandidateTermCommand(
            Long draftDictionaryId,
            String form,
            String proposedDefinition,
            String proposedEnglishName,
            List<Long> occurredDocumentIds,
            int occurrenceCount,
            List<String> contextSnippets,
            Long memberId) {
        this(
                draftDictionaryId,
                form,
                proposedDefinition,
                proposedEnglishName,
                occurredDocumentIds,
                occurrenceCount,
                contextSnippets,
                List.of(),
                memberId);
    }
}
