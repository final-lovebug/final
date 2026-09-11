package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import org.springframework.stereotype.Component;

@Component
public class SuggestionTermOwnershipValidator {
    public void validate(SuggestionTerm term, Long draftDocumentId) {
        if (!term.getDraftDocumentId().equals(draftDocumentId)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_SUGGESTION_TERM_MISMATCHED);
        }
    }
}
