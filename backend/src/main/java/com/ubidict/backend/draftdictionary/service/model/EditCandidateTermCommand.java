package com.ubidict.backend.draftdictionary.service.model;

public record EditCandidateTermCommand(
        Long candidateTermId, String form, String proposedDefinition, String proposedEnglishName, Long memberId) {}
