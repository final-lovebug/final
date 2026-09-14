package com.ubidict.backend.draftdictionary.infra.port;

import java.util.List;

public record ExtractedTerm(
        String form,
        String proposedDefinition,
        String proposedEnglishName,
        List<Long> occurredDocumentIds,
        int occurrenceCount,
        List<String> contextSnippets,
        List<String> variantForms) {
    public ExtractedTerm(
            String form,
            String proposedDefinition,
            String proposedEnglishName,
            List<Long> occurredDocumentIds,
            int occurrenceCount,
            List<String> contextSnippets) {
        this(
                form,
                proposedDefinition,
                proposedEnglishName,
                occurredDocumentIds,
                occurrenceCount,
                contextSnippets,
                List.of());
    }
}
