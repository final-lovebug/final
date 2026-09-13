package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CheckSuggestionValidator {

    public void validate(String body, List<CheckSuggestion> suggestions) {
        if (suggestions == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_RESULT);
        }
        for (CheckSuggestion suggestion : suggestions) {
            if (suggestion == null
                    || suggestion.anchor() == null
                    || suggestion.originTerm() == null
                    || suggestion.suggestionTerm() == null
                    || suggestion.originTerm().isBlank()
                    || suggestion.suggestionTerm().isBlank()
                    || suggestion.anchor().endOffset() > body.length()
                    || !body.substring(
                                    suggestion.anchor().startOffset(),
                                    suggestion.anchor().endOffset())
                            .equals(suggestion.originTerm())) {
                throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_RESULT);
            }
        }
    }
}
