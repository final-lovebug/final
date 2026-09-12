package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;

public record TextRangeRequest(int startOffset, int endOffset) {

    public TextRange to() {
        try {
            return new TextRange(startOffset, endOffset);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_ANCHOR);
        }
    }
}
