package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DraftBodyComposer {

    public String compose(String originalBody, List<SuggestionTerm> suggestionTerms) {
        List<SuggestionTerm> appliedTerms = suggestionTerms.stream()
                .filter(SuggestionTerm::isApplied)
                .sorted(Comparator.comparingInt(term -> term.getAnchor().startOffset()))
                .toList();

        validateAnchors(originalBody, appliedTerms);

        StringBuilder composedBody = new StringBuilder(originalBody);
        appliedTerms
                .reversed()
                .forEach(term -> composedBody.replace(
                        term.getAnchor().startOffset(), term.getAnchor().endOffset(), term.getSuggestionTerm()));
        return composedBody.toString();
    }

    private void validateAnchors(String originalBody, List<SuggestionTerm> appliedTerms) {
        int previousEndOffset = -1;
        for (SuggestionTerm term : appliedTerms) {
            TextRange anchor = term.getAnchor();
            if (anchor.endOffset() > originalBody.length() || anchor.startOffset() < previousEndOffset) {
                throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_ANCHOR);
            }
            previousEndOffset = anchor.endOffset();
        }
    }
}
