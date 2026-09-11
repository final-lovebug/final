package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.SuggestionTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuggestionTermReader {
    private final SuggestionTermRepository repo;

    public SuggestionTerm read(Long id) {
        return repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_SUGGESTION_TERM_NOT_FOUND));
    }

    public Page<SuggestionTerm> search(Long id, SuggestionTermStatus status, Pageable p) {
        return status == null
                ? repo.findAllByDraftDocumentIdAndDeletedAtIsNull(id, p)
                : repo.findAllByDraftDocumentIdAndStatusAndDeletedAtIsNull(id, status, p);
    }
}
