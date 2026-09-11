package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.implement.DraftDocumentReader;
import com.ubidict.backend.draftdocument.implement.SuggestionTermReader;
import com.ubidict.backend.draftdocument.implement.SuggestionTermWriter;
import com.ubidict.backend.draftdocument.service.model.AddSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.EditSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermResult;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuggestionTermService {
    private final SuggestionTermReader reader;
    private final SuggestionTermWriter writer;
    private final DraftDocumentReader draftReader;

    @Transactional
    public SuggestionTermResult add(AddSuggestionTermCommand c) {
        DraftDocument draftDocument = draftReader.read(c.draftDocumentId());
        validateAnchor(c.anchor(), draftDocument);
        return SuggestionTermResult.from(
                writer.add(c.draftDocumentId(), c.anchor(), c.originTerm(), c.suggestionTerm(), c.memberId()));
    }

    @Transactional
    public SuggestionTermResult edit(EditSuggestionTermCommand c) {
        SuggestionTerm suggestionTerm = reader.read(c.id());
        if (c.anchor() != null) {
            validateAnchor(c.anchor(), draftReader.read(suggestionTerm.getDraftDocumentId()));
        }
        return SuggestionTermResult.from(writer.update(suggestionTerm, c.anchor(), c.originTerm(), c.suggestionTerm()));
    }

    @Transactional
    public void delete(Long id) {
        reader.read(id).delete();
    }

    @Transactional(readOnly = true)
    public PageResult<SuggestionTermResult> search(SuggestionTermSearchQuery q) {
        String[] s = q.sort().split(",");
        Page<SuggestionTerm> x = reader.search(
                q.draftDocumentId(),
                q.status(),
                PageRequest.of(q.page(), q.size(), Sort.by(Sort.Direction.fromString(s[1]), s[0])));
        return new PageResult<>(
                x.getContent().stream().map(SuggestionTermResult::from).toList(),
                q.page(),
                q.size(),
                x.getTotalElements());
    }

    private void validateAnchor(TextRange anchor, DraftDocument draftDocument) {
        if (anchor == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_ANCHOR);
        }
        if (anchor.endOffset() > draftDocument.getDraftBody().length()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY);
        }
    }
}
