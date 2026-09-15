package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.domain.CandidateTermType;
import com.ubidict.backend.draftdictionary.service.model.EditCandidateTermCommand;
import java.util.*;

public record EditCandidateTermRequest(
        String form, String proposedDefinition, String proposedEnglishName, CandidateTermType type) {
    public EditCandidateTermCommand toCommand(Long id, Long member) {
        return new EditCandidateTermCommand(id, form, proposedDefinition, proposedEnglishName, member, type);
    }
}
