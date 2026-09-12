package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.implement.DraftDocumentReader;
import com.ubidict.backend.draftdocument.implement.SuggestionTermProcessor;
import com.ubidict.backend.draftdocument.implement.SuggestionTermReader;
import com.ubidict.backend.draftdocument.implement.SuggestionTermWriter;
import com.ubidict.backend.draftdocument.service.model.AcceptSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.AddSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.EditSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.RejectSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermResult;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermSearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionTermService {
    private final SuggestionTermReader reader;
    private final SuggestionTermWriter writer;
    private final DraftDocumentReader draftReader;
    private final SuggestionTermProcessor processor;

    @Transactional
    public SuggestionTermResult add(AddSuggestionTermCommand c) {
        DraftDocument draftDocument = draftReader.read(c.draftDocumentId());
        draftDocument.validateExamining();
        validateAnchor(c.anchor(), draftDocument);
        return SuggestionTermResult.from(
                writer.add(c.draftDocumentId(), c.anchor(), c.originTerm(), c.suggestionTerm(), c.memberId()));
    }

    @Transactional
    public SuggestionTermResult edit(EditSuggestionTermCommand c) {
        SuggestionTerm suggestionTerm = reader.read(c.id());
        DraftDocument draftDocument = draftReader.read(suggestionTerm.getDraftDocumentId());
        draftDocument.validateExamining();
        if (c.anchor() != null) {
            validateAnchor(c.anchor(), draftDocument);
        }
        return SuggestionTermResult.from(writer.update(suggestionTerm, c.anchor(), c.originTerm(), c.suggestionTerm()));
    }

    @Transactional
    public void delete(Long id) {
        SuggestionTerm suggestionTerm = reader.read(id);
        draftReader.read(suggestionTerm.getDraftDocumentId()).validateExamining();
        suggestionTerm.delete();
    }

    @Transactional
    public SuggestionTermResult accept(AcceptSuggestionTermCommand command) {
        SuggestionTerm suggestionTerm = reader.read(command.suggestionTermId());
        DraftDocument draftDocument = draftReader.read(suggestionTerm.getDraftDocumentId());
        processor.accept(draftDocument, suggestionTerm, command.memberId());

        log.info(
                "[SuggestionTermService.accept] Suggestion term accepted. suggestionTermId={}, draftDocumentId={}, memberId={}",
                suggestionTerm.getId(),
                draftDocument.getId(),
                command.memberId());

        return SuggestionTermResult.from(suggestionTerm);
    }

    @Transactional
    public SuggestionTermResult reject(RejectSuggestionTermCommand command) {
        SuggestionTerm suggestionTerm = reader.read(command.suggestionTermId());
        DraftDocument draftDocument = draftReader.read(suggestionTerm.getDraftDocumentId());
        processor.reject(draftDocument, suggestionTerm, command.memberId(), command.rejectReason());

        log.info(
                "[SuggestionTermService.reject] Suggestion term rejected. suggestionTermId={}, draftDocumentId={}, memberId={}",
                suggestionTerm.getId(),
                draftDocument.getId(),
                command.memberId());

        return SuggestionTermResult.from(suggestionTerm);
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
