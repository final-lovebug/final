package com.ubidict.backend.draftdictionary.service.model;

import com.ubidict.backend.draftdictionary.domain.CandidateTermType;
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
        Long memberId,
        CandidateTermType type) {
    /** 분류 없이 등록한다(추출 생성분·기존 호출부). */
    public AddCandidateTermCommand(
            Long draftDictionaryId,
            String form,
            String proposedDefinition,
            String proposedEnglishName,
            List<Long> occurredDocumentIds,
            int occurrenceCount,
            List<String> contextSnippets,
            List<String> variantForms,
            Long memberId) {
        this(
                draftDictionaryId,
                form,
                proposedDefinition,
                proposedEnglishName,
                occurredDocumentIds,
                occurrenceCount,
                contextSnippets,
                variantForms,
                memberId,
                null);
    }

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
                memberId,
                null);
    }
}
