package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.implement.DraftDocumentReader;
import com.ubidict.backend.draftdocument.implement.DraftDocumentRemover;
import com.ubidict.backend.draftdocument.implement.DraftDocumentWriter;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentService {

    private final DraftDocumentReader draftDocumentReader;
    private final DraftDocumentWriter draftDocumentWriter;
    private final DraftDocumentRemover draftDocumentRemover;

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
}
