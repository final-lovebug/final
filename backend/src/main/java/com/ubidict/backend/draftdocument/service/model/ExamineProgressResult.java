package com.ubidict.backend.draftdocument.service.model;

import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import java.util.List;

public record ExamineProgressResult(
        long total, long pending, long keptOrigin, long appliedSuggestion, String previewBody) {

    public static ExamineProgressResult from(List<SuggestionTerm> suggestionTerms, String previewBody) {
        long pending =
                suggestionTerms.stream().filter(SuggestionTerm::isPending).count();
        long appliedSuggestion =
                suggestionTerms.stream().filter(SuggestionTerm::isApplied).count();
        return new ExamineProgressResult(
                suggestionTerms.size(),
                pending,
                suggestionTerms.size() - pending - appliedSuggestion,
                appliedSuggestion,
                previewBody);
    }
}
