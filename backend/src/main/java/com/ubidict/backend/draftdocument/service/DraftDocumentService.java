package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.implement.DraftDocumentReader;
import com.ubidict.backend.draftdocument.implement.DraftDocumentRemover;
import com.ubidict.backend.draftdocument.implement.DraftDocumentWriter;
import com.ubidict.backend.draftdocument.implement.SuggestionTermProcessor;
import com.ubidict.backend.draftdocument.implement.SuggestionTermReader;
import com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentSearchQuery;
import com.ubidict.backend.draftdocument.service.model.ExamineProgressResult;
import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentService {

    private final DraftDocumentReader draftDocumentReader;
    private final DraftDocumentWriter draftDocumentWriter;
    private final DraftDocumentRemover draftDocumentRemover;
    private final SuggestionTermReader suggestionTermReader;
    private final SuggestionTermProcessor suggestionTermProcessor;

    @Transactional
    public DraftDocumentResult create(CreateDraftDocumentCommand command) {
        DraftDocument draftDocument = draftDocumentWriter.append(
                command.documentId(),
                command.baseVersionNo(),
                command.draftBody(),
                command.memberId(),
                command.memberId());

        log.info(
                "[DraftDocumentService.create] Draft document created. draftDocumentId={}, documentId={}, memberId={}",
                draftDocument.getId(),
                draftDocument.getDocumentId(),
                command.memberId());

        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional(readOnly = true)
    public DraftDocumentResult read(Long draftDocumentId, Long memberId) {
        return DraftDocumentResult.from(draftDocumentReader.read(draftDocumentId));
    }

    @Transactional
    public DraftDocumentResult updateBody(UpdateDraftBodyCommand command) {
        DraftDocument draftDocument = draftDocumentReader.read(command.draftDocumentId());
        draftDocumentWriter.updateBody(draftDocument, command.draftBody());

        log.info(
                "[DraftDocumentService.updateBody] Draft document body updated. draftDocumentId={}, memberId={}",
                draftDocument.getId(),
                command.memberId());

        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional
    public void delete(Long draftDocumentId, Long memberId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        draftDocumentRemover.remove(draftDocument);

        log.info(
                "[DraftDocumentService.delete] Draft document deleted. draftDocumentId={}, memberId={}",
                draftDocument.getId(),
                memberId);
    }

    @Transactional(readOnly = true)
    public PageResult<DraftDocumentResult> search(DraftDocumentSearchQuery q) {
        String[] p = q.sort().split(",");
        Page<DraftDocument> r = draftDocumentReader.search(
                q.documentId(),
                q.status(),
                PageRequest.of(q.page(), q.size(), Sort.by(Sort.Direction.fromString(p[1]), p[0])));
        return new PageResult<>(
                r.getContent().stream().map(DraftDocumentResult::from).toList(),
                q.page(),
                q.size(),
                r.getTotalElements());
    }

    @Transactional
    public DraftDocumentResult completeExamine(CompleteExamineCommand command) {
        DraftDocument draftDocument = draftDocumentReader.read(command.draftDocumentId());
        List<SuggestionTerm> suggestionTerms = suggestionTermReader.readAll(draftDocument.getId());
        suggestionTermProcessor.complete(draftDocument, suggestionTerms);

        log.info(
                "[DraftDocumentService.completeExamine] Draft document examination completed. draftDocumentId={}, documentId={}, memberId={}",
                draftDocument.getId(),
                draftDocument.getDocumentId(),
                command.memberId());

        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional(readOnly = true)
    public ExamineProgressResult readExamineProgress(Long draftDocumentId, Long memberId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        List<SuggestionTerm> suggestionTerms = suggestionTermReader.readAll(draftDocumentId);
        String previewBody = suggestionTermProcessor.preview(draftDocument, suggestionTerms);
        return ExamineProgressResult.from(suggestionTerms, previewBody);
    }
}
